package fr.insa.optimod.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Programme de livraisons : l'entrepôt, l'heure de départ, le nombre de
 * livreurs, toutes les demandes de livraison à traiter (lues dans un fichier
 * de demandes) et, une fois calculées, les tournées des livreurs. Chaque
 * modification prévient les observateurs (design pattern Observer).
 *
 * <p>Les livreurs sont numérotés à partir de 1 ; il y en a un au départ, à
 * qui toutes les demandes d'un fichier sont confiées. Quand des tournées
 * existent, il y en a une par livreur, et les deux arrêts de chaque demande du
 * livreur <i>k</i> sont dans la tournée <i>k</i>, la collecte avant la
 * livraison : c'est à l'appelant qui modifie les demandes de garder les
 * tournées cohérentes (voir les commandes du contrôleur).
 */
public final class ProgrammeLivraisons extends Observable {

    /**
     * Demande de livraison : un colis à récupérer au point de collecte puis à
     * déposer au point de livraison, par un livreur.
     *
     * @param collecte point de collecte
     * @param livraison point de livraison
     * @param dureeCollecte durée sur place à la collecte, en secondes
     * @param dureeLivraison durée sur place à la livraison, en secondes
     * @param livreur numéro du livreur qui sert la demande (à partir de 1)
     */
    public record Demande(Noeud collecte, Noeud livraison, int dureeCollecte,
            int dureeLivraison, int livreur) {

        /**
         * Crée une demande.
         *
         * @param collecte point de collecte
         * @param livraison point de livraison
         * @param dureeCollecte durée sur place à la collecte, en secondes
         * @param dureeLivraison durée sur place à la livraison, en secondes
         * @param livreur numéro du livreur (à partir de 1)
         * @throws IllegalArgumentException si une durée est négative ou si le
         *         numéro de livreur est inférieur à 1
         */
        public Demande {
            Objects.requireNonNull(collecte, "collecte");
            Objects.requireNonNull(livraison, "livraison");
            if (dureeCollecte < 0 || dureeLivraison < 0) {
                throw new IllegalArgumentException("Durée sur place négative");
            }
            if (livreur < 1) {
                throw new IllegalArgumentException(
                        "Numéro de livreur invalide : " + livreur);
            }
        }

        /**
         * Crée une demande confiée au livreur 1.
         *
         * @param collecte point de collecte
         * @param livraison point de livraison
         * @param dureeCollecte durée sur place à la collecte, en secondes
         * @param dureeLivraison durée sur place à la livraison, en secondes
         * @throws IllegalArgumentException si une durée est négative
         */
        public Demande(Noeud collecte, Noeud livraison, int dureeCollecte,
                int dureeLivraison) {
            this(collecte, livraison, dureeCollecte, dureeLivraison, 1);
        }

        /**
         * Renvoie une copie de la demande confiée à un autre livreur.
         *
         * @param autreLivreur numéro du livreur (à partir de 1)
         * @return la copie ; la demande d'origine ne change pas
         * @throws IllegalArgumentException si le numéro est inférieur à 1
         */
        public Demande avecLivreur(int autreLivreur) {
            return new Demande(collecte, livraison, dureeCollecte,
                    dureeLivraison, autreLivreur);
        }
    }

    private final Noeud entrepot;
    private final LocalTime heureDepart;
    private final List<Demande> demandes;

    /** Nombre de livreurs, au moins 1. */
    private int nombreLivreurs = 1;

    /** Tournées par numéro de livreur ; vide tant que rien n'est calculé. */
    private final SortedMap<Integer, Tournee> tournees = new TreeMap<>();

    /**
     * Crée un programme d'un seul livreur.
     *
     * @param entrepot adresse de l'entrepôt
     * @param heureDepart heure de départ de l'entrepôt
     * @param demandes demandes à traiter, dans l'ordre
     * @throws IllegalArgumentException si une demande est confiée à un autre
     *         livreur que le livreur 1
     */
    public ProgrammeLivraisons(Noeud entrepot, LocalTime heureDepart,
            List<Demande> demandes) {
        this.entrepot = Objects.requireNonNull(entrepot, "entrepot");
        this.heureDepart = Objects.requireNonNull(heureDepart, "heureDepart");
        this.demandes = new ArrayList<>(demandes);
        for (Demande demande : this.demandes) {
            verifierLivreur(demande);
        }
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
     * @throws IllegalArgumentException si le livreur de la demande n'existe
     *         pas
     */
    public void ajouterDemande(int position, Demande demande) {
        verifierLivreur(demande);
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

    /**
     * Remplace la demande à cette position (par exemple pour la confier à un
     * autre livreur) et prévient les observateurs.
     *
     * @param position rang de la demande à remplacer
     * @param demande nouvelle demande
     * @return la demande remplacée
     * @throws IndexOutOfBoundsException si la position est hors de la liste
     * @throws IllegalArgumentException si le livreur de la demande n'existe
     *         pas
     */
    public Demande remplacerDemande(int position, Demande demande) {
        verifierLivreur(demande);
        Demande remplacee = demandes.set(position, demande);
        notifierObservateurs();
        return remplacee;
    }

    private void verifierLivreur(Demande demande) {
        if (demande.livreur() > nombreLivreurs) {
            throw new IllegalArgumentException("Livreur " + demande.livreur()
                    + " inconnu : il y a " + nombreLivreurs + " livreur(s).");
        }
    }

    /** @return nombre de livreurs, 1 par défaut */
    public int obtenirNombreLivreurs() {
        return nombreLivreurs;
    }

    /**
     * Change le nombre de livreurs et prévient les observateurs. Si des
     * tournées existent, un livreur ajouté reçoit une tournée vide et la
     * tournée d'un livreur retiré disparaît. Seuls les derniers livreurs
     * sont retirés, et seulement s'ils n'ont aucune demande.
     *
     * @param nombre nouveau nombre de livreurs
     * @throws IllegalArgumentException si le nombre est inférieur à 1 ou si
     *         un livreur retiré a encore des demandes
     */
    public void definirNombreLivreurs(int nombre) {
        if (nombre < 1) {
            throw new IllegalArgumentException("Il faut au moins un livreur.");
        }
        for (Demande demande : demandes) {
            if (demande.livreur() > nombre) {
                throw new IllegalArgumentException("Le livreur "
                        + demande.livreur() + " a encore des demandes.");
            }
        }
        if (!tournees.isEmpty()) {
            tournees.tailMap(nombre + 1).clear();
            for (int livreur = nombreLivreurs + 1; livreur <= nombre;
                    livreur++) {
                tournees.put(livreur, Tournee.vide(entrepot, heureDepart));
            }
        }
        nombreLivreurs = nombre;
        notifierObservateurs();
    }

    /**
     * Renvoie les tournées des livreurs, triées par numéro de livreur. C'est
     * une copie : elle ne suit pas les modifications suivantes.
     *
     * @return tournées par numéro de livreur (non modifiable), vide tant
     *         qu'aucune tournée n'est calculée
     */
    public Map<Integer, Tournee> obtenirTournees() {
        return Collections.unmodifiableSortedMap(new TreeMap<>(tournees));
    }

    /**
     * Range les tournées (issues de « Calculer », ou remises par une
     * annulation) et prévient les observateurs. Une table vide retire toutes
     * les tournées.
     *
     * @param nouvellesTournees une tournée par livreur, de 1 au nombre de
     *        livreurs, ou aucune
     * @throws IllegalArgumentException s'il ne manque ou s'il y a en trop
     *         une tournée de livreur
     */
    public void definirTournees(Map<Integer, Tournee> nouvellesTournees) {
        if (!nouvellesTournees.isEmpty()) {
            boolean complet = nouvellesTournees.size() == nombreLivreurs;
            for (int livreur = 1; livreur <= nombreLivreurs; livreur++) {
                complet &= nouvellesTournees.get(livreur) != null;
            }
            if (!complet) {
                throw new IllegalArgumentException("Il faut une tournée par "
                        + "livreur, de 1 à " + nombreLivreurs + ".");
            }
        }
        tournees.clear();
        tournees.putAll(nouvellesTournees);
        notifierObservateurs();
    }

    /**
     * Remplace la tournée d'un livreur (après un recalcul local) et prévient
     * les observateurs.
     *
     * @param livreur numéro du livreur
     * @param tournee sa nouvelle tournée
     * @throws IllegalStateException si aucune tournée n'est calculée
     * @throws IllegalArgumentException si le livreur n'existe pas
     */
    public void definirTournee(int livreur, Tournee tournee) {
        if (tournees.isEmpty()) {
            throw new IllegalStateException("Aucune tournée calculée.");
        }
        if (livreur < 1 || livreur > nombreLivreurs) {
            throw new IllegalArgumentException("Livreur inconnu : " + livreur);
        }
        tournees.put(livreur, Objects.requireNonNull(tournee, "tournee"));
        notifierObservateurs();
    }

    /** Retire toutes les tournées et prévient les observateurs. */
    public void effacerTournees() {
        if (!tournees.isEmpty()) {
            tournees.clear();
            notifierObservateurs();
        }
    }
}
