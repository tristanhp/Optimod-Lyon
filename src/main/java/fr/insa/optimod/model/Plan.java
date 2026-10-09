package fr.insa.optimod.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Plan{
    Map<Noeud, List<Troncon>> Plan;
    private final List<Troncon> troncons=null;
    private List<Noeud> noeuds;

    public Plan(String xmlFileName) {
    try {
        File xmlFile = new File(xmlFileName);

        DocumentBuilderFactory dbFactory =
            DocumentBuilderFactory.newInstance();

        DocumentBuilder dBuilder =
            dbFactory.newDocumentBuilder();

        Document doc = dBuilder.parse(xmlFile);

        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("noeud");

        for (int i = 0; i < nodeList.getLength(); i++) {

            Element element = (Element) nodeList.item(i);

            Integer id = Integer.parseInt(element.getAttribute("id"));

            double latitude = Double.parseDouble(
                element.getAttribute("latitude")
            );

            double longitude = Double.parseDouble(
                element.getAttribute("longitude")
            );

            Noeud noeud = new Noeud(id, latitude, longitude);

            noeuds.add(noeud);
        }
            } catch (ParserConfigurationException | SAXException | IOException e) {
                e.printStackTrace();
            }

        }
        //faire un dictionnaire idNoeud vers Noeud
        //pour pouvoir créer les troncons

        //a voir pour adapter en un tuple 
}

