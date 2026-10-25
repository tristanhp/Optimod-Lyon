package fr.insa.optimod.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 * Ensemble des demandes à livrer depuis un entrepôt. Les demandes peuvent être
 * ajoutées ou retirées après la lecture ; chaque modification prévient les
 * observateurs (design pattern Observer).
 */
public final class Livraison extends Observable {
    private final List<Demande> demandes;
    private final Entrepot entrepot;
    private final String heureDepart; // heure de départ de l'entrepôt

    /**
     * Crée une livraison.
     *
     * @param demandes demandes à livrer
     * @param entrepot entrepôt de départ
     * @param heureDepart heure de départ de la tournée
     */
    public Livraison(List<Demande> demandes, Entrepot entrepot,
            String heureDepart) {
        this.demandes = new ArrayList<>(demandes);
        this.entrepot = entrepot;
        this.heureDepart = heureDepart;
    }

    /**
     * Crée une livraison à l'heure de départ de l'entrepôt.
     *
     * @param demandes demandes à livrer
     * @param entrepot entrepôt de départ
     */
    public Livraison(List<Demande> demandes, Entrepot entrepot) {
        this(demandes, entrepot, entrepot.obtenirHeureDepart());
    }

    /**
     * Lit les demandes depuis un fichier XML, comme {@link Plan}.
     *
     * @param cheminFichierXml chemin du fichier XML
     * @throws IOException si le fichier est illisible
     * @throws SAXException si le XML ou ses données sont invalides
     */
    public Livraison(String cheminFichierXml) throws IOException, SAXException {
        this(creerLecteurXml().parse(new File(cheminFichierXml)));
    }

    /**
     * Lit les demandes depuis un flux XML.
     *
     * @param fluxXml contenu XML des demandes
     * @throws IOException si le flux est illisible
     * @throws SAXException si le XML ou ses données sont invalides
     */
    public Livraison(InputStream fluxXml) throws IOException, SAXException {
        this(creerLecteurXml().parse(fluxXml));
    }

    private Livraison(Document documentXml) throws SAXException {
        Element racine = documentXml.getDocumentElement();
        racine.normalize();
        if (!"demandeDeLivraisons".equals(racine.getTagName())) {
            throw new SAXException("Racine attendue : demandeDeLivraisons");
        }
        NodeList entrepots = racine.getElementsByTagName("entrepot");
        if (entrepots.getLength() != 1) {
            throw new SAXException("Un unique entrepôt est requis");
        }
        Element depot = (Element) entrepots.item(0);
        heureDepart = lireAttributObligatoire(depot, "heureDepart");
        try {
            entrepot = new Entrepot(
                    Long.parseLong(lireAttributObligatoire(depot, "adresse")),
                    heureDepart);
            List<Demande> demandesLues = new ArrayList<>();
            NodeList livraisons = racine.getElementsByTagName("livraison");
            for (int indiceElement = 0; indiceElement < livraisons.getLength(); indiceElement++) {
                Element element = (Element) livraisons.item(indiceElement);
                long collecte = Long.parseLong(
                        lireAttributObligatoire(element, "adresseEnlevement"));
                long livraison = Long.parseLong(
                        lireAttributObligatoire(element, "adresseLivraison"));
                int dureeCollecte = Integer.parseInt(
                        lireAttributObligatoire(element, "dureeEnlevement"));
                int dureeLivraison = Integer.parseInt(
                        lireAttributObligatoire(element, "dureeLivraison"));
                if (dureeCollecte < 0 || dureeLivraison < 0) {
                    throw new SAXException("Durée de service négative");
                }
                demandesLues.add(new Demande(collecte, livraison,
                        dureeCollecte, dureeLivraison));
            }
            demandes = demandesLues;
        } catch (NumberFormatException erreur) {
            throw new SAXException("Adresse ou durée invalide", erreur);
        }
    }

    private static String lireAttributObligatoire(Element element, String nom)
            throws SAXException {
        String valeur = element.getAttribute(nom);
        if (valeur.isBlank()) {
            throw new SAXException("Attribut requis : " + nom);
        }
        return valeur;
    }

    private static DocumentBuilder creerLecteurXml() {
        try {
            DocumentBuilderFactory fabriqueLecteurXml =
                    DocumentBuilderFactory.newInstance();
            fabriqueLecteurXml.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabriqueLecteurXml.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true);
            DocumentBuilder lecteurXml = fabriqueLecteurXml.newDocumentBuilder();
            lecteurXml.setErrorHandler(new DefaultHandler());
            return lecteurXml;
        } catch (ParserConfigurationException erreur) {
            throw new IllegalStateException(erreur);
        }
    }

    /**
     * Renvoie les demandes. La liste n'est pas modifiable par l'appelant mais
     * suit les ajouts et retraits faits par {@link #ajouterDemande} et
     * {@link #retirerDemande}.
     *
     * @return demandes à livrer, dans l'ordre (non modifiable)
     */
    public List<Demande> obtenirDemandes() {
        return Collections.unmodifiableList(demandes);
    }

    /**
     * Insère une demande et prévient les observateurs.
     *
     * @param position rang de la nouvelle demande, entre 0 et le nombre de
     *        demandes
     * @param demande demande à ajouter
     * @throws IndexOutOfBoundsException si la position est hors de la liste
     */
    public void ajouterDemande(int position, Demande demande) {
        demandes.add(position, demande);
        notifierObservateurs();
    }

    /**
     * Retire une demande et prévient les observateurs.
     *
     * @param position rang de la demande à retirer
     * @return la demande retirée
     * @throws IndexOutOfBoundsException si la position est hors de la liste
     */
    public Demande retirerDemande(int position) {
        Demande retiree = demandes.remove(position);
        notifierObservateurs();
        return retiree;
    }

    /** @return entrepôt de départ */
    public Entrepot obtenirEntrepot() {
        return entrepot;
    }

    /** @return heure de départ de la tournée, telle que lue dans le XML */
    public String obtenirHeureDepart() {
        return heureDepart;
    }
}
