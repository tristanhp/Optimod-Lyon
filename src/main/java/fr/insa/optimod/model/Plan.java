package fr.insa.optimod.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
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
import fr.insa.optimod.algo.tsp.TSP3;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee.TypeArret;

/**
 * Plan de la ville lu dans un fichier de plan : des nœuds et des tronçons.
 *
 * <p>Le plan calcule aussi les tournées d'un programme de livraisons, une par
 * livreur ({@link #calculerTournees}) : plus courts chemins entre les
 * adresses (A*), meilleur ordre de passage avec la contrainte d'ordre
 * (voyageur de commerce, {@link TSP3}), puis heures de passage.
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

    private final Map<Long, Noeud> noeuds = new LinkedHashMap<>();
    private final Map<Noeud, List<Troncon>> tronconsSortants =
            new LinkedHashMap<>();
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
        try {
            NodeList elementsNoeuds = documentXml.getElementsByTagName("noeud");
            for (int indice = 0; indice < elementsNoeuds.getLength();
                    indice++) {
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
            for (int indice = 0; indice < elementsTroncons.getLength();
                    indice++) {
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
     * <p>Une tournée qui revient à l'entrepôt après
     * {@link #HEURE_LIMITE_RETOUR} est renvoyée quand même : elle est
     * signalée par {@link Tournee#estPossible()}.
     *
     * @param programme entrepôt, heure de départ, livreurs et demandes
     * @return une tournée par numéro de livreur, triées (non modifiable)
     * @throws IllegalStateException si une adresse est inaccessible ou si
     *         aucun ordre de passage n'a été trouvé dans la limite de temps
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
        List<Integer> ordre = calculerOrdreDePassage(chemins, limiteTemps);
        return calculerHeuresDePassage(heureDepart, sommets, chemins, ordre);
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
     *         le retour à l'entrepôt
     */
    private static List<Integer> calculerOrdreDePassage(Chemin[][] chemins,
            int limiteTemps) {
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
        TSP tsp = new TSP3();
        tsp.chercherSolution(limiteTemps,
                new GrapheDepandance(nombre, couts, collecteAvantLivraison));
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
            List<Sommet> sommets, Chemin[][] chemins, List<Integer> ordre) {
        List<Integer> parcours = new ArrayList<>(ordre);
        if (ordre.size() > 1) {
            parcours.add(0); // retour à l'entrepôt
        }
        List<Tournee.Arret> arrets = new ArrayList<>();
        List<Chemin> trajets = new ArrayList<>();
        LocalTime depart = heureDepart;
        for (int rang = 0; rang < parcours.size(); rang++) {
            Sommet sommet = sommets.get(parcours.get(rang));
            long trajet = 0;
            if (rang > 0) {
                Chemin chemin = chemins[parcours.get(rang - 1)]
                        [parcours.get(rang)];
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

    /**
     * Plus court chemin d'un nœud à un autre en suivant le sens des tronçons,
     * avec l'algorithme A* (Dijkstra guidé par la distance à vol d'oiseau).
     * Visible dans le package pour les tests.
     *
     * @param source nœud de départ
     * @param cible nœud d'arrivée
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
        double ecartLongitude =
                Math.toRadians(cible.longitude() - noeud.longitude());
        double h = Math.pow(Math.sin((latitudeB - latitudeA) / 2), 2)
                + Math.cos(latitudeA) * Math.cos(latitudeB)
                * Math.pow(Math.sin(ecartLongitude / 2), 2);
        return COEFFICIENT_HEURISTIQUE * 2 * RAYON_TERRE
                * Math.asin(Math.sqrt(h));
    }
}
