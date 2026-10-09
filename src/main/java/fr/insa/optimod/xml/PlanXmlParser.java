package fr.insa.optimod.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import fr.insa.optimod.model.Intersection;
import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.Troncon;

/**
 * Lecture d'un plan de ville au format XML.
 *
 * <p>Format attendu : une racine {@code <reseau>} contenant des
 * {@code <noeud id latitude longitude>} (intersections), des
 * {@code <troncon origine destination nomRue longueur>} (segments de route) et,
 * facultativement, un {@code <entrepot adresse>} dont l'adresse est l'id d'une
 * intersection.
 */
public final class PlanXmlParser {

    /** Nom de la balise racine. */
    private static final String RACINE = "reseau";

    private PlanXmlParser() {
    }

    /**
     * Lit un plan dans un fichier.
     *
     * @param fichier fichier XML du plan
     * @return le plan lu
     * @throws XmlInvalideException si le fichier est illisible ou invalide
     */
    public static Plan lire(File fichier) throws XmlInvalideException {
        try (InputStream flux = Files.newInputStream(fichier.toPath())) {
            return lire(flux);
        } catch (IOException e) {
            throw new XmlInvalideException("Impossible de lire le fichier « "
                    + fichier.getName() + " ».", e);
        }
    }

    /**
     * Lit un plan dans un flux.
     *
     * @param flux contenu XML du plan
     * @return le plan lu
     * @throws XmlInvalideException si le contenu est invalide
     */
    public static Plan lire(InputStream flux) throws XmlInvalideException {
        Element racine = lireDocument(flux).getDocumentElement();
        if (!RACINE.equals(racine.getTagName())) {
            throw new XmlInvalideException("La balise racine doit être <"
                    + RACINE + "> (trouvée : <" + racine.getTagName()
                    + ">). Ce fichier n'est pas un plan de ville.");
        }
        Map<Long, Intersection> intersections = new LinkedHashMap<>();
        List<Element> troncons = new ArrayList<>();
        List<Element> entrepots = new ArrayList<>();
        for (Node enfant = racine.getFirstChild(); enfant != null;
                enfant = enfant.getNextSibling()) {
            if (enfant instanceof Element element) {
                switch (element.getTagName()) {
                    case "noeud" -> ajouterIntersection(element, intersections);
                    case "troncon" -> troncons.add(element);
                    case "entrepot" -> entrepots.add(element);
                    default -> { }
                }
            }
        }
        if (intersections.isEmpty()) {
            throw new XmlInvalideException(
                    "Le plan ne contient aucune intersection (<noeud>).");
        }
        List<Troncon> lus = new ArrayList<>();
        for (Element element : troncons) {
            lus.add(creerTroncon(element, intersections));
        }
        return new Plan(intersections.values(), lus,
                lireEntrepot(entrepots, intersections));
    }

    private static Document lireDocument(InputStream flux)
            throws XmlInvalideException {
        try {
            DocumentBuilderFactory usine = DocumentBuilderFactory.newInstance();
            usine.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            usine.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true);
            usine.setXIncludeAware(false);
            usine.setExpandEntityReferences(false);
            DocumentBuilder lecteur = usine.newDocumentBuilder();
            lecteur.setErrorHandler(new ErreursStrictes());
            return lecteur.parse(flux);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(
                    "Analyseur XML indisponible sur cette machine.", e);
        } catch (SAXParseException e) {
            throw new XmlInvalideException("Le fichier n'est pas un XML bien "
                    + "formé (ligne " + e.getLineNumber() + ") : "
                    + e.getMessage(), e);
        } catch (SAXException e) {
            throw new XmlInvalideException(
                    "Le fichier n'est pas un XML bien formé : "
                    + e.getMessage(), e);
        } catch (IOException e) {
            throw new XmlInvalideException("Impossible de lire le fichier.", e);
        }
    }

    private static void ajouterIntersection(Element element,
            Map<Long, Intersection> intersections)
            throws XmlInvalideException {
        long id = lireEntier(element, "id");
        double latitude = lireReel(element, "latitude");
        double longitude = lireReel(element, "longitude");
        if (latitude < -90 || latitude > 90) {
            throw new XmlInvalideException("<noeud> " + id
                    + " : latitude hors de [-90 ; 90] (" + latitude + ").");
        }
        if (longitude < -180 || longitude > 180) {
            throw new XmlInvalideException("<noeud> " + id
                    + " : longitude hors de [-180 ; 180] (" + longitude
                    + ").");
        }
        if (intersections.containsKey(id)) {
            throw new XmlInvalideException(
                    "<noeud> " + id + " : identifiant présent plusieurs fois.");
        }
        intersections.put(id, new Intersection(id, latitude, longitude));
    }

    private static Troncon creerTroncon(Element element,
            Map<Long, Intersection> intersections)
            throws XmlInvalideException {
        Intersection origine = trouver(intersections,
                lireEntier(element, "origine"), "<troncon> : origine");
        Intersection destination = trouver(intersections,
                lireEntier(element, "destination"), "<troncon> : destination");
        String nomRue = lireTexte(element, "nomRue");
        double longueur = lireReel(element, "longueur");
        if (longueur < 0) {
            throw new XmlInvalideException("<troncon> « " + nomRue
                    + " » : longueur négative (" + longueur + ").");
        }
        return new Troncon(origine, destination, nomRue, longueur);
    }

    private static Intersection lireEntrepot(List<Element> entrepots,
            Map<Long, Intersection> intersections)
            throws XmlInvalideException {
        if (entrepots.isEmpty()) {
            return null;
        }
        if (entrepots.size() > 1) {
            throw new XmlInvalideException(
                    "Le plan indique plusieurs entrepôts (<entrepot>).");
        }
        return trouver(intersections,
                lireEntier(entrepots.get(0), "adresse"), "<entrepot> : adresse");
    }

    private static Intersection trouver(Map<Long, Intersection> intersections,
            long id, String contexte) throws XmlInvalideException {
        Intersection trouvee = intersections.get(id);
        if (trouvee == null) {
            throw new XmlInvalideException(contexte + " " + id
                    + " ne correspond à aucune intersection du plan.");
        }
        return trouvee;
    }

    private static String lireTexte(Element element, String attribut)
            throws XmlInvalideException {
        if (!element.hasAttribute(attribut)) {
            throw new XmlInvalideException("<" + element.getTagName()
                    + "> : attribut « " + attribut + " » manquant.");
        }
        return element.getAttribute(attribut);
    }

    private static long lireEntier(Element element, String attribut)
            throws XmlInvalideException {
        String texte = lireTexte(element, attribut);
        try {
            return Long.parseLong(texte.trim());
        } catch (NumberFormatException e) {
            throw new XmlInvalideException("<" + element.getTagName()
                    + "> : « " + attribut + " » doit être un entier (trouvé : « "
                    + texte + " »).", e);
        }
    }

    private static double lireReel(Element element, String attribut)
            throws XmlInvalideException {
        String texte = lireTexte(element, attribut);
        try {
            double valeur = Double.parseDouble(texte.trim());
            if (!Double.isFinite(valeur)) {
                throw new NumberFormatException(texte);
            }
            return valeur;
        } catch (NumberFormatException e) {
            throw new XmlInvalideException("<" + element.getTagName()
                    + "> : « " + attribut + " » doit être un nombre (trouvé : « "
                    + texte + " »).", e);
        }
    }

    /**
     * Refuse les erreurs de l'analyseur au lieu de les afficher dans la
     * console.
     */
    private static final class ErreursStrictes extends DefaultHandler {
        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }
    }
}
