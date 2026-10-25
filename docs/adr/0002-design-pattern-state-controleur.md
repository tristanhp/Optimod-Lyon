# 0002. Design pattern State dans le contrôleur

- **Statut** : Proposée
- **Date** : 2026-10-25

## Contexte

`MainController` décidait de ce qui était permis avec des tests sur ses
champs (`plan != null`, `livraison != null`, `calculEnCours`) répétés dans
chaque méthode. Chaque nouveau cas d'utilisation (modifier une tournée,
annuler, ajouter une demande) aurait ajouté un booléen et des conditions
croisées. Pendant un calcul, l'utilisateur pouvait aussi importer un autre plan :
le résultat devait alors être ignoré par une comparaison de références.

Issue Linear : TIG-90.

## Décision

1. Le contrôleur délègue chaque clic à son **état courant**, une
   implémentation de l'interface `Etat` (package `controller`, visibilité
   package) : `EtatInitial`, `EtatPlanCharge`, `EtatProgrammeCharge`,
   `EtatCalculEnCours`, `EtatTourneeCalculee`.
2. L'état décide si l'action est permise et quel est l'état suivant
   (`controleur.changerEtat(...)`). Le contrôleur ne fournit que les
   opérations élémentaires : lire un fichier (`lirePlan`, `lireProgramme`, qui
   renvoient `Lecture.ANNULEE | REUSSIE | ECHOUEE`), lancer le calcul, mettre
   à jour l'écran.
3. Les règles communes sont des **méthodes par défaut** de `Etat` (importer un
   plan ou des demandes est possible, calculer ne l'est pas) ; chaque état ne
   redéfinit que ses exceptions.
4. L'état fixe aussi les **boutons actifs** via
   `MapScreen.activerActions(...)` : la vue ne
   décide plus elle-même quel bouton activer.
5. Pendant `EtatCalculEnCours`, tous les imports sont bloqués : le plan et les
   demandes ne peuvent plus changer pendant le calcul.

Diagramme : [`etats-controleur.puml`](../diagrams/etats-controleur.puml).

## Alternatives envisagées

- **Garder des booléens** : simple aujourd'hui, mais la combinatoire grandit à
  chaque cas d'utilisation et les règles sont dispersées.
- **Une `enum` d'états** : un seul fichier (moins de classes), mais les
  comportements spécifiques se retrouvent dans des `switch` ou des méthodes
  d'`enum` moins lisibles ; la version « une classe par état » est celle du
  cours et la plus claire à présenter.
- **Laisser les imports pendant le calcul** (ancien comportement) : oblige à
  comparer les références au retour du calcul et à gérer un calcul « orphelin ».

## Conséquences

- Ajouter un cas d'utilisation = ajouter un état ou une méthode à `Etat`, sans
  toucher aux autres états.
- Cinq petites classes de plus dans `controller` (exception assumée à la règle
  « minimiser le nombre de classes »).
- Les tests du contrôleur vérifient les transitions (`obtenirEtat()`, visible
  dans le package) et les boutons actifs.
- Le contrôleur garde une référence vers l'écran : SpotBugs signale
  `EI_EXPOSE_REP2`, exclu de façon ciblée dans `config/spotbugs-exclude.xml`.
