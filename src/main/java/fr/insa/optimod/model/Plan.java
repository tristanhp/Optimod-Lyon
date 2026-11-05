package fr.insa.optimod.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.insa.optimod.algo.tsp.GrapheDepandance;
import fr.insa.optimod.algo.tsp.TSP;
import fr.insa.optimod.algo.tsp.TSP.ResultatCalculTSP;
import fr.insa.optimod.algo.tsp.TSP5;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee.TypeArret;

/**
 * Plan de la ville lu dans un fichier de plan : des nœuds et des tronçons.
 *
 * <p>
 * Le plan calcule aussi les tournées d'un programme de livraisons, une par
 * livreur ({@link #calculerTournees}) : plus courts chemins entre les
 * adresses (A*), meilleur ordre de passage avec la contrainte d'ordre
 * (voyageur de commerce, {@link TSP5}), puis heures de passage.
 *
 * <p>
 * Le voyageur de commerce ne sert qu'au calcul complet. Quand l'utilisateur
 * supprime une demande, l'ordre des autres arrêts du livreur est gardé, seuls
 * les trajets des jonctions créées sont recalculés et les heures sont
 * refaites ({@link #calculerTourneeSansDemande}) : c'est le recalcul local.
 */
public final class Plan {

    /** Vitesse du livreur, en km/h. */
    public static final double VITESSE_KMH = 15;

    /** Heure limite de retour à l'entrepôt. */
    public static final LocalTime HEURE_LIMITE_RETOUR = LocalTime.of(13, 0);

    /**
     * Limite de temps du calcul des ordres de passage, en millisecondes,
     * partagée entre les livreurs qui ont des demandes.
     */
    private static final int LIMITE_TEMPS_TSP_MS = 10_000;

    /** Secondes dans une journée, au-delà desquelles une heure repart de 0. */
    private static final int SECONDES_PAR_JOUR = 24 * 3600;

    /** Rayon moyen de la Terre, en mètres. */
    private static final double RAYON_TERRE = 6_371_000;

    /**
     * Coefficient appliqué à la distance à vol d'oiseau. Les longueurs des
     * fichiers fournis sont arrondies et descendent jusqu'à 0,91 fois la
     * distance à vol d'oiseau : sans ce coefficient, l'heuristique
     * surestimerait certains chemins et A* ne serait plus exact.
     */
    private static final double COEFFICIENT_HEURISTIQUE = 0.9;

    /** Nœud à explorer par A*, avec sa distance depuis la source et g + h. */
    private record Etiquette(Noeud noeud, double distance, double estimation) {
    }

    /**
     * Sommet du voyageur de commerce : une adresse de la tournée, sa durée
     * sur place (en secondes), son type et sa demande ({@code null} pour
     * l'entrepôt).
     */
    private record Sommet(Noeud adresse, int duree, TypeArret type,
            Demande demande) {
    }

    /**
     * Ordre de passage trouvé par le voyageur de commerce.
     *
     * @param sommets les sommets dans l'ordre de passage, en commençant par 0
     * @param optimal vrai si l'ordre est prouvé optimal, faux si la limite de
     *                temps a coupé la recherche
     */
    private record OrdreDePassage(List<Integer> sommets, boolean optimal) {
    }

    /**
     * Ordres de passage optimaux déjà trouvés par le voyageur de commerce, car
     * il est coûteux de le relancer : un par liste de sommets d'un livreur
     * (adresses, durées, types et demandes). Un ordre trouvé à la limite de
     * temps, non prouvé optimal, n'est pas gardé : un nouveau « Calculer »
     * cherche de nouveau un meilleur ordre. Seul le calcul complet les
     * consulte, jamais une modification de l'utilisateur, qui garde l'ordre
     * affiché. Le plan étant relu à chaque import, la mémoire repart de zéro
     * avec lui. Le calcul tourne hors du thread de l'interface : la table est
     * concurrente.
     */
    private final Map<List<Sommet>, List<Integer>> ordresMemorises = new ConcurrentHashMap<>();

    private final Map<Long, Noeud> noeuds = new LinkedHashMap<>();
    private final Map<Noeud, List<Troncon>> tronconsSortants = new LinkedHashMap<>();
    private final List<Troncon> troncons = new ArrayList<>();

    /**
     * Lit un plan depuis un fichier XML.
     *
     * @param cheminFichierXml chemin du fichier XML
     * @throws IOException  si le fichier est illisible
     * @throws SAXException si le XML est invalide
     */
    public Plan(String cheminFichierXml) throws IOException, SAXException {
        this(creerLecteurXml().parse(new File(cheminFichierXml)));
    }

    /**
     * Lit un plan depuis un flux XML.
     *
     * @param fluxXml contenu XML du plan
     * @throws IOException  si le flux est illisible
     * @throws SAXException si le XML est invalide
     */
    public Plan(InputStream fluxXml) throws IOException, SAXException {
        this(creerLecteurXml().parse(fluxXml));
    }

    private Plan(Document documentXml) throws SAXException {
        documentXml.getDocumentElement().normalize();
        try {
            NodeList elementsNoeuds = documentXml.getElementsByTagName("noeud");
            for (int indice = 0; indice < elementsNoeuds.getLength(); indice++) {
                Element element = (Element) elementsNoeuds.item(indice);
                Noeud noeud = new Noeud(
                        Long.parseLong(element.getAttribute("id")),
                        Double.parseDouble(element.getAttribute("longitude")),
                        Double.parseDouble(element.getAttribute("latitude")));
                noeuds.put(noeud.identifiant(), noeud);
                tronconsSortants.put(noeud, new ArrayList<>());
            }
            NodeList elementsTroncons = documentXml.getElementsByTagName("troncon");
            for (int indice = 0; indice < elementsTroncons.getLength(); indice++) {
                Element element = (Element) elementsTroncons.item(indice);
                Noeud origine = trouverNoeud(element.getAttribute("origine"));
                Troncon troncon = new Troncon(origine,
                        trouverNoeud(element.getAttribute("destination")),
                        Double.parseDouble(element.getAttribute("longueur")),
                        element.getAttribute("nomRue"));
                troncons.add(troncon);
                tronconsSortants.get(origine).add(troncon);
            }
        } catch (NumberFormatException erreur) {
            throw new SAXException("Nombre invalide dans le plan : "
                    + erreur.getMessage(), erreur);
        }
    }

    /**
     * Crée un lecteur XML sécurisé (pas de DOCTYPE, pas d'entité externe),
     * partagé avec {@link ProgrammeLivraisons}.
     *
     * @return le lecteur
     */
    static DocumentBuilder creerLecteurXml() {
        try {
            DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
            fabrique.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrique.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder lecteur = fabrique.newDocumentBuilder();
            lecteur.setErrorHandler(new DefaultHandler()); // pas de trace
            return lecteur;
        } catch (ParserConfigurationException erreur) {
            throw new IllegalStateException(erreur);
        }
    }

    private Noeud trouverNoeud(String identifiant) throws SAXException {
        Noeud noeud = noeuds.get(Long.parseLong(identifiant));
        if (noeud == null) {
            throw new SAXException("Tronçon vers un nœud inconnu : "
                    + identifiant);
        }
        return noeud;
    }

    /**
     * Renvoie nœuds du plan (non modifiable).
     *
     * @return nœuds du plan (non modifiable)
     */
    public Collection<Noeud> obtenirNoeuds() {
        return Collections.unmodifiableCollection(noeuds.values());
    }

    /**
     * Renvoie un nœud à partir de son identifiant.
     *
     * @param identifiant identifiant du nœud
     * @return le nœud, ou {@code null} s'il n'existe pas
     */
    public Noeud obtenirNoeud(long identifiant) {
        return noeuds.get(identifiant);
    }

    /**
     * Renvoie tronçons du plan (non modifiable).
     *
     * @return tronçons du plan (non modifiable)
     */
    public List<Troncon> obtenirTroncons() {
        return Collections.unmodifiableList(troncons);
    }

    // --- Calcul des tournées ---

    /**
     * Calcule la meilleure tournée de chaque livreur : voyageur de commerce
     * sur les seules demandes du livreur ; un livreur sans demande a une
     * tournée vide. Chaque tournée part de l'entrepôt à l'heure de départ,
     * récupère chaque colis avant de le livrer et revient à l'entrepôt le
     * plus tôt possible, à {@value #VITESSE_KMH} km/h. La limite de temps du
     * voyageur de commerce est partagée à parts égales entre les livreurs qui
     * ont des demandes.
     *
     * <p>
     * Une tournée qui revient à l'entrepôt après
     * {@link #HEURE_LIMITE_RETOUR} est renvoyée quand même : elle est
     * signalée par {@link Tournee#estPossible()}.
     *
     * @param programme entrepôt, heure de départ, livreurs et demandes
     * @return une tournée par numéro de livreur, triées (non modifiable)
     * @throws IllegalStateException si une adresse est inaccessible ou si
     *                               aucun ordre de passage n'a été trouvé dans la
     *                               limite de temps
     */
    public Map<Integer, Tournee> calculerTournees(
            ProgrammeLivraisons programme) {
        int nombreLivreurs = programme.obtenirNombreLivreurs();
        List<List<Demande>> demandesParLivreur = new ArrayList<>();
        for (int livreur = 1; livreur <= nombreLivreurs; livreur++) {
            demandesParLivreur.add(new ArrayList<>());
        }
        for (Demande demande : programme.obtenirDemandes()) {
            demandesParLivreur.get(demande.livreur() - 1).add(demande);
        }
        long livreursOccupes = demandesParLivreur.stream()
                .filter(demandes -> !demandes.isEmpty()).count();
        int limiteTemps = (int) (LIMITE_TEMPS_TSP_MS
                / Math.max(1, livreursOccupes));
        SortedMap<Integer, Tournee> tournees = new TreeMap<>();
        for (int livreur = 1; livreur <= nombreLivreurs; livreur++) {
            tournees.put(livreur, calculerTournee(programme.obtenirEntrepot(),
                    programme.obtenirHeureDepart(),
                    demandesParLivreur.get(livreur - 1), limiteTemps));
        }
        return Collections.unmodifiableSortedMap(tournees);
    }

    /**
     * Meilleure tournée d'un livreur : plus courts chemins entre ses
     * adresses, ordre de passage, puis heures de passage. Sommets : 0 est
     * l'entrepôt, puis pour la demande k, 2k+1 est sa collecte et 2k+2 sa
     * livraison.
     */
    private Tournee calculerTournee(Noeud entrepot, LocalTime heureDepart,
            List<Demande> demandes, int limiteTemps) {
        List<Sommet> sommets = new ArrayList<>();
        sommets.add(new Sommet(entrepot, 0, TypeArret.ENTREPOT, null));
        for (Demande demande : demandes) {
            sommets.add(new Sommet(demande.collecte(), demande.dureeCollecte(),
                    TypeArret.COLLECTE, demande));
            sommets.add(new Sommet(demande.livraison(),
                    demande.dureeLivraison(), TypeArret.LIVRAISON, demande));
        }
        Chemin[][] chemins = calculerPlusCourtsChemins(sommets);
        List<Integer> ordre = ordresMemorises.get(sommets);
        boolean dejaMemorise = ordre != null;
        OrdreDePassage trouve = null;
        if (!dejaMemorise) {
            trouve = calculerOrdreDePassage(chemins, limiteTemps);
            ordre = List.copyOf(trouve.sommets());
        }
        Tournee tournee = calculerHeuresDePassage(heureDepart, sommets,
                chemins, ordre);
        if (!dejaMemorise && trouve.optimal()) {
            // Gardé seulement une fois la tournée obtenue : le voyageur de
            // commerce renvoie aussi un ordre par une adresse inaccessible.
            ordresMemorises.put(List.copyOf(sommets), ordre);
        }
        return tournee;
    }

    /**
     * Nombre d'ordres de passage gardés en mémoire. Visible dans le package
     * pour les tests.
     *
     * @return le nombre d'ordres mémorisés
     */
    int nombreOrdresMemorises() {
        return ordresMemorises.size();
    }

    /**
     * Recalcule les trajets et les heures pour un ordre imposé, sans TSP.
     *
     * @param heureDepart heure de départ
     * @param adresses    arrêts ordonnés, entrepôt au début et à la fin
     * @param durees      durées sur place en secondes
     * @param types       ce que le livreur fait à chaque arrêt
     * @param demandes    demande servie à chaque arrêt, {@code null} à l'entrepôt
     * @return tournée dans l'ordre fourni
     * @throws IllegalArgumentException si l'ordre est invalide
     * @throws IllegalStateException    si la tournée est impossible
     */
    public Tournee recalculerTournee(LocalTime heureDepart,
            List<Noeud> adresses, List<Integer> durees,
            List<TypeArret> types, List<Demande> demandes) {
        int taille = adresses.size();
        if (taille == 0 || taille != durees.size() || taille != types.size()
                || taille != demandes.size()
                || !adresses.get(0).equals(adresses.get(taille - 1))
                || durees.stream().anyMatch(duree -> duree < 0)
                || durees.get(0) != 0 || durees.get(taille - 1) != 0) {
            throw new IllegalArgumentException("Ordre des arrêts invalide.");
        }
        int nombre = Math.max(1, taille - 1);
        List<Sommet> sommets = new ArrayList<>();
        for (int rang = 0; rang < nombre; rang++) {
            sommets.add(new Sommet(adresses.get(rang), durees.get(rang),
                    types.get(rang), demandes.get(rang)));
        }
        Chemin[][] chemins = new Chemin[nombre][nombre];
        List<Integer> ordre = new ArrayList<>();
        for (int rang = 0; rang < nombre; rang++) {
            ordre.add(rang);
            int suivant = (rang + 1) % nombre;
            chemins[rang][suivant] = plusCourtChemin(adresses.get(rang),
                    adresses.get(suivant));
        }
        return calculerHeuresDePassage(heureDepart, sommets, chemins, ordre);
    }

    /**
     * Insère collecte puis livraison à la position choisie par l'utilisateur.
     * Seuls les trois nouveaux chemins entre les arrêts voisins sont calculés.
     * Les autres chemins et les plages annoncées restent inchangés.
     *
     * @param tournee  tournée de départ, jamais modifiée
     * @param demande  demande à insérer
     * @param position rang de collecte, entre 1 et le rang du retour inclus
     * @return nouvelle tournée sans recherche de position ni appel au TSP
     * @throws IllegalArgumentException si la position est invalide
     * @throws IllegalStateException    si un nouveau chemin est inaccessible ou
     *                                  si l'insertion rend le retour impossible
     *                                  (après 13 h, alors que
     *                                  la tournée d'avant rentrait à l'heure)
     */
    public Tournee insererDemande(Tournee tournee, Demande demande,
            int position) {
        List<Tournee.Arret> anciens = new ArrayList<>(tournee.arrets());
        boolean tourneeVide = anciens.size() == 1;
        if (tourneeVide) {
            anciens.add(anciens.get(0));
        }
        if (position < 1 || position >= anciens.size()) {
            throw new IllegalArgumentException(
                    "Position d'insertion invalide.");
        }
        // Le retour à l'entrepôt (dernier arrêt) n'est pas un sommet : l'étape
        // des heures de passage l'ajoute elle-même.
        List<Sommet> sommets = new ArrayList<>();
        for (Tournee.Arret arret : anciens.subList(0, anciens.size() - 1)) {
            sommets.add(new Sommet(arret.noeud(), dureeSurPlace(arret),
                    arret.type(), arret.demande()));
        }
        sommets.add(position, new Sommet(demande.collecte(),
                demande.dureeCollecte(), TypeArret.COLLECTE, demande));
        sommets.add(position + 1, new Sommet(demande.livraison(),
                demande.dureeLivraison(), TypeArret.LIVRAISON, demande));
        List<Chemin> trajets = new ArrayList<>(tournee.chemins());
        if (trajets.isEmpty()) {
            trajets.add(new Chemin(0, List.of()));
        }
        trajets.remove(position - 1);
        trajets.addAll(position - 1, List.of(
                plusCourtChemin(anciens.get(position - 1).noeud(),
                        demande.collecte()),
                plusCourtChemin(demande.collecte(), demande.livraison()),
                plusCourtChemin(demande.livraison(),
                        anciens.get(position).noeud())));
        int nombre = sommets.size();
        Chemin[][] chemins = new Chemin[nombre][nombre];
        List<Integer> ordre = new ArrayList<>();
        for (int rang = 0; rang < nombre; rang++) {
            ordre.add(rang);
            chemins[rang][(rang + 1) % nombre] = trajets.get(rang);
        }
        Tournee candidate = calculerHeuresDePassage(anciens.get(0).depart(),
                sommets, chemins, ordre);
        List<Tournee.Arret> arrets = new ArrayList<>(candidate.arrets());
        for (int rang = 0; rang < anciens.size(); rang++) {
            if (tourneeVide && rang == 1) {
                continue; // retour ajouté plus haut : il garde sa plage
            }
            int nouveauRang = rang < position ? rang : rang + 2;
            Tournee.Arret nouveau = arrets.get(nouveauRang);
            Tournee.Arret ancien = anciens.get(rang);
            arrets.set(nouveauRang, new Tournee.Arret(nouveau.noeud(),
                    nouveau.arrivee(), nouveau.depart(), nouveau.type(),
                    nouveau.demande(), ancien.debutPlage(),
                    ancien.finPlage()));
        }
        Tournee apres = new Tournee(arrets, candidate.chemins());
        if (!apres.estPossible() && tournee.estPossible()) {
            throw new IllegalStateException("Insertion refusée : le retour à "
                    + "l'entrepôt serait à " + apres.obtenirHeureRetour()
                    + ", après " + HEURE_LIMITE_RETOUR + ".");
        }
        return apres;
    }

    /**
     * Cherche la position où insérer une demande pour que le livreur rentre à
     * l'entrepôt le plus tôt : la collecte puis la livraison sont placées l'une
     * après l'autre, comme le fait {@link #insererDemande}. C'est une aide à la
     * décision : rien n'est inséré, et l'administrateur reste libre de choisir
     * une autre position.
     *
     * <p>
     * Les durées sur place sont les mêmes à toute position : seul compte le
     * temps de trajet ajouté, c'est-à-dire les deux trajets qui entourent la
     * demande moins le trajet direct qu'ils remplacent (repris de la tournée).
     * Le voyageur de commerce n'est pas lancé. En cas d'égalité, la première
     * position est gardée. La règle des 13 h n'est pas vérifiée ici, mais par
     * {@link #insererDemande}.
     *
     * @param tournee tournée actuelle d'un livreur, jamais modifiée
     * @param demande demande à insérer
     * @return rang de collecte, entre 1 et le rang du retour inclus (valide
     *         pour {@link #insererDemande})
     * @throws IllegalStateException si aucune position n'est accessible
     */
    public int trouverPositionMoindreCout(Tournee tournee, Demande demande) {
        List<Tournee.Arret> arrets = tournee.arrets();
        if (!plusCourtChemin(demande.collecte(), demande.livraison())
                .estAccessible()) {
            throw new IllegalStateException("La livraison (nœud "
                    + demande.livraison().identifiant() + ") est inaccessible "
                    + "depuis la collecte (nœud "
                    + demande.collecte().identifiant() + ").");
        }
        int meilleurePosition = -1;
        long meilleurAjout = Long.MAX_VALUE;
        for (int position = 1; position <= Math.max(1, arrets.size() - 1); position++) {
            Noeud precedent = arrets.get(position - 1).noeud();
            Noeud suivant = arrets.get(Math.min(position,
                    arrets.size() - 1)).noeud();
            Chemin allee = plusCourtChemin(precedent, demande.collecte());
            Chemin retour = plusCourtChemin(demande.livraison(), suivant);
            if (!allee.estAccessible() || !retour.estAccessible()) {
                continue;
            }
            // Trajet direct remplacé : celui de la tournée, nul si elle est
            // vide.
            long direct = arrets.size() == 1 ? 0
                    : secondesDeTrajet(tournee.chemins().get(position - 1)
                            .longueur());
            long ajout = secondesDeTrajet(allee.longueur())
                    + secondesDeTrajet(retour.longueur()) - direct;
            if (ajout < meilleurAjout) {
                meilleurAjout = ajout;
                meilleurePosition = position;
            }
        }
        if (meilleurePosition < 0) {
            throw new IllegalStateException("Aucune position possible : la "
                    + "collecte (nœud " + demande.collecte().identifiant()
                    + ") ou la livraison (nœud "
                    + demande.livraison().identifiant() + ") est "
                    + "inaccessible depuis cette tournée.");
        }
        return meilleurePosition;
    }

    /**
     * Graphe des plus courts chemins : le plus court chemin entre chaque
     * couple d'adresses.
     */
    private Chemin[][] calculerPlusCourtsChemins(List<Sommet> sommets) {
        int nombre = sommets.size();
        Chemin[][] chemins = new Chemin[nombre][nombre];
        for (int i = 0; i < nombre; i++) {
            for (int j = 0; j < nombre; j++) {
                chemins[i][j] = i == j ? new Chemin(0, List.of())
                        : plusCourtChemin(sommets.get(i).adresse(),
                                sommets.get(j).adresse());
            }
        }
        return chemins;
    }

    /**
     * Meilleur ordre de passage (voyageur de commerce avec contrainte
     * d'ordre), cherché pendant au plus {@code limiteTemps} millisecondes.
     *
     * @return les sommets dans l'ordre de passage, en commençant par 0, sans
     *         le retour à l'entrepôt, et si cet ordre est prouvé optimal
     */
    private static OrdreDePassage calculerOrdreDePassage(Chemin[][] chemins,
            int limiteTemps) {
        int nombre = chemins.length;
        if (nombre == 1) {
            return new OrdreDePassage(List.of(0), true);
        }
        // Coût d'un arc inaccessible : assez grand pour ne jamais être choisi,
        // assez petit pour que la somme des coûts ne déborde pas.
        int coutInaccessible = Integer.MAX_VALUE / (nombre + 1);
        int[][] couts = new int[nombre][nombre];
        for (int i = 0; i < nombre; i++) {
            for (int j = 0; j < nombre; j++) {
                couts[i][j] = chemins[i][j].estAccessible()
                        ? (int) Math.round(chemins[i][j].longueur())
                        : coutInaccessible;
            }
        }
        HashMap<Integer, Integer> collecteAvantLivraison = new HashMap<>();
        for (int livraison = 2; livraison < nombre; livraison += 2) {
            collecteAvantLivraison.put(livraison, livraison - 1);
        }
        TSP tsp = new TSP5();
        ResultatCalculTSP res = tsp.chercherSolution(limiteTemps,
                new GrapheDepandance(nombre, couts, collecteAvantLivraison));
        switch (res) {
            case PAS_DE_SOLUTION:
                throw new IllegalStateException(
                        "Au moins un sommet du graphe est inaccessible.");
            case CALCUL_TROP_LONG_SANS_RESULTAT:
                throw new IllegalStateException(
                        "Aucune tournée possible trouvée dans la limite de "
                                + limiteTemps + " ms.");
            default:
                // CALCUL_TROP_LONG : la meilleure solution trouvée avant la
                // limite de temps est gardée, même si elle n'est pas prouvée
                // optimale.
                break;
        }
        List<Integer> ordre = new ArrayList<>();
        for (int rang = 0; rang < nombre; rang++) {
            Integer sommet = tsp.obtenirSommetSolution(rang);
            if (sommet == null) {
                throw new IllegalStateException(
                        "Aucune tournée possible trouvée dans la limite de "
                                + limiteTemps + " ms.");
            }
            ordre.add(sommet);
        }
        return new OrdreDePassage(ordre, res == ResultatCalculTSP.RESULTAT_OK);
    }

    /**
     * Heures de passage : l'arrivée sur un arrêt est le départ de l'arrêt
     * précédent plus le temps de trajet (arrondi à la seconde), le départ est
     * l'arrivée plus la durée sur place.
     */
    private static Tournee calculerHeuresDePassage(LocalTime heureDepart,
            List<Sommet> sommets, Chemin[][] chemins, List<Integer> ordre) {
        List<Integer> parcours = new ArrayList<>(ordre);
        if (ordre.size() > 1) {
            parcours.add(0); // retour à l'entrepôt
        }
        List<Tournee.Arret> arrets = new ArrayList<>();
        List<Chemin> trajets = new ArrayList<>();
        LocalTime depart = heureDepart;
        long ecoule = 0;
        for (int rang = 0; rang < parcours.size(); rang++) {
            Sommet sommet = sommets.get(parcours.get(rang));
            long trajet = 0;
            if (rang > 0) {
                Chemin chemin = chemins[parcours.get(rang - 1)][parcours.get(rang)];
                if (!chemin.estAccessible()) {
                    throw new IllegalStateException("Adresse "
                            + sommet.adresse().identifiant()
                            + " inaccessible depuis l'adresse "
                            + sommets.get(parcours.get(rang - 1)).adresse()
                                    .identifiant()
                            + ".");
                }
                trajets.add(chemin);
                trajet = secondesDeTrajet(chemin.longueur());
            }
            ecoule += trajet + sommet.duree();
            if (heureDepart.toSecondOfDay() + ecoule >= SECONDES_PAR_JOUR) {
                // Les heures sont des LocalTime : au-delà de minuit elles
                // repartiraient de 0 et la tournée paraîtrait à l'heure.
                throw new IllegalStateException("La tournée dépasse minuit.");
            }
            LocalTime arrivee = depart.plusSeconds(trajet);
            depart = arrivee.plusSeconds(sommet.duree());
            arrets.add(new Tournee.Arret(sommet.adresse(), arrivee, depart,
                    sommet.type(), sommet.demande()));
        }
        return new Tournee(arrets, trajets);
    }

    private static long secondesDeTrajet(double metres) {
        double metresParSeconde = VITESSE_KMH * 1000 / 3600;
        return Math.round(metres / metresParSeconde);
    }

    // --- Recalcul local d'une tournée (sans relancer le voyageur de commerce)

    /**
     * Retire une demande d'une tournée déjà calculée en <b>gardant l'ordre de
     * passage de tous les autres arrêts</b> : les deux arrêts de la demande
     * (collecte et livraison) disparaissent, puis les heures de passage sont
     * recalculées. Le voyageur de commerce n'est pas relancé, et A* ne tourne
     * que sur les jonctions créées par la suppression (une par arrêt retiré, au
     * plus deux) ; les autres trajets sont repris de la tournée actuelle. La
     * réponse est donc immédiate, même sur le grand plan.
     *
     * <p>
     * Chaque arrêt de la tournée connaît sa demande : les deux arrêts à
     * retirer sont ceux de cette demande. Si le programme contient deux
     * demandes identiques, on retire la première collecte et la première
     * livraison : celles qui restent forment une demande identique, et sa
     * collecte précède toujours sa livraison.
     *
     * <p>
     * Mêmes règles que {@link #calculerTournees} : {@value #VITESSE_KMH}
     * km/h, départ à l'heure de départ de la tournée actuelle. Retirer des
     * arrêts ne rallonge jamais une tournée : si elle était possible, elle le
     * reste.
     *
     * @param tournee tournée actuelle d'un livreur, avec la demande à retirer
     * @param demande demande à retirer
     * @return la nouvelle tournée, sans les deux arrêts de la demande
     * @throws IllegalArgumentException si la collecte ou la livraison de la
     *                                  demande est introuvable dans la tournée
     * @throws IllegalStateException    si une jonction est inaccessible
     */
    public Tournee calculerTourneeSansDemande(Tournee tournee,
            Demande demande) {
        List<Tournee.Arret> arrets = tournee.arrets();
        int rangCollecte = trouverRangArret(arrets, demande,
                TypeArret.COLLECTE);
        int rangLivraison = trouverRangArret(arrets, demande,
                TypeArret.LIVRAISON);
        // Rangs, dans la tournée actuelle, des arrêts qui restent : tous sauf
        // les deux de la demande (le départ et le retour restent toujours).
        List<Integer> rangsGardes = new ArrayList<>();
        for (int rang = 0; rang < arrets.size(); rang++) {
            if (rang != rangCollecte && rang != rangLivraison) {
                rangsGardes.add(rang);
            }
        }
        return recalculerDansLOrdre(tournee, rangsGardes);
    }

    /**
     * Tournée qui visite des arrêts de la tournée actuelle dans un ordre donné
     * (arrêts retirés, ou arrêts échangés). Un trajet entre deux arrêts qui se
     * suivaient déjà dans la tournée actuelle, dans le même sens, est repris
     * tel quel ; seuls les trajets des jonctions créées par le changement sont
     * calculés par A*.
     *
     * @param tournee     tournée actuelle
     * @param rangsGardes rangs dans la tournée actuelle des arrêts de la
     *                    nouvelle tournée, dans leur nouvel ordre : le premier est
     *                    le
     *                    départ de l'entrepôt, le dernier le retour à l'entrepôt
     */
    private Tournee recalculerDansLOrdre(Tournee tournee,
            List<Integer> rangsGardes) {
        return recalculerDansLOrdre(tournee, rangsGardes, -1);
    }

    /**
     * Comme {@link #recalculerDansLOrdre(Tournee, List)}, mais les trajets qui
     * arrivent à l'arrêt de rang {@code rangDeplace} ou en partent sont
     * toujours recalculés par A* : cet arrêt a changé d'adresse.
     *
     * @param rangDeplace rang de l'arrêt déplacé, ou -1 si aucun
     */
    private Tournee recalculerDansLOrdre(Tournee tournee,
            List<Integer> rangsGardes, int rangDeplace) {
        List<Tournee.Arret> arrets = tournee.arrets();
        // Le retour à l'entrepôt (dernier rang gardé) n'est pas un sommet :
        // l'étape des heures de passage l'ajoute elle-même, comme dans
        // calculerTournee. Il reste l'entrepôt (sommet 0) puis les arrêts
        // gardés (sommets 1, 2, ...) dans leur ordre.
        int nombre = rangsGardes.size() - 1;
        List<Sommet> sommets = new ArrayList<>();
        List<Integer> ordre = new ArrayList<>();
        for (int sommet = 0; sommet < nombre; sommet++) {
            Tournee.Arret arret = arrets.get(rangsGardes.get(sommet));
            sommets.add(new Sommet(arret.noeud(), dureeSurPlace(arret),
                    arret.type(), arret.demande()));
            ordre.add(sommet);
        }
        Chemin[][] chemins = new Chemin[nombre][nombre];
        // Avec le seul entrepôt, la tournée se réduit au départ : aucun trajet.
        for (int sommet = 0; sommet < nombre && nombre > 1; sommet++) {
            int rang = rangsGardes.get(sommet);
            int rangSuivant = rangsGardes.get(sommet + 1);
            // Les deux arrêts se suivaient déjà : on reprend leur trajet. Sinon
            // c'est une jonction créée par la suppression : A*.
            boolean trajetInchange = rangSuivant == rang + 1
                    && rang != rangDeplace && rangSuivant != rangDeplace;
            Chemin chemin = trajetInchange
                    ? tournee.chemins().get(rang)
                    : plusCourtChemin(arrets.get(rang).noeud(),
                            arrets.get(rangSuivant).noeud());
            chemins[sommet][(sommet + 1) % nombre] = chemin;
        }
        // Le premier arrêt est l'entrepôt : il est « atteint » à l'heure de
        // départ de la tournée.
        return calculerHeuresDePassage(arrets.get(0).arrivee(), sommets,
                chemins, ordre);
    }

    /**
     * Change l'ordre de passage d'une tournée déjà calculée en <b>échangeant un
     * arrêt avec son voisin</b> (« Monter » ou « Descendre ») : tous les autres
     * arrêts gardent leur ordre. Le voyageur de commerce n'est pas relancé, et
     * A* ne tourne que sur les trois trajets autour des deux arrêts échangés ;
     * les autres trajets sont repris de la tournée actuelle, puis les heures
     * sont recalculées.
     *
     * <p>
     * La contrainte d'ordre est vérifiée <b>avant</b> le recalcul. Échanger
     * deux arrêts voisins ne change que leur ordre mutuel : l'échange n'est
     * donc interdit que si ces deux arrêts sont le point de collecte (en
     * premier) et le point de livraison (en second) d'une même demande, que
     * chaque arrêt connaît. Avec deux demandes identiques, l'échange est aussi
     * refusé : c'est prudent, sans ambiguïté sur les arrêts.
     *
     * <p>
     * Comme le calcul complet, la méthode renvoie la tournée même si elle
     * finit après {@link #HEURE_LIMITE_RETOUR} : c'est à l'appelant de la
     * refuser avec {@link Tournee#estPossible()}.
     *
     * @param tournee  tournée actuelle d'un livreur
     * @param rang     rang de l'arrêt à déplacer dans la tournée (0 est le départ
     *                 de l'entrepôt, le dernier rang le retour à l'entrepôt)
     * @param decalage {@code -1} pour monter l'arrêt (le faire passer plus
     *                 tôt), {@code 1} pour le descendre (plus tard)
     * @return la nouvelle tournée, avec ses heures de passage
     * @throws IllegalArgumentException si l'arrêt est l'entrepôt, s'il n'a pas
     *                                  de voisin dans ce sens, ou si l'échange
     *                                  ferait passer un point de
     *                                  livraison avant son point de collecte
     * @throws IllegalStateException    si une jonction est inaccessible
     */
    public Tournee calculerTourneeOrdreChange(Tournee tournee, int rang,
            int decalage) {
        List<Tournee.Arret> arrets = tournee.arrets();
        int dernier = arrets.size() - 1; // retour à l'entrepôt
        if (rang < 1 || rang >= dernier) {
            throw new IllegalArgumentException("Le départ et le retour à "
                    + "l'entrepôt ne peuvent pas être déplacés : choisissez "
                    + "un arrêt.");
        }
        int voisin = rang + decalage;
        if (voisin < 1) {
            throw new IllegalArgumentException("Cet arrêt est déjà le premier "
                    + "de la tournée.");
        }
        if (voisin >= dernier) {
            throw new IllegalArgumentException("Cet arrêt est déjà le dernier "
                    + "de la tournée.");
        }
        // Les deux arrêts échangés, dans leur ordre actuel.
        Tournee.Arret premier = arrets.get(Math.min(rang, voisin));
        Tournee.Arret second = arrets.get(Math.max(rang, voisin));
        if (premier.type() == TypeArret.COLLECTE
                && second.type() == TypeArret.LIVRAISON
                && premier.demande().equals(second.demande())) {
            Demande demande = premier.demande();
            throw new IllegalArgumentException("Contrainte d'ordre non "
                    + "respectée : le point de collecte (nœud "
                    + demande.collecte().identifiant() + ") doit être "
                    + "visité avant son point de livraison (nœud "
                    + demande.livraison().identifiant() + ").");
        }
        // Nouvel ordre : les rangs de la tournée actuelle, avec les deux rangs
        // échangés. Seuls les trois trajets autour des deux arrêts échangés
        // sont nouveaux ; tous les autres sont repris de la tournée actuelle.
        List<Integer> rangs = new ArrayList<>();
        for (int i = 0; i < arrets.size(); i++) {
            rangs.add(i);
        }
        Collections.swap(rangs, rang, voisin);
        return recalculerDansLOrdre(tournee, rangs);
    }

    /**
     * Déplace un point de collecte ou de livraison d'une tournée déjà
     * calculée : l'arrêt change d'adresse, <b>l'ordre de passage ne change
     * pas</b> et le voyageur de commerce n'est pas relancé. A* ne tourne que
     * sur les deux trajets autour de l'arrêt déplacé, les autres sont repris
     * de la tournée actuelle, puis les heures de passage sont refaites. Les
     * deux arrêts de la demande reçoivent la demande déplacée.
     *
     * <p>
     * Comme le calcul complet, la méthode renvoie la tournée même si elle
     * finit après {@link #HEURE_LIMITE_RETOUR} : c'est à l'appelant de la
     * refuser avec {@link Tournee#estPossible()}.
     *
     * @param tournee      tournée actuelle d'un livreur
     * @param rang         rang, dans cette tournée, de l'arrêt à déplacer (0 est le
     *                     départ de l'entrepôt, le dernier rang le retour à
     *                     l'entrepôt)
     * @param nouveauNoeud nouvelle adresse du point
     * @return la nouvelle tournée, avec ses heures de passage
     * @throws IllegalArgumentException si l'arrêt est l'entrepôt, ou si la
     *                                  demande n'est pas identifiable (deux
     *                                  demandes identiques)
     * @throws IllegalStateException    si un trajet est inaccessible
     */
    public Tournee calculerTourneeAvecPointDeplace(Tournee tournee, int rang,
            Noeud nouveauNoeud) {
        List<Tournee.Arret> arrets = tournee.arrets();
        if (rang < 1 || rang >= arrets.size() - 1) {
            throw new IllegalArgumentException("Le départ et le retour à "
                    + "l'entrepôt ne peuvent pas être déplacés : choisissez "
                    + "un point de collecte ou de livraison.");
        }
        Tournee.Arret arret = arrets.get(rang);
        Demande ancienne = arret.demande();
        long arretsDeLaDemande = arrets.stream()
                .filter(autre -> ancienne.equals(autre.demande())).count();
        if (arretsDeLaDemande != 2) {
            throw new IllegalArgumentException("Ce point ne peut pas être "
                    + "identifié : plusieurs demandes ont les mêmes adresses "
                    + "et les mêmes durées.");
        }
        Demande deplacee = arret.type() == TypeArret.COLLECTE
                ? new Demande(nouveauNoeud, ancienne.livraison(),
                        ancienne.dureeCollecte(), ancienne.dureeLivraison(),
                        ancienne.livreur())
                : new Demande(ancienne.collecte(), nouveauNoeud,
                        ancienne.dureeCollecte(), ancienne.dureeLivraison(),
                        ancienne.livreur());
        // Même tournée et mêmes heures (seules les durées sur place en sont
        // lues), mais l'arrêt a son nouveau nœud et les deux arrêts de la
        // demande portent la demande déplacée.
        List<Tournee.Arret> arretsDeplaces = new ArrayList<>(arrets);
        List<Integer> rangs = new ArrayList<>();
        for (int i = 0; i < arrets.size(); i++) {
            rangs.add(i);
            Tournee.Arret courant = arrets.get(i);
            if (ancienne.equals(courant.demande())) {
                arretsDeplaces.set(i, new Tournee.Arret(
                        i == rang ? nouveauNoeud : courant.noeud(),
                        courant.arrivee(), courant.depart(), courant.type(),
                        deplacee));
            }
        }
        return recalculerDansLOrdre(
                new Tournee(arretsDeplaces, tournee.chemins()), rangs, rang);
    }

    /**
     * Rang du premier arrêt de ce type qui sert cette demande.
     *
     * @throws IllegalArgumentException si la tournée n'a pas cet arrêt
     */
    private static int trouverRangArret(List<Tournee.Arret> arrets,
            Demande demande, TypeArret type) {
        for (int rang = 0; rang < arrets.size(); rang++) {
            Tournee.Arret arret = arrets.get(rang);
            if (arret.type() == type && demande.equals(arret.demande())) {
                return rang;
            }
        }
        throw new IllegalArgumentException("Arrêt de " + type
                + " introuvable dans la tournée pour la demande " + demande
                + ".");
    }

    /** Durée sur place d'un arrêt, en secondes : départ moins arrivée. */
    private static int dureeSurPlace(Tournee.Arret arret) {
        return (int) arret.arrivee().until(arret.depart(), ChronoUnit.SECONDS);
    }

    /**
     * Plus court chemin d'un nœud à un autre en suivant le sens des tronçons,
     * avec l'algorithme A* (Dijkstra guidé par la distance à vol d'oiseau).
     * Visible dans le package pour les tests.
     *
     * @param source nœud de départ
     * @param cible  nœud d'arrivée
     * @return le plus court chemin, ou {@link Chemin#INACCESSIBLE}
     */
    Chemin plusCourtChemin(Noeud source, Noeud cible) {
        Map<Noeud, Double> distances = new HashMap<>();
        Map<Noeud, Troncon> predecesseurs = new HashMap<>();
        PriorityQueue<Etiquette> file = new PriorityQueue<>(
                Comparator.comparingDouble(Etiquette::estimation));
        distances.put(source, 0.0);
        file.add(new Etiquette(source, 0, heuristique(source, cible)));
        while (!file.isEmpty()) {
            Etiquette courante = file.poll();
            Noeud noeud = courante.noeud();
            if (noeud.equals(cible)) {
                return reconstruireChemin(predecesseurs, source, cible,
                        courante.distance());
            }
            if (courante.distance() > distances.get(noeud)) {
                continue; // étiquette périmée
            }
            for (Troncon troncon : tronconsSortants.getOrDefault(noeud,
                    List.of())) {
                Noeud voisin = troncon.destination();
                double distance = courante.distance() + troncon.longueur();
                if (distance < distances.getOrDefault(voisin,
                        Double.POSITIVE_INFINITY)) {
                    distances.put(voisin, distance);
                    predecesseurs.put(voisin, troncon);
                    file.add(new Etiquette(voisin, distance,
                            distance + heuristique(voisin, cible)));
                }
            }
        }
        return Chemin.INACCESSIBLE;
    }

    private static Chemin reconstruireChemin(Map<Noeud, Troncon> predecesseurs,
            Noeud source, Noeud cible, double longueur) {
        List<Troncon> parcours = new ArrayList<>();
        for (Noeud noeud = cible; !noeud.equals(source);) {
            Troncon troncon = predecesseurs.get(noeud);
            parcours.add(troncon);
            noeud = troncon.origine();
        }
        Collections.reverse(parcours);
        return new Chemin(longueur, parcours);
    }

    /**
     * Borne inférieure de la longueur restante d'un nœud à la cible :
     * distance à vol d'oiseau (haversine) multipliée par
     * {@link #COEFFICIENT_HEURISTIQUE}. Visible dans le package pour les
     * tests.
     *
     * @param noeud nœud courant
     * @param cible nœud d'arrivée
     * @return une estimation qui ne surestime pas, en mètres
     */
    static double heuristique(Noeud noeud, Noeud cible) {
        double latitudeA = Math.toRadians(noeud.latitude());
        double latitudeB = Math.toRadians(cible.latitude());
        double ecartLongitude = Math.toRadians(cible.longitude()
                - noeud.longitude());
        double h = Math.pow(Math.sin((latitudeB - latitudeA) / 2), 2)
                + Math.cos(latitudeA) * Math.cos(latitudeB)
                        * Math.pow(Math.sin(ecartLongitude / 2), 2);
        return COEFFICIENT_HEURISTIQUE * 2 * RAYON_TERRE
                * Math.asin(Math.sqrt(h));
    }
}
