package fr.insa.optimod.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;

/** Vérifie le format XML et la restauration sans recherche d'ordre. */
public class SauvegardeTourneeTest {
    /** Fichiers isolés pour chaque test. */
    @Rule
    public TemporaryFolder dossier = new TemporaryFolder();

    /**
     * La tournée restaurée a les mêmes arrêts et chemins ; le fichier ne dit
     * pas si l'ordre était prouvé optimal, elle ne l'est donc plus.
     */
    private static void assertMemeTourneeNonProuvee(Tournee attendue,
            Tournee restauree) {
        assertEquals(attendue.arrets(), restauree.arrets());
        assertEquals(attendue.chemins(), restauree.chemins());
        assertFalse(restauree.prouveeOptimale());
    }

    /** Une sauvegarde conserve demandes, ordre, trajets et horaires. */
    @Test
    public void allerRetourConserveProgrammeEtTournee() throws Exception {
        Path fichierPlan = dossier.newFile("plan.xml").toPath();
        Files.writeString(fichierPlan, PlansPourTests.PETIT_RESEAU);
        Plan plan = new Plan(fichierPlan.toString());
        ProgrammeLivraisons programme = new ProgrammeLivraisons(
                plan.obtenirNoeud(1), LocalTime.of(8, 0),
                List.of(new Demande(plan.obtenirNoeud(3),
                        plan.obtenirNoeud(2), 120, 240)));
        programme.definirTournees(plan.calculerTournees(programme));
        Tournee tournee = programme.obtenirTournees().get(1);
        Path fichier = dossier.newFile("tournee.xml").toPath();
        SauvegardeTournee.sauvegarder(fichier, fichierPlan, programme);
        SauvegardeTournee restauree = SauvegardeTournee.restaurer(fichier);
        assertEquals(programme.obtenirDemandes(),
                restauree.programme().obtenirDemandes());
        assertEquals(programme.obtenirHeureDepart(),
                restauree.programme().obtenirHeureDepart());
        assertMemeTourneeNonProuvee(tournee, restauree.tournee());
        assertTrue(Files.readString(fichier).contains("nombreLivreurs=\"1\""));
        Element racine = Plan.creerLecteurXml().parse(fichier.toFile())
                .getDocumentElement();
        assertEquals("sauvegardeTournee", racine.getTagName());
        assertEquals("1", racine.getAttribute("version"));
        assertEquals(fichierPlan.toAbsolutePath().toString(),
                racine.getAttribute("plan"));
        assertEquals("1", racine.getAttribute("entrepot"));
        assertEquals("08:00", racine.getAttribute("heureDepart"));
        NodeList demandes = racine.getElementsByTagName("demande");
        assertEquals(1, demandes.getLength());
        Element demande = (Element) demandes.item(0);
        assertEquals("3", demande.getAttribute("collecte"));
        assertEquals("2", demande.getAttribute("livraison"));
        assertEquals("120", demande.getAttribute("dureeCollecte"));
        assertEquals("240", demande.getAttribute("dureeLivraison"));
        NodeList arrets = racine.getElementsByTagName("arret");
        assertEquals(4, arrets.getLength());
        List<String> adresses = List.of("1", "3", "2", "1");
        List<String> durees = List.of("0", "120", "240", "0");
        for (int i = 0; i < arrets.getLength(); i++) {
            Element arret = (Element) arrets.item(i);
            assertEquals(adresses.get(i), arret.getAttribute("noeud"));
            assertEquals(durees.get(i), arret.getAttribute("duree"));
        }
    }

    /** Les plages annoncées sont conservées même après changement d'horaire. */
    @Test
    public void allerRetourConservePlagesAnnoncees() throws Exception {
        Path fichierPlan = dossier.newFile("plan.xml").toPath();
        Files.writeString(fichierPlan, PlansPourTests.PETIT_RESEAU);
        Plan plan = new Plan(fichierPlan.toString());
        ProgrammeLivraisons programme = new ProgrammeLivraisons(
                plan.obtenirNoeud(1), LocalTime.of(8, 0),
                List.of(new Demande(plan.obtenirNoeud(3),
                        plan.obtenirNoeud(2), 120, 240)));
        Tournee avant = plan.calculerTournees(programme).get(1);
        List<Tournee.Arret> arrets = avant.arrets().stream()
                .map(arret -> new Tournee.Arret(arret.noeud(), arret.arrivee(),
                        arret.depart(), arret.type(), arret.demande(),
                        LocalTime.of(6, 0), LocalTime.of(7, 0)))
                .toList();
        Tournee annoncee = new Tournee(arrets, avant.chemins(), true);
        programme.definirTournees(Map.of(1, annoncee));
        Path fichier = dossier.newFile("plages.xml").toPath();
        SauvegardeTournee.sauvegarder(fichier, fichierPlan, programme);
        assertMemeTourneeNonProuvee(annoncee,
                SauvegardeTournee.restaurer(fichier).tournee());
        String contenu = Files.readString(fichier);
        Files.writeString(fichier, contenu.replace(" finPlage=\"07:00\"", ""));
        assertThrows(SAXException.class,
                () -> SauvegardeTournee.restaurer(fichier));
    }

    /** Plusieurs livreurs ne tiennent pas dans le format : refusés. */
    @Test
    public void plusieursLivreursRefuses() throws Exception {
        Path fichierPlan = dossier.newFile("plan.xml").toPath();
        Files.writeString(fichierPlan, PlansPourTests.PETIT_RESEAU);
        Plan plan = new Plan(fichierPlan.toString());
        ProgrammeLivraisons programme = new ProgrammeLivraisons(
                plan.obtenirNoeud(1), LocalTime.of(8, 0), List.of());
        programme.definirNombreLivreurs(2);
        programme.definirTournees(plan.calculerTournees(programme));
        Path fichier = dossier.newFile("deux.xml").toPath();
        assertThrows(IOException.class, () -> SauvegardeTournee.sauvegarder(
                fichier, fichierPlan, programme));
    }

    /** Un document étranger n'est pas une sauvegarde. */
    @Test
    public void fichierInvalideRefuse() throws Exception {
        Path fichier = dossier.newFile().toPath();
        Files.writeString(fichier, "<invalide/>");
        assertThrows(SAXException.class,
                () -> SauvegardeTournee.restaurer(fichier));
    }

    /** Une référence de plan manquante produit une erreur lisible. */
    @Test
    public void planIntrouvableRefuse() throws Exception {
        Path fichier = dossier.newFile().toPath();
        Files.writeString(fichier,
                "<sauvegardeTournee version=\"1\" nombreLivreurs=\"1\" "
                        + "plan=\"absent.xml\"/>");
        assertThrows(SAXException.class,
                () -> SauvegardeTournee.restaurer(fichier));
    }

    /** Le lecteur refuse les déclarations de type de document. */
    @Test
    public void doctypeInterdit() throws Exception {
        Path fichier = dossier.newFile().toPath();
        Files.writeString(fichier, "<!DOCTYPE sauvegardeTournee>"
                + "<sauvegardeTournee/>");
        assertThrows(SAXException.class,
                () -> SauvegardeTournee.restaurer(fichier));
    }

    /** Une tournée vide garde seulement l'entrepôt. */
    @Test
    public void programmeVideRestaure() throws Exception {
        Path fichierPlan = dossier.newFile("plan.xml").toPath();
        Files.writeString(fichierPlan, PlansPourTests.PETIT_RESEAU);
        Plan plan = new Plan(fichierPlan.toString());
        ProgrammeLivraisons programme = new ProgrammeLivraisons(
                plan.obtenirNoeud(1), LocalTime.of(8, 0), List.of());
        Path fichier = dossier.newFile("vide.xml").toPath();
        programme.definirTournees(plan.calculerTournees(programme));
        Tournee tournee = programme.obtenirTournees().get(1);
        SauvegardeTournee.sauvegarder(fichier, fichierPlan, programme);
        assertMemeTourneeNonProuvee(tournee,
                SauvegardeTournee.restaurer(fichier).tournee());
    }

    /** Une livraison précédant sa collecte invalide la sauvegarde. */
    @Test
    public void ordreContraireAuProgrammeRefuse() throws Exception {
        Path fichierPlan = dossier.newFile("plan.xml").toPath();
        Files.writeString(fichierPlan, PlansPourTests.PETIT_RESEAU);
        Path fichier = dossier.newFile("inverse.xml").toPath();
        Files.writeString(fichier, "<sauvegardeTournee version=\"1\" "
                + "nombreLivreurs=\"1\" plan=\"plan.xml\" entrepot=\"1\" "
                + "heureDepart=\"08:00\">"
                + "<demande collecte=\"3\" livraison=\"2\" "
                + "dureeCollecte=\"120\" dureeLivraison=\"240\"/>"
                + "<arret noeud=\"1\" duree=\"0\"/>"
                + "<arret noeud=\"2\" duree=\"240\"/>"
                + "<arret noeud=\"3\" duree=\"120\"/>"
                + "<arret noeud=\"1\" duree=\"0\"/>"
                + "</sauvegardeTournee>");
        assertThrows(SAXException.class,
                () -> SauvegardeTournee.restaurer(fichier));
    }

    /** Une heure illisible devient une erreur XML, pas une erreur de calcul. */
    @Test
    public void heureInvalideRefusee() throws Exception {
        Path fichierPlan = dossier.newFile("plan.xml").toPath();
        Files.writeString(fichierPlan, PlansPourTests.PETIT_RESEAU);
        Path fichier = dossier.newFile("heure.xml").toPath();
        Files.writeString(fichier, "<sauvegardeTournee version=\"1\" "
                + "nombreLivreurs=\"1\" plan=\"plan.xml\" entrepot=\"1\" "
                + "heureDepart=\"25:00\"><arret noeud=\"1\" duree=\"0\"/>"
                + "</sauvegardeTournee>");
        assertThrows(SAXException.class,
                () -> SauvegardeTournee.restaurer(fichier));
    }
}
