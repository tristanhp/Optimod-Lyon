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
import fr.insa.optimod.algo.tsp.TSP3;
import fr.insa.optimod.algo.tsp.TSP5;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;

/**
 * Plan de la ville lu dans un fichier de plan : des nœuds et des tronçons.
 *
 * <p>
 * Le plan calcule aussi la tournée d'un programme de livraisons
 * ({@link #calculerTournee}) : plus courts chemins entre les adresses (A*),
 * meilleur ordre de passage avec la contrainte d'ordre (voyageur de commerce,
 * {@link TSP3}), puis heures de passage.
 *
 * <p>Le voyageur de commerce ne sert qu'au calcul complet. Quand l'utilisateur
 * modifie une tournée, l'ordre des autres arrêts est gardé, seuls les trajets
 * des jonctions créées sont recalculés et les heures sont refaites
 * ({@link #calculerTourneeSansDemande}) : c'est le recalcul local.
 */
public final class Plan {

    /** Vitesse du livreur, en km/h. */
    public static final double VITESSE_KMH = 15;

    /** Heure limite de retour à l'entrepôt. */
    public static final LocalTime HEURE_LIMITE_RETOUR = LocalTime.of(13, 0);

    /** Limite de temps du calcul de l'ordre de passage, en millisecondes. */
    private static final int LIMITE_TEMPS_TSP_MS = 10_000;

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

    private final Map<Long, Noeud> noeuds = new LinkedHashMap<>();
    private final Map<Noeud, List<Troncon>> tronconsSortants =
            new LinkedHashMap<>();
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
            for (int indice = 0; indice
                    < elementsNoeuds.getLength(); indice++) {
                Element element = (Element) elementsNoeuds.item(indice);
                Noeud noeud = new Noeud(
                        Long.parseLong(element.getAttribute("id")),
                        Double.parseDouble(element.getAttribute("longitude")),
                        Double.parseDouble(element.getAttribute("latitude")));
                noeuds.put(noeud.identifiant(), noeud);
                tronconsSortants.put(noeud, new ArrayList<>());
            }
            NodeList elementsTroncons =
                    documentXml.getElementsByTagName("troncon");
            for (int indice = 0; indice
                    < elementsTroncons.getLength(); indice++) {
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
            DocumentBuilderFactory fabrique =
                    DocumentBuilderFactory.newInstance();
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

    /** @return nœuds du plan (non modifiable) */
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

    /** @return tronçons du plan (non modifiable) */
    public List<Troncon> obtenirTroncons() {
        return Collections.unmodifiableList(troncons);
    }

    // --- Calcul de la tournée ---

    /**
     * Calcule la meilleure tournée d'un programme de livraisons : départ de
     * l'entrepôt à l'heure de départ, chaque colis récupéré avant d'être
     * livré, retour à l'entrepôt le plus tôt possible, à
     * {@value #VITESSE_KMH} km/h.
     *
     * @param programme entrepôt, heure de départ et demandes
     * @return la tournée, avec ses heures de passage
     * @throws IllegalStateException si aucune tournée possible n'existe : une
     *                               adresse est inaccessible, aucun ordre n'a
     *                               été
     *                               trouvé dans la
     *                               limite de temps, ou le retour à l'entrepôt
     *                               dépasse
     *                               {@link #HEURE_LIMITE_RETOUR}
     */
    public Tournee calculerTournee(ProgrammeLivraisons programme) {
        List<Noeud> adresses = new ArrayList<>();
        List<Integer> durees = new ArrayList<>();
        adresses.add(programme.obtenirEntrepot());
        durees.add(0);
        for (Demande demande : programme.obtenirDemandes()) {
            adresses.add(demande.collecte());
            durees.add(demande.dureeCollecte());
            adresses.add(demande.livraison());
            durees.add(demande.dureeLivraison());
        }
        Chemin[][] chemins = calculerPlusCourtsChemins(adresses);
        List<Integer> ordre = calculerOrdreDePassage(chemins);
        return calculerHeuresDePassage(programme.obtenirHeureDepart(),
                adresses, durees, chemins, ordre);
    }

    /**
     * Graphe des plus courts chemins : le plus court chemin entre chaque
     * couple d'adresses.
     */
    private Chemin[][] calculerPlusCourtsChemins(List<Noeud> adresses) {
        int nombre = adresses.size();
        Chemin[][] chemins = new Chemin[nombre][nombre];
        for (int i = 0; i < nombre; i++) {
            for (int j = 0; j < nombre; j++) {
                chemins[i][j] = i == j ? new Chemin(0, List.of())
                        : plusCourtChemin(adresses.get(i), adresses.get(j));
            }
        }
        return chemins;
    }

    /**
     * Meilleur ordre de passage (voyageur de commerce avec contrainte
     * d'ordre). Sommets : 0 est l'entrepôt, puis pour la demande k, 2k+1 est
     * sa collecte et 2k+2 sa livraison.
     *
     * @return les sommets dans l'ordre de passage, en commençant par 0, sans
     *         le retour à l'entrepôt
     */
    private static List<Integer> calculerOrdreDePassage(Chemin[][] chemins) {
        int nombre = chemins.length;
        if (nombre == 1) {
            return List.of(0);
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
        ResultatCalculTSP res = tsp.chercherSolution(LIMITE_TEMPS_TSP_MS,
                new GrapheDepandance(nombre, couts, collecteAvantLivraison));
        switch (res) {
            case PAS_DE_SOLUTION:
                throw new IllegalStateException(
                "Au moins un sommet du graphe est inaccessible.");
            case CALCUL_TROP_LONG_SANS_RESULTAT:
                throw new IllegalStateException(
                "Aucune tournée possible trouvée dans la limite de "
                        + LIMITE_TEMPS_TSP_MS / 1000 + " s.");
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
                                + LIMITE_TEMPS_TSP_MS / 1000 + " s.");
            }
            ordre.add(sommet);
        }
        return ordre;
    }

    /**
     * Heures de passage : l'arrivée sur un arrêt est le départ de l'arrêt
     * précédent plus le temps de trajet (arrondi à la seconde), le départ est
     * l'arrivée plus la durée sur place.
     */
    private static Tournee calculerHeuresDePassage(LocalTime heureDepart,
            List<Noeud> adresses, List<Integer> durees, Chemin[][] chemins,
            List<Integer> ordre) {
        List<Integer> parcours = new ArrayList<>(ordre);
        if (ordre.size() > 1) {
            parcours.add(0); // retour à l'entrepôt
        }
        List<Tournee.Arret> arrets = new ArrayList<>();
        List<Chemin> trajets = new ArrayList<>();
        LocalTime depart = heureDepart;
        long secondesDepuisDepart = 0;
        for (int rang = 0; rang < parcours.size(); rang++) {
            int sommet = parcours.get(rang);
            long trajet = 0;
            if (rang > 0) {
                Chemin chemin = chemins[parcours.get(rang - 1)][sommet];
                if (!chemin.estAccessible()) {
                    throw new IllegalStateException("Adresse "
                            + adresses.get(sommet).identifiant()
                            + " inaccessible depuis l'adresse "
                            + adresses.get(parcours.get(rang - 1)).identifiant()
                            + ".");
                }
                trajets.add(chemin);
                trajet = secondesDeTrajet(chemin.longueur());
            }
            LocalTime arrivee = depart.plusSeconds(trajet);
            int duree = rang == parcours.size() - 1 && rang > 0
                    ? 0
                    : durees.get(sommet);
            secondesDepuisDepart += trajet + duree;
            depart = arrivee.plusSeconds(duree);
            arrets.add(new Tournee.Arret(adresses.get(sommet), arrivee,
                    depart));
        }
        LocalTime retour = arrets.get(arrets.size() - 1).arrivee();
        if (secondesDepuisDepart > heureDepart.until(HEURE_LIMITE_RETOUR,
                ChronoUnit.SECONDS)) {
            throw new IllegalStateException("Tournée impossible : retour à "
                    + "l'entrepôt à " + retour + ", après "
                    + HEURE_LIMITE_RETOUR + ".");
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
     * <p>Une tournée ne garde pas la demande de chaque arrêt : les deux arrêts
     * sont retrouvés par leur nœud et leur durée sur place (la durée est
     * l'écart entre l'heure d'arrivée et l'heure de départ). Si ce couple se
     * retrouve sur plusieurs arrêts (deux demandes avec le même point de
     * collecte et la même durée), on ne sait pas lequel retirer sans risquer
     * de casser la contrainte d'ordre d'une autre demande : la méthode
     * refuse.
     *
     * <p>Mêmes règles que {@link #calculerTournee} : {@value #VITESSE_KMH}
     * km/h, départ à l'heure de départ de la tournée actuelle, retour à
     * l'entrepôt avant {@link #HEURE_LIMITE_RETOUR}.
     *
     * @param tournee tournée actuelle, avec la demande à retirer
     * @param demande demande à retirer
     * @return la nouvelle tournée, sans les deux arrêts de la demande
     * @throws IllegalArgumentException si un arrêt de la demande est
     *         introuvable dans la tournée, ou ne peut pas être identifié sans
     *         ambiguïté
     * @throws IllegalStateException si une jonction est inaccessible ou si le
     *         retour à l'entrepôt dépasse {@link #HEURE_LIMITE_RETOUR} ;
     *         retirer des arrêts ne devrait jamais allonger une tournée
     *         possible
     */
    public Tournee calculerTourneeSansDemande(Tournee tournee,
            Demande demande) {
        List<Tournee.Arret> arrets = tournee.arrets();
        int rangCollecte = trouverRangArret(arrets, demande.collecte(),
                demande.dureeCollecte());
        int rangLivraison = trouverRangArret(arrets, demande.livraison(),
                demande.dureeLivraison());
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
     * @param tournee tournée actuelle
     * @param rangsGardes rangs dans la tournée actuelle des arrêts de la
     *        nouvelle tournée, dans leur nouvel ordre : le premier est le
     *        départ de l'entrepôt, le dernier le retour à l'entrepôt
     */
    private Tournee recalculerDansLOrdre(Tournee tournee,
            List<Integer> rangsGardes) {
        List<Tournee.Arret> arrets = tournee.arrets();
        // Le retour à l'entrepôt (dernier rang gardé) n'est pas un sommet :
        // l'étape des heures de passage l'ajoute elle-même, comme dans
        // calculerTournee. Il reste l'entrepôt (sommet 0) puis les arrêts
        // gardés (sommets 1, 2, ...) dans leur ordre.
        int nombre = rangsGardes.size() - 1;
        List<Noeud> sommets = new ArrayList<>();
        List<Integer> durees = new ArrayList<>();
        List<Integer> ordre = new ArrayList<>();
        for (int sommet = 0; sommet < nombre; sommet++) {
            Tournee.Arret arret = arrets.get(rangsGardes.get(sommet));
            sommets.add(arret.noeud());
            durees.add(dureeSurPlace(arret));
            ordre.add(sommet);
        }
        Chemin[][] chemins = new Chemin[nombre][nombre];
        // Avec le seul entrepôt, la tournée se réduit au départ : aucun trajet.
        for (int sommet = 0; sommet < nombre && nombre > 1; sommet++) {
            int rang = rangsGardes.get(sommet);
            int rangSuivant = rangsGardes.get(sommet + 1);
            // Les deux arrêts se suivaient déjà : on reprend leur trajet. Sinon
            // c'est une jonction créée par la suppression : A*.
            Chemin chemin = rangSuivant == rang + 1
                    ? tournee.chemins().get(rang)
                    : plusCourtChemin(arrets.get(rang).noeud(),
                            arrets.get(rangSuivant).noeud());
            chemins[sommet][(sommet + 1) % nombre] = chemin;
        }
        // Le premier arrêt est l'entrepôt : il est « atteint » à l'heure de
        // départ de la tournée.
        return calculerHeuresDePassage(arrets.get(0).arrivee(), sommets,
                durees, chemins, ordre);
    }

    /**
     * Change l'ordre de passage d'une tournée déjà calculée en <b>échangeant un
     * arrêt avec son voisin</b> (« Monter » ou « Descendre ») : tous les autres
     * arrêts gardent leur ordre. Le voyageur de commerce n'est pas relancé, et
     * A* ne tourne que sur les trois trajets autour des deux arrêts échangés ;
     * les autres trajets sont repris de la tournée actuelle, puis les heures
     * sont recalculées.
     *
     * <p>La contrainte d'ordre est vérifiée <b>avant</b> le recalcul. Échanger
     * deux arrêts voisins ne change que leur ordre mutuel : l'échange n'est
     * donc interdit que si ces deux arrêts sont le point de collecte (en
     * premier) et le point de livraison (en second) d'une même demande. Comme
     * pour {@link #calculerTourneeSansDemande}, un arrêt est reconnu par son
     * nœud et sa durée sur place.
     *
     * @param tournee tournée actuelle
     * @param demandes demandes de la tournée
     * @param rang rang de l'arrêt à déplacer dans la tournée (0 est le départ
     *        de l'entrepôt, le dernier rang le retour à l'entrepôt)
     * @param decalage {@code -1} pour monter l'arrêt (le faire passer plus
     *        tôt), {@code 1} pour le descendre (plus tard)
     * @return la nouvelle tournée, avec ses heures de passage
     * @throws IllegalArgumentException si l'arrêt est l'entrepôt, s'il n'a pas
     *         de voisin dans ce sens, ou si l'échange ferait passer un point de
     *         livraison avant son point de collecte
     * @throws IllegalStateException si le retour à l'entrepôt dépasse
     *         {@link #HEURE_LIMITE_RETOUR}
     */
    public Tournee calculerTourneeOrdreChange(Tournee tournee,
            List<Demande> demandes, int rang, int decalage) {
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
        for (Demande demande : demandes) {
            if (estArret(premier, demande.collecte(), demande.dureeCollecte())
                    && estArret(second, demande.livraison(),
                            demande.dureeLivraison())) {
                throw new IllegalArgumentException("Contrainte d'ordre non "
                        + "respectée : le point de collecte (nœud "
                        + demande.collecte().identifiant() + ") doit être "
                        + "visité avant son point de livraison (nœud "
                        + demande.livraison().identifiant() + ").");
            }
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

    /** Vrai si cet arrêt a ce nœud et cette durée sur place. */
    private static boolean estArret(Tournee.Arret arret, Noeud noeud,
            int duree) {
        return arret.noeud().equals(noeud) && dureeSurPlace(arret) == duree;
    }

    /**
     * Rang de l'unique arrêt qui a ce nœud et cette durée sur place. Le départ
     * (rang 0) et le retour (dernier rang) sont l'entrepôt : on ne les cherche
     * pas.
     */
    private static int trouverRangArret(List<Tournee.Arret> arrets,
            Noeud noeud, int duree) {
        int trouve = -1;
        for (int rang = 1; rang < arrets.size() - 1; rang++) {
            // Même prédicat que pour la contrainte d'ordre : estArret.
            if (estArret(arrets.get(rang), noeud, duree)) {
                if (trouve != -1) {
                    throw new IllegalArgumentException("Arrêt ambigu : le nœud "
                            + noeud.identifiant() + " est visité plusieurs "
                            + "fois avec la même durée sur place.");
                }
                trouve = rang;
            }
        }
        if (trouve == -1) {
            throw new IllegalArgumentException("Arrêt introuvable dans la "
                    + "tournée : nœud " + noeud.identifiant() + ", durée sur "
                    + "place " + duree + " s.");
        }
        return trouve;
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
