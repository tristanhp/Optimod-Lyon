package fr.insa.optimod.model;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee.TypeArret;

/**
 * Sauvegarde XML version 1 pour le modèle actuel à un livreur.
 * Le plan est référencé, les demandes et l'ordre sont conservés.
 *
 * <p>
 * Le plan est désigné par son chemin relatif au dossier de la sauvegarde :
 * une sauvegarde déplacée avec son plan se restaure encore. Les anciennes
 * sauvegardes, qui désignent le plan par un chemin absolu, se restaurent
 * aussi : si ce chemin n'existe pas (autre machine, autre système), le plan
 * est cherché par son nom dans le dossier de la sauvegarde.
 *
 * @param plan        plan rechargé
 * @param fichierPlan fichier du plan
 * @param programme   programme restauré
 * @param tournee     tournée recalculée sans TSP
 */
public record SauvegardeTournee(Plan plan, Path fichierPlan,
        ProgrammeLivraisons programme, Tournee tournee) {

    /**
     * Nombre maximal d'affectations partielles des arrêts aux demandes
     * examinées à la restauration : au-delà, la sauvegarde est refusée
     * plutôt que de bloquer l'application.
     */
    private static final int ESSAIS_MAXIMUM = 200_000;

    /**
     * Écrit une sauvegarde sans recopier le plan.
     *
     * @param fichier     destination XML
     * @param fichierPlan référence du plan
     * @param programme   programme courant, dont les tournées sont calculées
     * @throws IOException si l'écriture échoue, ou si le programme a plusieurs
     *                     livreurs (le format ne gère qu'un livreur)
     */
    public static void sauvegarder(Path fichier, Path fichierPlan,
            ProgrammeLivraisons programme) throws IOException {
        Tournee tournee = programme.obtenirTournees().get(1);
        if (programme.obtenirTournees().size() != 1 || tournee == null) {
            throw new IOException("La sauvegarde ne gère qu'un seul livreur.");
        }
        try (OutputStream sortie = Files.newOutputStream(fichier)) {
            XMLStreamWriter xml = XMLOutputFactory.newFactory()
                    .createXMLStreamWriter(sortie, "UTF-8");
            try {
                xml.writeStartDocument("UTF-8", "1.0");
                xml.writeStartElement("sauvegardeTournee");
                xml.writeAttribute("version", "1");
                xml.writeAttribute("nombreLivreurs", "1");
                xml.writeAttribute("plan", referencePlan(fichier,
                        fichierPlan));
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
     * Chemin du plan relatif au dossier de la sauvegarde, avec des « / »
     * (lus sur tous les systèmes). Chemin absolu si aucun chemin relatif
     * n'existe (plan sur un autre disque).
     */
    private static String referencePlan(Path fichier, Path fichierPlan) {
        Path plan = fichierPlan.toAbsolutePath().normalize();
        Path dossier = fichier.toAbsolutePath().normalize().getParent();
        if (dossier == null) {
            return plan.toString();
        }
        try {
            return dossier.relativize(plan).toString()
                    .replace(File.separatorChar, '/');
        } catch (IllegalArgumentException autreDisque) {
            return plan.toString();
        }
    }

    /**
     * Recharge le plan, les demandes et les arrêts avant tout affichage.
     *
     * @param fichier sauvegarde XML
     * @return état restauré, indépendant de l'état courant
     * @throws SAXException si le fichier ou le plan est invalide ou illisible
     *                      (message en français)
     */
    public static SauvegardeTournee restaurer(Path fichier)
            throws SAXException {
        try {
            Element racine = Plan.lireXml(fichier.toFile())
                    .getDocumentElement();
            if (!"sauvegardeTournee".equals(racine.getTagName())) {
                throw new SAXException("Ce fichier n'est pas une sauvegarde "
                        + "de tournée.");
            }
            if (!"1".equals(racine.getAttribute("version"))
                    || !"1".equals(racine.getAttribute("nombreLivreurs"))) {
                throw new SAXException("Format de sauvegarde non pris en "
                        + "charge : seule la version 1, à un livreur, se "
                        + "restaure.");
            }
            Path fichierPlan = trouverPlan(fichier,
                    Plan.lireAttribut(racine, "plan", "sauvegarde"));
            Plan plan = new Plan(fichierPlan.toString());
            LocalTime depart = ProgrammeLivraisons.lireHeure(
                    Plan.lireAttribut(racine, "heureDepart", "sauvegarde"));
            Noeud entrepot = adresse(racine, "entrepot", "entrepôt", plan);
            List<Demande> demandes = new ArrayList<>();
            List<Element> elements = Plan.elementsFils(racine, "demande");
            if (elements.size() > ProgrammeLivraisons.MAXIMUM_DEMANDES) {
                throw new SAXException("La sauvegarde contient "
                        + elements.size() + " demandes : l'application en "
                        + "accepte au plus "
                        + ProgrammeLivraisons.MAXIMUM_DEMANDES + ".");
            }
            for (int i = 0; i < elements.size(); i++) {
                Element element = elements.get(i);
                String quoi = "demande n° " + (i + 1);
                demandes.add(new Demande(
                        adresse(element, "collecte", quoi, plan),
                        adresse(element, "livraison", quoi, plan),
                        duree(element, "dureeCollecte", quoi),
                        duree(element, "dureeLivraison", quoi)));
            }
            ProgrammeLivraisons programme = new ProgrammeLivraisons(entrepot,
                    depart, demandes);
            List<Noeud> adresses = new ArrayList<>();
            List<Integer> durees = new ArrayList<>();
            List<Element> arrets = Plan.elementsFils(racine, "arret");
            for (int i = 0; i < arrets.size(); i++) {
                Element arret = arrets.get(i);
                String quoi = "arrêt n° " + (i + 1);
                adresses.add(adresse(arret, "noeud", quoi, plan));
                durees.add(duree(arret, "duree", quoi));
            }
            if (adresses.isEmpty() || !adresses.get(0).equals(entrepot)
                    || !adresses.get(adresses.size() - 1).equals(entrepot)) {
                throw new SAXException("La tournée doit partir de l'entrepôt "
                        + "et y revenir.");
            }
            int nombreAttendu = demandes.isEmpty()
                    ? 1
                    : 2 * demandes.size() + 2;
            List<TypeArret> types = new ArrayList<>();
            List<Demande> servies = new ArrayList<>();
            types.add(TypeArret.ENTREPOT);
            servies.add(null);
            if (adresses.size() != nombreAttendu
                    || !new AffectationArrets(adresses, durees, demandes,
                            types, servies).chercher(1)) {
                throw new SAXException("Les arrêts ne respectent pas les "
                        + "demandes et leur contrainte d'ordre.");
            }
            if (adresses.size() > 1) {
                types.add(TypeArret.ENTREPOT); // retour à l'entrepôt
                servies.add(null);
            }
            Tournee tournee = plan.recalculerTournee(depart, adresses, durees,
                    types, servies);
            List<Tournee.Arret> passages = new ArrayList<>();
            for (int i = 0; i < arrets.size(); i++) {
                Element element = arrets.get(i);
                Tournee.Arret arret = tournee.arrets().get(i);
                if (element.hasAttribute("debutPlage")
                        || element.hasAttribute("finPlage")) {
                    arret = new Tournee.Arret(arret.noeud(), arret.arrivee(),
                            arret.depart(), arret.type(), arret.demande(),
                            heurePlage(element, "debutPlage", i),
                            heurePlage(element, "finPlage", i));
                }
                passages.add(arret);
            }
            return new SauvegardeTournee(plan, fichierPlan, programme,
                    new Tournee(passages, tournee.chemins(), false));
        } catch (IOException | IllegalArgumentException
                | IllegalStateException erreur) {
            throw new SAXException("Impossible de restaurer la tournée : "
                    + erreur.getMessage(), erreur);
        }
    }

    /**
     * Retrouve le fichier du plan désigné par la sauvegarde : d'abord le
     * chemin écrit (relatif au dossier de la sauvegarde, ou absolu pour les
     * anciennes sauvegardes), puis un fichier de même nom dans le dossier de
     * la sauvegarde. Le nom est pris après le dernier « / » ou « \ », pour
     * lire aussi une sauvegarde écrite sous un autre système.
     *
     * @throws SAXException si aucun des deux fichiers n'existe
     */
    private static Path trouverPlan(Path fichier, String reference)
            throws SAXException {
        Path absolu = fichier.toAbsolutePath().normalize();
        Path dossier = absolu.getParent() != null ? absolu.getParent()
                : absolu;
        String nom = reference.substring(Math.max(reference.lastIndexOf('/'),
                reference.lastIndexOf('\\')) + 1);
        List<String> candidats = List.of(reference, nom);
        for (String candidat : candidats) {
            try {
                Path chemin = dossier.resolve(candidat).normalize();
                if (!candidat.isEmpty() && Files.isRegularFile(chemin)) {
                    return chemin;
                }
            } catch (InvalidPathException cheminIllisible) {
                // Chemin d'un autre système : on essaie le candidat suivant.
            }
        }
        throw new SAXException("Plan introuvable : « " + nom + " ». Placez "
                + "le fichier du plan dans le même dossier que la sauvegarde ("
                + dossier + ").");
    }

    /**
     * Affecte chaque arrêt de la tournée sauvegardée (hors entrepôt) à la
     * collecte ou à la livraison d'une demande, en respectant la contrainte
     * d'ordre. Les demandes identiques (mêmes adresses et durées) sont
     * interchangeables : elles forment une seule classe, ce qui évite
     * d'essayer toutes leurs permutations ; les échecs déjà rencontrés sont
     * retenus, et le nombre d'essais est borné. Range dans {@code types} et
     * {@code servies} ce que fait chaque arrêt quand l'affectation est
     * trouvée.
     */
    private static final class AffectationArrets {
        private final List<Noeud> adresses;
        private final List<Integer> durees;
        private final List<TypeArret> types;
        private final List<Demande> servies;
        /** Demandes de chaque classe de demandes identiques. */
        private final List<List<Demande>> classes = new ArrayList<>();
        /** Nombre de demandes collectées, par classe. */
        private final int[] collectees;
        /** Nombre de demandes livrées, par classe. */
        private final int[] livrees;
        /** États (rang, progression) déjà connus pour mener à un échec. */
        private final Set<String> echecs = new HashSet<>();
        private int essais;

        AffectationArrets(List<Noeud> adresses, List<Integer> durees,
                List<Demande> demandes, List<TypeArret> types,
                List<Demande> servies) {
            this.adresses = adresses;
            this.durees = durees;
            this.types = types;
            this.servies = servies;
            Map<Demande, List<Demande>> parDemande = new LinkedHashMap<>();
            for (Demande demande : demandes) {
                parDemande.computeIfAbsent(demande, cle -> new ArrayList<>())
                        .add(demande);
            }
            classes.addAll(parDemande.values());
            collectees = new int[classes.size()];
            livrees = new int[classes.size()];
        }

        boolean chercher(int rang) throws SAXException {
            if (rang >= adresses.size() - 1) {
                for (int k = 0; k < classes.size(); k++) {
                    if (livrees[k] != classes.get(k).size()) {
                        return false;
                    }
                }
                return true;
            }
            String etat = rang + Arrays.toString(collectees)
                    + Arrays.toString(livrees);
            if (echecs.contains(etat)) {
                return false;
            }
            if (++essais > ESSAIS_MAXIMUM) {
                throw new SAXException("Sauvegarde trop complexe à vérifier : "
                        + "les arrêts ne correspondent pas aux demandes.");
            }
            Noeud adresse = adresses.get(rang);
            int duree = durees.get(rang);
            for (int k = 0; k < classes.size(); k++) {
                List<Demande> classe = classes.get(k);
                Demande modele = classe.get(0);
                if (collectees[k] < classe.size()
                        && modele.collecte().equals(adresse)
                        && modele.dureeCollecte() == duree
                        && essayer(rang, TypeArret.COLLECTE,
                                classe.get(collectees[k]), collectees, k)) {
                    return true;
                }
                if (livrees[k] < collectees[k]
                        && modele.livraison().equals(adresse)
                        && modele.dureeLivraison() == duree
                        && essayer(rang, TypeArret.LIVRAISON,
                                classe.get(livrees[k]), livrees, k)) {
                    return true;
                }
            }
            echecs.add(etat);
            return false;
        }

        private boolean essayer(int rang, TypeArret type, Demande demande,
                int[] compteurs, int classe) throws SAXException {
            compteurs[classe]++;
            types.add(type);
            servies.add(demande);
            if (chercher(rang + 1)) {
                return true;
            }
            types.remove(types.size() - 1);
            servies.remove(servies.size() - 1);
            compteurs[classe]--;
            return false;
        }
    }

    private static LocalTime heurePlage(Element element, String nom,
            int rang) throws SAXException {
        String valeur = Plan.lireAttribut(element, nom,
                "arrêt n° " + (rang + 1));
        try {
            return LocalTime.parse(valeur);
        } catch (DateTimeParseException erreur) {
            throw new SAXException("Valeur invalide pour l'attribut « " + nom
                    + " » (arrêt n° " + (rang + 1) + ") : « " + valeur
                    + " ».", erreur);
        }
    }

    private static int duree(Element element, String nom, String quoi)
            throws SAXException {
        int valeur = Plan.lireEntier(element, nom, quoi);
        if (valeur < 0) {
            throw new SAXException("Durée sur place négative (" + quoi
                    + ").");
        }
        return valeur;
    }

    private static Noeud adresse(Element element, String nom, String quoi,
            Plan plan) throws SAXException {
        long identifiant = Plan.lireLong(element, nom, quoi);
        Noeud noeud = plan.obtenirNoeud(identifiant);
        if (noeud == null) {
            throw new SAXException("Adresse absente du plan : " + identifiant
                    + " (" + quoi + ").");
        }
        return noeud;
    }
}
