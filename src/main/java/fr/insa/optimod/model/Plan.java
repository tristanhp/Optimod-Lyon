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
     * @param xmlFileName chemin du fichier XML
     * @throws IOException si le fichier est illisible
     * @throws SAXException si le XML est invalide
     */
    public Plan(String xmlFileName) throws IOException, SAXException {
        this(nouveauBuilder().parse(new File(xmlFileName)));
    }

    /**
     * Lit un plan depuis un flux XML.
     *
     * @param xml contenu XML du plan
     * @throws IOException si le flux est illisible
     * @throws SAXException si le XML est invalide
     */
    public Plan(InputStream xml) throws IOException, SAXException {
        this(nouveauBuilder().parse(xml));
    }

    private Plan(Document doc) throws SAXException {
        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("noeud");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            long id = Long.parseLong(element.getAttribute("id"));
            double latitude = Double.parseDouble(element.getAttribute("latitude"));
            double longitude = Double.parseDouble(element.getAttribute("longitude"));
            Noeud noeud = new Noeud(id, longitude, latitude);
            noeuds.put(id, noeud);
            tronconsSortants.put(noeud, new ArrayList<>());
        }

        //dictionnaire idNoeud vers Noeud pour pouvoir créer les troncons
        NodeList tronconList = doc.getElementsByTagName("troncon");
        for (int i = 0; i < tronconList.getLength(); i++) {
            Element element = (Element) tronconList.item(i);
            Noeud origine = trouverNoeud(element.getAttribute("origine"));
            Noeud destination = trouverNoeud(element.getAttribute("destination"));
            double longueur = Double.parseDouble(element.getAttribute("longueur"));
            Troncon troncon = new Troncon(origine, destination, longueur,
                    element.getAttribute("nomRue"));
            troncons.add(troncon);
            tronconsSortants.get(origine).add(troncon);
        }
    }

    private static DocumentBuilder nouveauBuilder() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler()); // pas de trace sur stderr
            return builder;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Noeud trouverNoeud(String id) throws SAXException {
        Noeud noeud = noeuds.get(Long.parseLong(id));
        if (noeud == null) {
            throw new SAXException("Tronçon vers un noeud inconnu : " + id);
        }
        return noeud;
    }

    /** @return noeuds du plan (non modifiable) */
    public Collection<Noeud> getNoeuds() {
        return Collections.unmodifiableCollection(noeuds.values());
    }

    /**
     * Renvoie un noeud à partir de son id.
     *
     * @param id identifiant du noeud
     * @return le noeud, ou {@code null} s'il n'existe pas
     */
    public Noeud getNoeud(long id) {
        return noeuds.get(id);
    }

    /** @return tronçons du plan (non modifiable) */
    public List<Troncon> getTroncons() {
        return Collections.unmodifiableList(troncons);
    }

    /**
     * Renvoie les tronçons qui partent d'un noeud.
     *
     * @param noeud noeud de départ
     * @return tronçons sortants (non modifiable, vide si le noeud est inconnu)
     */
    public List<Troncon> getTronconsSortants(Noeud noeud) {
        return Collections.unmodifiableList(tronconsSortants.getOrDefault(noeud, List.of()));
    }
}
