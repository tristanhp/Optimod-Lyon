package fr.insa.optimod.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Plan de la ville lu depuis un fichier XML : des noeuds et des tronçons.
 */
public final class Plan {
    private final Map<Long, Noeud> noeuds = new LinkedHashMap<>();
    private final Map<Noeud, List<Troncon>> tronconsSortants = new LinkedHashMap<>();
    private final List<Troncon> troncons = new ArrayList<>();

    /**
     * Lit un plan depuis un fichier XML.
     *
     * @param cheminFichierXml chemin du fichier XML
     * @throws IOException si le fichier est illisible
     * @throws SAXException si le XML est invalide
     */
    public Plan(String cheminFichierXml) throws IOException, SAXException {
        this(creerLecteurXml().parse(new File(cheminFichierXml)));
    }

    /**
     * Lit un plan depuis un flux XML.
     *
     * @param fluxXml contenu XML du plan
     * @throws IOException si le flux est illisible
     * @throws SAXException si le XML est invalide
     */
    public Plan(InputStream fluxXml) throws IOException, SAXException {
        this(creerLecteurXml().parse(fluxXml));
    }

    private Plan(Document documentXml) throws SAXException {
        documentXml.getDocumentElement().normalize();

        NodeList elementsNoeuds = documentXml.getElementsByTagName("noeud");
        for (int indiceElement = 0; indiceElement < elementsNoeuds.getLength(); indiceElement++) {
            Element element = (Element) elementsNoeuds.item(indiceElement);
            long identifiant = Long.parseLong(element.getAttribute("id"));
            double latitude = Double.parseDouble(element.getAttribute("latitude"));
            double longitude = Double.parseDouble(element.getAttribute("longitude"));
            Noeud noeud = new Noeud(identifiant, longitude, latitude);
            noeuds.put(identifiant, noeud);
            tronconsSortants.put(noeud, new ArrayList<>());
        }

        //dictionnaire idNoeud vers Noeud pour pouvoir créer les troncons
        NodeList elementsTroncons = documentXml.getElementsByTagName("troncon");
        for (int indiceElement = 0; indiceElement < elementsTroncons.getLength(); indiceElement++) {
            Element element = (Element) elementsTroncons.item(indiceElement);
            Noeud origine = trouverNoeud(element.getAttribute("origine"));
            Noeud destination = trouverNoeud(element.getAttribute("destination"));
            double longueur = Double.parseDouble(element.getAttribute("longueur"));
            Troncon troncon = new Troncon(origine, destination, longueur,
                    element.getAttribute("nomRue"));
            troncons.add(troncon);
            tronconsSortants.get(origine).add(troncon);
        }
    }

    private static DocumentBuilder creerLecteurXml() {
        try {
            DocumentBuilderFactory fabriqueLecteurXml = DocumentBuilderFactory.newInstance();
            fabriqueLecteurXml.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabriqueLecteurXml.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder lecteurXml = fabriqueLecteurXml.newDocumentBuilder();
            lecteurXml.setErrorHandler(new DefaultHandler()); // pas de trace sur stderr
            return lecteurXml;
        } catch (ParserConfigurationException erreur) {
            throw new IllegalStateException(erreur);
        }
    }

    private Noeud trouverNoeud(String identifiant) throws SAXException {
        Noeud noeud = noeuds.get(Long.parseLong(identifiant));
        if (noeud == null) {
            throw new SAXException("Tronçon vers un noeud inconnu : " + identifiant);
        }
        return noeud;
    }

    /** @return noeuds du plan (non modifiable) */
    public Collection<Noeud> obtenirNoeuds() {
        return Collections.unmodifiableCollection(noeuds.values());
    }

    /**
     * Renvoie un noeud à partir de son identifiant.
     *
     * @param identifiant identifiant du noeud
     * @return le noeud, ou {@code null} s'il n'existe pas
     */
    public Noeud obtenirNoeud(long identifiant) {
        return noeuds.get(identifiant);
    }

    /** @return tronçons du plan (non modifiable) */
    public List<Troncon> obtenirTroncons() {
        return Collections.unmodifiableList(troncons);
    }

    /**
     * Renvoie les tronçons qui partent d'un noeud.
     *
     * @param noeud noeud de départ
     * @return tronçons sortants (non modifiable, vide si le noeud est inconnu)
     */
    public List<Troncon> obtenirTronconsSortants(Noeud noeud) {
        return Collections.unmodifiableList(tronconsSortants.getOrDefault(noeud, List.of()));
    }

}
