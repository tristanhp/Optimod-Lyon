package fr.insa.optimod.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Programme de livraisons : l'entrepôt, l'heure de départ et toutes les
 * demandes de livraison à traiter, lues dans un fichier de demandes. Les
 * demandes peuvent être ajoutées ou retirées ensuite ; chaque modification
 * prévient les observateurs (design pattern Observer).
 */
public final class ProgrammeLivraisons extends Observable {

    /**
     * Demande de livraison : un colis à récupérer au point de collecte puis à
     * déposer au point de livraison.
     *
     * @param collecte point de collecte
     * @param livraison point de livraison
     * @param dureeCollecte durée sur place à la collecte, en secondes
     * @param dureeLivraison durée sur place à la livraison, en secondes
     */
    public record Demande(Noeud collecte, Noeud livraison, int dureeCollecte,
            int dureeLivraison) {

        /**
         * Crée une demande.
         *
         * @param collecte point de collecte
         * @param livraison point de livraison
         * @param dureeCollecte durée sur place à la collecte, en secondes
         * @param dureeLivraison durée sur place à la livraison, en secondes
         * @throws IllegalArgumentException si une durée est négative
         */
        public Demande {
            Objects.requireNonNull(collecte, "collecte");
            Objects.requireNonNull(livraison, "livraison");
            if (dureeCollecte < 0 || dureeLivraison < 0) {
                throw new IllegalArgumentException("Durée sur place négative");
            }
        }
    }

    private final Noeud entrepot;
    private final LocalTime heureDepart;
    private final List<Demande> demandes;

    /**
     * Crée un programme.
     *
     * @param entrepot adresse de l'entrepôt
     * @param heureDepart heure de départ de l'entrepôt
     * @param demandes demandes à traiter, dans l'ordre
     */
    public ProgrammeLivraisons(Noeud entrepot, LocalTime heureDepart,
            List<Demande> demandes) {
        this.entrepot = Objects.requireNonNull(entrepot, "entrepot");
        this.heureDepart = Objects.requireNonNull(heureDepart, "heureDepart");
        this.demandes = new ArrayList<>(demandes);
    }

    /**
     * Lit un fichier de demandes. Les adresses doivent être des nœuds du plan.
     *
     * @param cheminFichierXml chemin du fichier XML
     * @param plan plan où se trouvent les adresses
     * @throws IOException si le fichier est illisible
     * @throws SAXException si le XML ou ses données sont invalides, ou si une
     *         adresse est absente du plan
     */
    public ProgrammeLivraisons(String cheminFichierXml, Plan plan)
            throws IOException, SAXException {
        this(Plan.creerLecteurXml().parse(new File(cheminFichierXml)), plan);
    }

    /**
     * Lit des demandes depuis un flux XML.
     *
     * @param fluxXml contenu XML des demandes
     * @param plan plan où se trouvent les adresses
     * @throws IOException si le flux est illisible
     * @throws SAXException si le XML ou ses données sont invalides, ou si une
     *         adresse est absente du plan
     */
    public ProgrammeLivraisons(InputStream fluxXml, Plan plan)
            throws IOException, SAXException {
        this(Plan.creerLecteurXml().parse(fluxXml), plan);
    }

    private ProgrammeLivraisons(Document documentXml, Plan plan)
            throws SAXException {
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
        heureDepart = lireHeure(lireAttribut(depot, "heureDepart"));
        entrepot = lireAdresse(depot, "adresse", plan);
        demandes = new ArrayList<>();
        NodeList livraisons = racine.getElementsByTagName("livraison");
        for (int indice = 0; indice < livraisons.getLength(); indice++) {
            Element element = (Element) livraisons.item(indice);
            try {
                demandes.add(new Demande(
                        lireAdresse(element, "adresseEnlevement", plan),
                        lireAdresse(element, "adresseLivraison", plan),
                        Integer.parseInt(lireAttribut(element,
                                "dureeEnlevement")),
                        Integer.parseInt(lireAttribut(element,
                                "dureeLivraison"))));
            } catch (IllegalArgumentException erreur) {
                throw new SAXException("Durée sur place invalide : "
                        + erreur.getMessage(), erreur);
            }
        }
    }

    private static String lireAttribut(Element element, String nom)
            throws SAXException {
        String valeur = element.getAttribute(nom);
        if (valeur.isBlank()) {
            throw new SAXException("Attribut requis : " + nom);
        }
        return valeur;
    }

    private static Noeud lireAdresse(Element element, String nom, Plan plan)
            throws SAXException {
        String valeur = lireAttribut(element, nom);
        Noeud noeud;
        try {
            noeud = plan.obtenirNoeud(Long.parseLong(valeur));
        } catch (NumberFormatException erreur) {
            throw new SAXException("Adresse invalide : " + valeur, erreur);
        }
        if (noeud == null) {
            throw new SAXException("Adresse " + valeur
                    + " absente du plan chargé");
        }
        return noeud;
    }

    /**
     * Lit une heure de départ écrite {@code 8:0:0}, {@code 08:00} ou
     * {@code 08:00:30}.
     */
    static LocalTime lireHeure(String texte) throws SAXException {
        String[] parties = texte.trim().split(":");
        try {
            if (parties.length < 2 || parties.length > 3) {
                throw new NumberFormatException(texte);
            }
            return LocalTime.of(Integer.parseInt(parties[0]),
                    Integer.parseInt(parties[1]),
                    parties.length == 3 ? Integer.parseInt(parties[2]) : 0);
        } catch (RuntimeException erreur) {
            throw new SAXException("Heure de départ invalide : " + texte,
                    erreur);
        }
    }

    /** @return adresse de l'entrepôt */
    public Noeud obtenirEntrepot() {
        return entrepot;
    }

    /** @return heure de départ de l'entrepôt */
    public LocalTime obtenirHeureDepart() {
        return heureDepart;
    }

    /**
     * Renvoie les demandes. La liste n'est pas modifiable par l'appelant mais
     * suit les ajouts et retraits faits par {@link #ajouterDemande} et
     * {@link #retirerDemande}.
     *
     * @return demandes à traiter, dans l'ordre (non modifiable)
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
}
