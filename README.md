# Optimod'Lyon

Projet longue durée du PLD Agile (INSA Lyon) : une application Java qui
calcule des **tournées de livraison à vélo** dans Lyon. Un livreur part de
l'entrepôt, récupère chaque colis à son point de collecte, le dépose à son
point de livraison et revient à l'entrepôt le plus tôt possible.

Ce README s'adresse aux membres de l'équipe : installer le projet, utiliser
l'application, comprendre le code et contribuer. Les règles destinées aux
agents IA (et utiles à tous) sont dans [`AGENTS.md`](AGENTS.md).

## Sommaire

- [Ce que fait l'application](#ce-que-fait-lapplication)
- [Installer le projet](#installer-le-projet)
- [Lancer et utiliser l'application](#lancer-et-utiliser-lapplication)
- [Glossaire](#glossaire)
- [Architecture](#architecture)
- [Structure du dépôt](#structure-du-dépôt)
- [Contribuer](#contribuer)
- [Tests](#tests)
- [Qualité et intégration continue](#qualité-et-intégration-continue)
- [Documentation](#documentation)
- [Équipe](#équipe)

## Ce que fait l'application

| Cas d'utilisation | État |
| --- | --- |
| Charger un plan (fichier de plan XML) et l'afficher | fait |
| Charger un programme de livraisons (fichier de demandes XML) et l'afficher | fait |
| Calculer la meilleure tournée (départ à l'heure du fichier, 15 km/h, retour avant 13 h) | fait |
| Afficher les heures de passage de chaque arrêt | fait |
| Ajouter ou supprimer une demande, annuler / rétablir | fait |
| Changer l'ordre des arrêts d'une tournée (Monter / Descendre) | fait |
| Déplacer un point, plusieurs livreurs | à faire |
| Sauvegarder et restaurer les tournées | à faire |

## Installer le projet

### Prérequis

- **JDK 27** (fixé dans [`.sdkmanrc`](.sdkmanrc) ; ce n'est pas une version
  LTS) et **Maven 3.9+**.
- **Sous Windows : WSL** (Ubuntu conseillé). Le projet se clone, se compile et
  se lance dans WSL ; VS Code s'y connecte avec l'extension *WSL*.
- **VS Code** avec les extensions recommandées, proposées à l'ouverture du
  dossier (liste dans [`.vscode/extensions.json`](.vscode/extensions.json)) :
  Java, Maven, tests, débogueur, EditorConfig, Coverage Gutters.

### Installer le JDK avec SDKMAN

```bash
curl -s "https://get.sdkman.io" | bash   # nécessite zip, unzip, curl
sdk env install                           # installe la version de .sdkmanrc
sdk env                                   # l'active dans le terminal courant
```

Si VS Code ne trouve pas le bon JDK, pointer `JAVA_HOME` vers
`~/.sdkman/candidates/java/27.0.0+35-open` (réglage local, à ne pas committer).

### Vérifier l'installation

```bash
git clone https://github.com/tristanhp/Optimod-Lyon.git
cd Optimod-Lyon
mvn clean verify    # compile, lance tous les tests, vérifie la couverture
```

## Lancer et utiliser l'application

### Lancer

```bash
mvn javafx:run
```

Maven télécharge JavaFX au premier lancement ; sous Windows, la fenêtre
s'affiche grâce à WSLg (Windows 11). Autres façons de lancer :

- VS Code : *Exécuter et déboguer*, configuration **Interface JavaFX** ;
- archive exécutable : `mvn clean package` puis
  `java -jar target/optimod-lyon-0.1.0-SNAPSHOT.jar` (avec `--version`, le jar
  affiche seulement son nom). Le jar embarque JavaFX pour Linux, Windows et
  macOS Apple Silicon.

Ne pas lancer `MainApp` directement : JavaFX exige de passer par
`AppLauncher`.

### Utiliser

Les fichiers fournis sont dans `src/main/resources/data/` : trois plans
(`petitPlan.xml`, `moyenPlan.xml`, `grandPlan.xml`) et des fichiers de
demandes (`demandePetit1.xml` … `demandeGrand9.xml`), chacun prévu pour le plan
de même taille.

1. **Importer le plan** : choisir un fichier de plan. La carte s'affiche
   (tronçons en gris, nœuds en points sombres). Molette pour zoomer,
   glisser pour se déplacer, double-clic pour revenir à la vue d'ensemble.
2. **Importer les demandes** : choisir un fichier de demandes. L'entrepôt
   (losange), les points de collecte (carrés) et de livraison (ronds)
   apparaissent, une couleur par demande, et la liste des demandes s'affiche
   à gauche. Survoler un point met sa demande en évidence.
3. **Calculer** : l'application calcule la meilleure tournée et affiche à
   droite les arrêts avec leurs heures d'arrivée et de départ. Si aucune
   tournée possible n'existe (adresse inaccessible, retour après 13 h), un
   message l'explique.
4. **Modifier le programme** :
   - *Ajouter une demande* : cliquer sur le bouton, puis sur le point de
     collecte, puis sur le point de livraison (durées sur place : 5 min) ;
     recliquer sur le bouton annule l'ajout ;
   - *Supprimer la demande* : sélectionner une demande dans la liste, puis
     cliquer sur le bouton sous la liste. Si la tournée est affichée, elle est
     mise à jour tout de suite : l'ordre des autres arrêts est gardé et seuls
     les trajets et les heures sont recalculés (pas de nouveau calcul de 10 s) ;
   - *Monter* / *Descendre* : sélectionner un arrêt dans la liste de la
     tournée (à droite), puis cliquer sur le bouton sous la liste. L'arrêt
     échange sa place avec son voisin et les heures de passage sont recalculées
     tout de suite. L'entrepôt (départ et retour) ne se déplace pas. Si le
     point de livraison d'une demande passerait avant son point de collecte
     (contrainte d'ordre), ou si le retour à l'entrepôt dépasserait 13 h, une
     fenêtre explique le refus et rien ne change ;
   - *Annuler* (Ctrl+Z) et *Rétablir* (Ctrl+Y) : toute modification peut être
     défaite ; si la tournée correspondante a déjà été calculée, elle
     réapparaît sans nouveau calcul.

Un fichier invalide (XML mal formé, adresse absente du plan, durée négative…)
ouvre une fenêtre qui explique l'erreur ; rien n'est perdu.

## Glossaire

Le vocabulaire du projet est fixé par le document Linear
[*AGILE Glossaire du projet*](https://linear.app/les-tigresses/document/agile-glossaire-du-projet-54433845fd38).
Le code, l'interface, les issues et les diagrammes utilisent **ces mots-là**.
Termes principaux et leur traduction dans le code :

| Terme | Sens | Dans le code |
| --- | --- | --- |
| Plan | La carte de la ville, lue dans un fichier de plan | `Plan` |
| Nœud | Un croisement, avec latitude et longitude | `Noeud` |
| Tronçon | Un bout de rue entre deux nœuds, avec nom et longueur | `Troncon` |
| Adresse | Un nœud du plan où l'on passe | `Noeud` (champ `entrepot`, `collecte`…) |
| Entrepôt | Lieu de départ et de retour des livreurs | `ProgrammeLivraisons.obtenirEntrepot()` |
| Livreur | Cycliste qui fait la tournée (un seul par défaut) | — |
| Demande de livraison | Un colis à récupérer puis à déposer | `ProgrammeLivraisons.Demande` |
| Programme de livraisons | Toutes les demandes à traiter, lues dans un fichier ou saisies | `ProgrammeLivraisons` |
| Point de collecte | Où l'on récupère le colis (carré sur la carte) | `Demande.collecte()` |
| Point de livraison | Où l'on dépose le colis (rond sur la carte) | `Demande.livraison()` |
| Collecte / Livraison | Récupération / dépôt du colis par le livreur | — |
| Durée sur place | Temps passé à récupérer ou déposer, en secondes | `dureeCollecte`, `dureeLivraison` |
| Tournée | Trajet d'un livreur : départ à 8 h, retour avant 13 h | `Tournee` |
| Arrêt, heure de passage | Un point de la tournée et ses heures d'arrivée et de départ | `Tournee.Arret` |
| Meilleure tournée | Celle qui revient à l'entrepôt le plus tôt | `Plan.calculerTournee` |
| Tournée possible | Tournée qui respecte toutes les règles | sinon `IllegalStateException` |
| Contrainte d'ordre | Le colis est récupéré avant d'être livré | `GrapheDepandance` |
| Vitesse | 15 km/h pour tous | `Plan.VITESSE_KMH` |
| Modification interactive | Ajout, suppression, déplacement, changement d'ordre | `Commande*` |
| Graphe des plus courts chemins | Chaque arrêt relié aux autres par le plus court chemin | `Plan.calculerPlusCourtsChemins` |
| Voyageur de commerce | Trouver le meilleur ordre pour passer partout | `algo.tsp` (`TSP3`) |
| Séparation et évaluation, borne | Recherche complète qui abandonne les pistes sans espoir | `TemplateTSP` |
| Patron de méthode | Une méthode fixe les étapes, les sous-classes les détails | `TemplateTSP` |
| Limite de temps | Durée maximale du calcul ; on garde le meilleur trouvé | 10 s dans `Plan` |
| Fichier de plan / de demandes | Les fichiers XML d'entrée | `src/main/resources/data/` |
| Heure de départ | Écrite `8:0:0` ou `08:00` dans le fichier de demandes | `obtenirHeureDepart()` |

Un nouveau concept métier ? On l'ajoute d'abord au glossaire Linear, puis on
nomme le code avec ce terme.

## Architecture

Architecture **MVC** stricte, plus trois design patterns. Les décisions sont
expliquées dans les [ADR](docs/adr/README.md) et dessinées dans les
[diagrammes](docs/diagrams/README.md).

| Package | Rôle | Classes principales |
| --- | --- | --- |
| `model` | Données métier, lecture des fichiers XML, **calcul de la tournée** | `Plan`, `ProgrammeLivraisons`, `Tournee`, `Noeud`, `Troncon`, `Chemin`, `Observable` |
| `algo.tsp` | Voyageur de commerce avec contrainte d'ordre (code fourni adapté) | `TemplateTSP`, `TSP3`, `GrapheDepandance` |
| `controller` | Réagit aux actions de l'utilisateur, modifie le modèle | `MainController`, `MapScreen`, `Etat*`, `Commande*` |
| `view` | Fenêtre JavaFX et carte | `MainView`, `MapCanvas`, `MapProjection`, `MainApp` |

Règles de dépendance : le modèle ne connaît ni le contrôleur, ni la vue, ni
JavaFX ; la vue ne lit aucun fichier et ne modifie pas le modèle ; le
contrôleur ne connaît la vue que par l'interface `MapScreen`, ce qui permet de
le tester sans fenêtre.

Les trois design patterns :

- **State** (TIG-90) : le contrôleur délègue chaque clic à son état courant
  (`EtatInitial`, `EtatPlanCharge`, `EtatProgrammeCharge`,
  `EtatCalculEnCours`, `EtatTourneeCalculee`, `EtatAjoutCollecte`,
  `EtatAjoutLivraison`), qui décide de ce qui est permis et des boutons
  actifs ([ADR 0002](docs/adr/0002-design-pattern-state-controleur.md)).
- **Observer** (TIG-91) : `ProgrammeLivraisons` prévient la vue quand ses
  demandes changent ; la vue se redessine seule
  ([ADR 0003](docs/adr/0003-design-pattern-observer-modele-vue.md)).
- **Command** (TIG-92) : chaque modification du programme est une commande
  annulable ; les tournées calculées sont gardées en cache
  ([ADR 0004](docs/adr/0004-design-pattern-command-annulation.md)).

Le calcul de la tournée vit dans `Plan.calculerTournee` : A* entre chaque
couple d'adresses, ordre de passage par séparation et évaluation (`TSP3`),
puis heures de passage ([ADR 0005](docs/adr/0005-nettoyage-du-code-et-glossaire.md)).

Le voyageur de commerce ne sert qu'au bouton *Calculer*. Quand on modifie une
tournée déjà calculée, on fait un **recalcul local** : l'ordre des autres
arrêts est gardé et seuls les trajets touchés (A*) et les heures de passage
sont recalculés, par `Plan.recalculerTournee` (ordre imposé) ; la suppression
d'une demande passe par `Plan.calculerTourneeSansDemande` et le changement
d'ordre d'un arrêt par `Plan.calculerTourneeOrdreChange`. « Recalcul local »
est un terme technique, pas du vocabulaire métier.

## Structure du dépôt

```text
Optimod-Lyon/
├── AGENTS.md / CLAUDE.md      règles pour les agents IA (CLAUDE.md importe AGENTS.md)
├── .github/                   CI, Dependabot, CODEOWNERS, modèle de PR
├── config/                    Checkstyle, exclusions SpotBugs
├── docs/
│   ├── adr/                   décisions d'architecture
│   ├── code-fourni/tsp/       code TSP fourni par les enseignants (lecture seule)
│   └── diagrams/              sources PlantUML des schémas du logiciel
├── scripts/render-diagrams.sh rendu local des diagrammes
├── src/main/java/fr/insa/optimod/
│   ├── Main.java              point d'entrée du jar
│   ├── model/                 plan, programme de livraisons, tournée
│   ├── algo/tsp/              voyageur de commerce
│   ├── controller/            contrôleur, états, commandes
│   └── view/                  interface JavaFX
├── src/main/resources/data/   fichiers de plan et de demandes fournis
├── src/test/java/             tests JUnit 4 (même arborescence que main)
└── pom.xml
```

## Contribuer

Le travail est suivi dans Linear (équipe TIG, projet *AGILE Optimod'Lyon*).

1. **Prendre une issue** dans Linear (`TIG-XX`) et se l'assigner.
2. **Créer la branche** depuis `main` à jour, avec le nom proposé par Linear :
   `tig-XX-titre-court`. Pour de l'outillage sans issue : `chore/titre`.
   Tout autre nom est refusé par la CI.
3. **Coder** en suivant [`AGENTS.md`](AGENTS.md) (MVC, design patterns,
   glossaire, noms de variables en français, Javadoc) et **écrire les tests** avant ou avec le code (les
   tests d'abord sont obligatoires pour les calculs).
4. **Vérifier** en local : `mvn clean verify` doit passer.
5. **Committer** avec des messages courts : `feat: …`, `fix: …`, `refactor: …`,
   `test: …`, `docs: …`, `ci: …`, `chore: …`.
6. **Ouvrir une pull request** vers `main` : titre au même format ou contenant
   `TIG-XX`, [modèle de PR](.github/pull_request_template.md) rempli. Mettre à
   jour les diagrammes et le README si l'architecture ou l'utilisation change.
7. **Relecture** : au moins une personne de l'équipe approuve et le check
   **CI OK** est vert. Le merge se fait en *squash* ; la branche est supprimée ensuite.

PR qui dépendent l'une de l'autre (« pile ») : on les merge dans l'ordre ; après
chaque merge, rebaser la suivante sur `main` (`git rebase --onto main
<ancienne-base> <branche>`) puis `git push --force-with-lease`.

Les décisions d'architecture importantes font l'objet d'un ADR : copier
[`docs/adr/modele.md`](docs/adr/modele.md) et le proposer dans la PR.

## Tests

Organisation détaillée dans
[l'ADR 0001](docs/adr/0001-organisation-des-tests-unitaires.md).

- JUnit 4 ; la classe `Foo` est testée par `FooTest`, dans le même package
  sous `src/test/java`.
- Maven lance automatiquement toutes les classes `*Test` : `mvn test`, ou une
  seule classe avec `mvn test -Dtest=PlanTest`.
- Les fichiers fournis (`/data/petitPlan.xml`…) se chargent dans les tests avec
  `getClass().getResourceAsStream("/data/...")` ; les petits XML écrits à la
  main restent dans le test.
- Le contrôleur se teste avec un faux `MapScreen` et des `Executor`
  synchrones, sans ouvrir de fenêtre (`MainControllerTest`).
- Couverture : `target/site/jacoco/index.html` après `mvn clean verify` ; le
  seuil minimal est `jacoco.line.minimum` dans le `pom.xml`.

## Qualité et intégration continue

| Commande | Rôle |
| --- | --- |
| `mvn clean verify` | tests, couverture, jar (à lancer avant chaque PR) |
| `mvn checkstyle:check` | style (avertissements) |
| `mvn compile spotbugs:check` | détection de bugs |
| `mvn javadoc:javadoc` | documentation du code |
| `./scripts/render-diagrams.sh` | rendu local des diagrammes |

Les mêmes actions existent dans VS Code (*Terminal > Run Task*).

Sur chaque PR, GitHub Actions lance : build, tests et couverture (avec un
résumé des tests), Checkstyle et SpotBugs, hygiène (EditorConfig, Markdown,
liens, XML), syntaxe des diagrammes, détection de secrets, règles de nommage
de la PR et analyse CodeQL. Le check requis est **CI OK**. La compatibilité
avec le JDK 25 est vérifiée sur `main` et chaque lundi.

## Documentation

- **GitHub Pages** : <https://tristanhp.github.io/Optimod-Lyon/> — Javadoc
  avec diagrammes de classes, rapport de couverture et diagrammes, mis à jour à
  chaque merge sur `main`.
- **Diagrammes** : sources dans [`docs/diagrams/`](docs/diagrams/README.md)
  (les images ne sont pas versionnées).
- **Décisions** : [`docs/adr/`](docs/adr/README.md).
- **Release** : pousser un tag `vX.Y.Z` publie le jar exécutable sur GitHub.

## Équipe

<!-- À remplir : nom, prénom, rôle -->

| Nom | Prénom | Rôle |
| --- | --- | --- |
| | | |
