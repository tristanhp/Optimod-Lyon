package fr.insa.optimod.model;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import fr.insa.optimod.model.Tournee.Arret;
import fr.insa.optimod.model.Tournee.TypeArret;

/**
 * Feuille de route d'un livreur : une page HTML, imprimable depuis le
 * navigateur, qui donne l'ordre des arrêts, leurs heures de passage et
 * l'itinéraire rue par rue entre deux arrêts. Le HTML est écrit à la main, sans
 * dépendance.
 */
public final class FeuilleDeRoute {

    /** Texte affiché quand une rue n'a pas de nom dans le plan. */
    private static final String RUE_SANS_NOM = "rue sans nom";

    /** Style de la page, avec une mise en page propre à l'impression. */
    private static final String STYLE = """
            body { font-family: "Segoe UI", Arial, sans-serif; color: #222;
                   margin: 0; background: #fff; }
            main { max-width: 1000px; margin: 0 auto; padding: 24px; }
            h1 { margin: 0 0 4px; font-size: 1.8em; }
            .resume { margin: 0 0 20px; color: #444; }
            .resume p { margin: 2px 0; }
            table { border-collapse: collapse; width: 100%;
                    table-layout: fixed; }
            caption { text-align: left; font-weight: 600;
                      padding-bottom: 6px; }
            th { background: #2f4858; color: #fff; text-align: left;
                 padding: 8px; font-size: 0.9em; }
            td { border-bottom: 1px solid #ccc; padding: 8px;
                 vertical-align: top; overflow-wrap: anywhere; }
            col.type { width: 13%; } col.adresse { width: 27%; }
            col.plan { width: 16%; } col.heure { width: 11%; }
            col.duree { width: 11%; }
            td.centre, th.centre { text-align: center; }
            tr.entrepot td { font-weight: 600; background: #eef3f6; }
            tr.itineraire td { border-bottom: 1px solid #ccc;
                               color: #555; font-size: 0.85em;
                               padding: 4px 8px 8px 24px; }
            .trajet { font-weight: 600; margin-right: 6px; }
            a { color: #1a5f8a; }
            @media print {
              main { max-width: none; padding: 0; }
              th { background: #ddd; color: #000;
                   -webkit-print-color-adjust: exact; }
              tr { break-inside: avoid; }
              a { color: black; text-decoration: none; }
            }
            """;

    /** Format des heures : les secondes n'aident pas le livreur. */
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private FeuilleDeRoute() {
    }

    /**
     * Nom du fichier de la feuille de route d'un livreur.
     *
     * @param livreur numéro du livreur
     * @return {@code feuille-de-route-livreur-N.html}
     */
    public static String nomFichier(int livreur) {
        return "feuille-de-route-livreur-" + livreur + ".html";
    }

    /**
     * Écrit la feuille de route d'un livreur.
     *
     * @param plan    plan, pour retrouver les noms des rues d'un nœud
     * @param livreur numéro du livreur
     * @param tournee tournée du livreur, avec au moins un arrêt
     * @return page HTML complète
     * @throws IllegalArgumentException si la tournée n'a aucun arrêt
     */
    public static String generer(Plan plan, int livreur, Tournee tournee) {
        List<Arret> arrets = tournee.arrets();
        if (arrets.isEmpty()) {
            throw new IllegalArgumentException(
                    "La tournée du livreur " + livreur + " est vide.");
        }
        Map<Noeud, Set<String>> rues = ruesDesArrets(plan, arrets);
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"fr\">\n<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<title>Feuille de route du livreur ").append(livreur)
                .append("</title>\n<style>\n").append(STYLE)
                .append("</style>\n</head>\n<body>\n<main>\n")
                .append("<h1>Livreur n°").append(livreur).append("</h1>\n")
                .append("<div class=\"resume\">\n")
                .append("<p>Heure de départ prévue de l'entrepôt : ")
                .append(heure(arrets.get(0).depart())).append("</p>\n")
                .append("<p>Heure de retour prévue à l'entrepôt : ")
                .append(heure(arrets.get(arrets.size() - 1).arrivee())).append("</p>\n")
                .append("</div>\n")
                .append("<table>\n<caption>Arrêts de la tournée</caption>\n"
                        + "<colgroup><col class=\"type\">"
                        + "<col class=\"adresse\"><col class=\"plan\">"
                        + "<col class=\"heure\"><col class=\"heure\">"
                        + "<col class=\"heure\"><col class=\"duree\">"
                        + "</colgroup>\n<thead>\n<tr><th>Type d'arrêt</th>"
                        + "<th>Adresse</th><th>Plan</th>"
                        + "<th class=\"centre\">Plage annoncée</th>"
                        + "<th class=\"centre\">Heure d'arrivée</th>"
                        + "<th class=\"centre\">Heure de départ</th>"
                        + "<th class=\"centre\">Durée sur place</th>"
                        + "</tr>\n</thead>\n<tbody>\n");
        for (int i = 0; i < arrets.size(); i++) {
            Arret arret = arrets.get(i);
            ajouterArret(html, rues, arret, i == 0);
            if (i < tournee.chemins().size()) {
                ajouterItineraire(html, tournee.chemins().get(i));
            }
        }
        html.append("</tbody>\n</table>\n</main>\n</body>\n</html>\n");
        return html.toString();
    }

    /** Ajoute la ligne du tableau d'un arrêt. */
    private static void ajouterArret(StringBuilder html,
            Map<Noeud, Set<String>> rues,
            Arret arret, boolean premier) {
        String plage;
        String arrivee = "--";
        String depart = "--";
        if (arret.type() == TypeArret.ENTREPOT) {
            plage = heure(premier ? arret.depart() : arret.arrivee());
            if (premier) {
                depart = heure(arret.depart());
            } else {
                arrivee = heure(arret.arrivee());
            }
        } else {
            plage = heure(arret.debutPlage()) + " - " + heure(arret.finPlage());
            arrivee = heure(arret.arrivee());
            depart = heure(arret.depart());
        }
        Noeud noeud = arret.noeud();
        String coordonnees = String.format(Locale.ROOT, "%f, %f",
                noeud.latitude(), noeud.longitude());
        html.append(arret.type() == TypeArret.ENTREPOT
                ? "<tr class=\"entrepot\"><td>"
                : "<tr><td>")
                .append(libelleType(arret.type()))
                .append("</td><td>")
                .append(echapper(rues.get(noeud).isEmpty() ? coordonnees
                        : String.join(", ", rues.get(noeud))))
                .append("</td><td><a href=\"https://maps.google.com/?q=")
                .append(coordonnees.replace(" ", ""))
                .append("\">").append(coordonnees).append("</a></td>")
                .append(cellule(plage)).append(cellule(arrivee))
                .append(cellule(depart))
                .append(cellule(formaterDuree(Duration
                        .between(arret.arrivee(), arret.depart())
                        .getSeconds())))
                .append("</tr>\n");
    }

    /** Ajoute la ligne de l'itinéraire entre deux arrêts. */
    private static void ajouterItineraire(StringBuilder html, Chemin chemin) {
        List<String> etapes = new ArrayList<>();
        String rue = null;
        double longueur = 0;
        for (Troncon troncon : chemin.troncons()) {
            String nom = troncon.nomRue().isBlank() ? RUE_SANS_NOM
                    : troncon.nomRue();
            if (rue != null && !rue.equals(nom)) {
                etapes.add(etape(rue, longueur));
                longueur = 0;
            }
            rue = nom;
            longueur += troncon.longueur();
        }
        if (rue == null) {
            return;
        }
        etapes.add(etape(rue, longueur));
        html.append("<tr class=\"itineraire\"><td colspan=\"7\">"
                + "<span class=\"trajet\">Itinéraire :</span>")
                .append(echapper(String.join(" → ", etapes)))
                .append("</td></tr>\n");
    }

    /** Une étape de l'itinéraire : une rue et la longueur parcourue. */
    private static String etape(String rue, double longueur) {
        return rue + " (" + Math.round(longueur) + " m)";
    }

    /** Noms des rues qui touchent chaque arrêt, en un seul parcours du plan. */
    private static Map<Noeud, Set<String>> ruesDesArrets(Plan plan,
            List<Arret> arrets) {
        Map<Noeud, Set<String>> rues = new HashMap<>();
        arrets.forEach(arret -> rues.put(arret.noeud(),
                new LinkedHashSet<>()));
        for (Troncon troncon : plan.obtenirTroncons()) {
            if (troncon.nomRue().isBlank()) {
                continue;
            }
            Set<String> origine = rues.get(troncon.origine());
            if (origine != null) {
                origine.add(troncon.nomRue());
            }
            Set<String> destination = rues.get(troncon.destination());
            if (destination != null) {
                destination.add(troncon.nomRue());
            }
        }
        return rues;
    }

    /** Heure au format {@code HH:mm}, sans les secondes. */
    private static String heure(LocalTime heure) {
        return heure.format(HEURE);
    }

    private static String libelleType(TypeArret type) {
        return switch (type) {
            case ENTREPOT -> "Entrepôt";
            case COLLECTE -> "Point de collecte";
            case LIVRAISON -> "Point de livraison";
        };
    }

    private static String cellule(String texte) {
        return "<td class=\"centre\">" + echapper(texte) + "</td>";
    }

    /** Écrit une durée comme « 1 min 30 s », ou « -- » si elle est nulle. */
    private static String formaterDuree(long secondes) {
        if (secondes == 0) {
            return "--";
        }
        String minutes = secondes / 60 == 0 ? "" : secondes / 60 + " min";
        String reste = secondes % 60 == 0 ? "" : secondes % 60 + " s";
        return (minutes + " " + reste).trim();
    }

    /** Échappe les caractères spéciaux du HTML. */
    private static String echapper(String texte) {
        return texte.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
