package fr.insa.optimod.model;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;

/**
 * Sauvegarde XML version 1 pour le modèle actuel à un livreur.
 * Le plan est référencé, les demandes et l'ordre sont conservés.
 *
 * @param plan plan rechargé
 * @param fichierPlan fichier du plan
 * @param programme programme restauré
 * @param tournee tournée recalculée sans TSP
 */
public record SauvegardeTournee(Plan plan, Path fichierPlan,
        ProgrammeLivraisons programme, Tournee tournee) {

    /**
     * Écrit une sauvegarde sans recopier le plan.
     *
     * @param fichier destination XML
     * @param fichierPlan référence du plan
     * @param programme programme courant
     * @param tournee tournée courante
     * @throws IOException si l'écriture échoue
     */
    public static void sauvegarder(Path fichier, Path fichierPlan,
            ProgrammeLivraisons programme, Tournee tournee)
            throws IOException {
        try (OutputStream sortie = Files.newOutputStream(fichier)) {
            XMLStreamWriter xml = XMLOutputFactory.newFactory()
                    .createXMLStreamWriter(sortie, "UTF-8");
            try {
                xml.writeStartDocument("UTF-8", "1.0");
                xml.writeStartElement("sauvegardeTournee");
                xml.writeAttribute("version", "1");
                xml.writeAttribute("nombreLivreurs", "1");
                xml.writeAttribute("plan", fichierPlan.toAbsolutePath()
                        .normalize().toString());
                xml.writeAttribute("entrepot", Long.toString(
                        programme.obtenirEntrepot().identifiant()));
                xml.writeAttribute("heureDepart",
                        programme.obtenirHeureDepart().toString());
                for (Demande demande : programme.obtenirDemandes()) {
                    xml.writeEmptyElement("demande");
                    xml.writeAttribute("collecte",
                            Long.toString(demande.collecte().identifiant()));
                    xml.writeAttribute("livraison",
                            Long.toString(demande.livraison().identifiant()));
                    xml.writeAttribute("dureeCollecte",
                            Integer.toString(demande.dureeCollecte()));
                    xml.writeAttribute("dureeLivraison",
                            Integer.toString(demande.dureeLivraison()));
                }
                for (Tournee.Arret arret : tournee.arrets()) {
                    xml.writeEmptyElement("arret");
                    xml.writeAttribute("noeud",
                            Long.toString(arret.noeud().identifiant()));
                    xml.writeAttribute("duree", Long.toString(
                            arret.arrivee().until(arret.depart(),
                                    ChronoUnit.SECONDS)));
                    xml.writeAttribute("debutPlage",
                            arret.debutPlage().toString());
                    xml.writeAttribute("finPlage", arret.finPlage().toString());
                }
                xml.writeEndElement();
                xml.writeEndDocument();
            } finally {
                xml.close();
            }
        } catch (XMLStreamException erreur) {
            throw new IOException("Impossible d'écrire la sauvegarde.", erreur);
        }
    }

    /**
     * Recharge le plan, les demandes et les arrêts avant tout affichage.
     *
     * @param fichier sauvegarde XML
     * @return état restauré, indépendant de l'état courant
     * @throws SAXException si le fichier ou le plan est invalide ou illisible
     */
    public static SauvegardeTournee restaurer(Path fichier)
            throws SAXException {
        try {
            Element racine = Plan.creerLecteurXml().parse(fichier.toFile())
                    .getDocumentElement();
            if (!"sauvegardeTournee".equals(racine.getTagName())
                    || !"1".equals(attribut(racine, "version"))
                    || !"1".equals(attribut(racine, "nombreLivreurs"))) {
                throw new SAXException(
                        "Format de sauvegarde non pris en charge.");
            }
            Path fichierPlan = fichier.toAbsolutePath()
                    .resolveSibling(attribut(racine, "plan")).normalize();
            Plan plan = new Plan(fichierPlan.toString());
            LocalTime depart = LocalTime.parse(attribut(racine, "heureDepart"));
            Noeud entrepot = adresse(racine, "entrepot", plan);
            List<Demande> demandes = new ArrayList<>();
            NodeList elements = racine.getElementsByTagName("demande");
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                demandes.add(new Demande(adresse(element, "collecte", plan),
                        adresse(element, "livraison", plan),
                        entier(element, "dureeCollecte"),
                        entier(element, "dureeLivraison")));
            }
            ProgrammeLivraisons programme = new ProgrammeLivraisons(entrepot,
                    depart, demandes);
            List<Noeud> adresses = new ArrayList<>();
            List<Integer> durees = new ArrayList<>();
            NodeList arrets = racine.getElementsByTagName("arret");
            for (int i = 0; i < arrets.getLength(); i++) {
                Element arret = (Element) arrets.item(i);
                adresses.add(adresse(arret, "noeud", plan));
                durees.add(entier(arret, "duree"));
            }
            if (adresses.isEmpty() || !adresses.get(0).equals(entrepot)
                    || !adresses.get(adresses.size() - 1).equals(entrepot)) {
                throw new SAXException("La tournée doit partir de l'entrepôt "
                        + "et y revenir.");
            }
            int nombreAttendu = demandes.isEmpty()
                    ? 1 : 2 * demandes.size() + 2;
            if (adresses.size() != nombreAttendu
                    || !respecteDemandes(adresses, durees, demandes,
                            new int[demandes.size()], 1)) {
                throw new SAXException("Les arrêts ne respectent pas les "
                        + "demandes et leur contrainte d'ordre.");
            }
            Tournee tournee = plan.recalculerTournee(depart, adresses, durees);
            List<Tournee.Arret> passages = new ArrayList<>();
            for (int i = 0; i < arrets.getLength(); i++) {
                Element element = (Element) arrets.item(i);
                Tournee.Arret arret = tournee.arrets().get(i);
                if (element.hasAttribute("debutPlage")
                        || element.hasAttribute("finPlage")) {
                    arret = new Tournee.Arret(arret.noeud(), arret.arrivee(),
                            arret.depart(), LocalTime.parse(attribut(element,
                                    "debutPlage")), LocalTime.parse(attribut(
                                            element, "finPlage")));
                }
                passages.add(arret);
            }
            return new SauvegardeTournee(plan, fichierPlan, programme,
                    new Tournee(passages, tournee.chemins()));
        } catch (IOException | DateTimeException | IllegalArgumentException
                | IllegalStateException erreur) {
            throw new SAXException("Impossible de restaurer la tournée : "
                    + erreur.getMessage(), erreur);
        }
    }

    /**
     * Retrouve une affectation valide même si des demandes partagent une
     * adresse et une durée. 0 : avant collecte, 1 : collectée, 2 : livrée.
     */
    private static boolean respecteDemandes(List<Noeud> adresses,
            List<Integer> durees, List<Demande> demandes, int[] progression,
            int rang) {
        if (rang >= adresses.size() - 1) {
            for (int etape : progression) {
                if (etape != 2) {
                    return false;
                }
            }
            return true;
        }
        for (int i = 0; i < demandes.size(); i++) {
            Demande demande = demandes.get(i);
            int etape = progression[i];
            Noeud adresse = etape == 0 ? demande.collecte()
                    : demande.livraison();
            int duree = etape == 0 ? demande.dureeCollecte()
                    : demande.dureeLivraison();
            if (etape < 2 && adresse.equals(adresses.get(rang))
                    && duree == durees.get(rang)) {
                progression[i]++;
                if (respecteDemandes(adresses, durees, demandes, progression,
                        rang + 1)) {
                    return true;
                }
                progression[i]--;
            }
        }
        return false;
    }

    private static String attribut(Element element, String nom)
            throws SAXException {
        String valeur = element.getAttribute(nom);
        if (valeur.isBlank()) {
            throw new SAXException("Attribut requis : " + nom);
        }
        return valeur;
    }

    private static int entier(Element element, String nom) throws SAXException {
        int valeur = Integer.parseInt(attribut(element, nom));
        if (valeur < 0) {
            throw new SAXException("Durée sur place négative.");
        }
        return valeur;
    }

    private static Noeud adresse(Element element, String nom, Plan plan)
            throws SAXException {
        Noeud noeud = plan.obtenirNoeud(Long.parseLong(attribut(element, nom)));
        if (noeud == null) {
            throw new SAXException("Adresse absente du plan : "
                    + attribut(element, nom));
        }
        return noeud;
    }
}
