# Optimod'Lyon

Projet PLD Agile (INSA Lyon) : application Java d'optimisation de tournées de
livraison à vélo (*Pickup & Delivery*) dans Lyon.

L'application :

1. charge un plan de ville au format XML ;
2. charge des demandes de livraison (Pickup & Delivery) au format XML ;
3. calcule la tournée optimale (plus courts chemins avec Dijkstra, puis TSP
   avec contraintes de précédence) ;
4. affiche la tournée sur une carte ;
5. permet de la modifier et de la sauvegarder.

## Prérequis

- **JDK 27** (version fixée dans [`.sdkmanrc`](.sdkmanrc)). Attention : le JDK 27
  n'est pas une version LTS.
- **Maven 3.9+**
- **VS Code** avec les extensions recommandées (VS Code te les propose à
  l'ouverture du dossier, sinon voir `.vscode/extensions.json`) :
  - Extension Pack for Java (`vscjava.vscode-java-pack`)
  - Maven for Java (`vscjava.vscode-maven`)
  - Test Runner for Java (`vscjava.vscode-java-test`)
  - Debugger for Java (`vscjava.vscode-java-debug`)
  - EditorConfig (`EditorConfig.EditorConfig`)
  - Coverage Gutters (`ryanluker.vscode-coverage-gutters`)
- **WSL** (Ubuntu conseillé) si tu es sous Windows, avec VS Code connecté à WSL
  via l'extension *WSL*. Le projet se clone et se compile dans WSL.

### Installer le JDK avec SDKMAN

```bash
curl -s "https://get.sdkman.io" | bash   # nécessite zip, unzip, curl
sdk env install                           # installe la version de .sdkmanrc
sdk env                                   # l'active dans le terminal courant
```

Si VS Code ne trouve pas le bon JDK, pointe `JAVA_HOME` vers
`~/.sdkman/candidates/java/27.0.0+35-open` (réglage local, ne pas committer).

## Commandes

| Action | Commande |
| --- | --- |
| Compiler | `mvn compile` |
| Lancer les tests | `mvn test` |
| Tests + couverture (rapports JaCoCo) | `mvn clean verify` |
| Générer la Javadoc | `mvn javadoc:javadoc` |
| Produire le jar exécutable | `mvn clean package` |
| Style (Checkstyle) | `mvn checkstyle:check` |
| Détection de bugs (SpotBugs) | `mvn compile spotbugs:check` |
| Javadoc avec diagrammes de classes UML | `mvn -Puml javadoc:javadoc` |
| Rendre les diagrammes PlantUML | `./scripts/render-diagrams.sh` |
| Lancer l'application | `java -jar target/optimod-lyon-0.1.0-SNAPSHOT.jar` |
| Lancer l'interface JavaFX | `mvn javafx:run` |

- Rapport de couverture HTML : `target/site/jacoco/index.html`
- Rapport de couverture XML : `target/site/jacoco/jacoco.xml` (lu par Coverage
  Gutters : commande *Coverage Gutters: Display Coverage* dans VS Code)
- Javadoc : `target/reports/apidocs/index.html`

- Diagrammes PlantUML : sources dans `docs/diagrams/*.puml`, images (SVG)
  versionnées dans `docs/diagrams/out/`. Sur une PR, la CI régénère les images
  quand un `.puml` change et les pousse sur la branche : il suffit de
  récupérer le commit (`git pull`). Extension VS Code optionnelle :
  *PlantUML* (`jebbs.plantuml`).

Les mêmes actions sont disponibles dans VS Code via *Terminal > Run Task*
(`.vscode/tasks.json`).

## Interface graphique (itération 1)

Première version de l'interface JavaFX : charger un plan de ville au format
XML et en afficher la carte (tickets TIG-32 et TIG-58).

### Lancer l'interface

Depuis la racine du projet, avec le JDK 27 et Maven 3.9+ (voir
[Prérequis](#prérequis)) :

```bash
mvn javafx:run
```

Maven télécharge JavaFX au premier lancement. Sous Windows, il faut lancer la
commande dans WSL (Ubuntu) ; la fenêtre s'affiche avec WSLg (Windows 11).

Autres façons de lancer l'interface :

- VS Code : *Exécuter et déboguer*, configuration **Interface JavaFX** ;
- jar exécutable : `mvn clean package` puis
  `java -cp target/optimod-lyon-0.1.0-SNAPSHOT.jar fr.insa.optimod.view.AppLauncher`.

Ne pas lancer `MainApp` directement : JavaFX exige de passer par la classe
`AppLauncher`.

### Utiliser l'interface

La fenêtre comporte un bouton **Importer** en haut à gauche et la carte
dessous (vide au départ).

1. Cliquer sur **Importer**.
2. Choisir un plan XML dans le sélecteur de fichier. Les plans fournis sont
   dans `src/main/resources/data/` : `petitPlan.xml` (308 intersections),
   `moyenPlan.xml` (1 448) et `grandPlan.xml` (3 736).
3. La carte s'affiche, orientée nord en haut et adaptée à la taille de la
   fenêtre :
   - les **tronçons** (segments de route) en gris ;
   - les **intersections** (noeuds) en points sombres.
4. Importer un autre fichier remplace la carte affichée. Annuler le sélecteur
   ne change rien.

Si la lecture du fichier échoue (XML mal formé, attribut illisible, tronçon
vers un noeud inconnu, etc.), une fenêtre d'information « Fichier XML
invalide » explique l'erreur, la carte est vidée et l'utilisateur peut
réessayer avec un autre fichier. Un XML bien formé qui n'est pas un plan
(par exemple `demandePetit1.xml`) n'est pas encore refusé : il donne une
carte vide.

### Organisation du code

| Rôle | Classes |
| --- | --- |
| Modèle (`model`) | `Plan` (qui lit aussi le XML du plan), `Noeud`, `Troncon` |
| Contrôleur (`controller`) | `MainController` ; interface `MapScreen` qui décrit ce que le contrôleur attend de l'écran |
| Vue (`view`) | `MainView` (bouton et fenêtres), `MapCanvas` (dessin), `MapProjection` (latitude/longitude vers pixels), `MainApp` et `AppLauncher` (démarrage) |

La vue ne lit pas les fichiers : le clic sur **Importer** appelle le
contrôleur, qui construit le modèle à partir du fichier (`new Plan(chemin)`) et
demande à la vue de l'afficher. Grâce à `MapScreen`, le contrôleur est testé
sans fenêtre (`MainControllerTest`).

## Structure du projet

```text
optimod-lyon/
├── .github/                   CI (workflows), Dependabot, CODEOWNERS, modèle de PR
├── config/checkstyle.xml      règles de style
├── docs/                      livrables
│   ├── adr/                   décisions d'architecture (ADR)
│   ├── code-fourni/tsp/       code TSP fourni (TSP.jar), en lecture seule
│   └── diagrams/              sources PlantUML des diagrammes
├── scripts/                   outils (rendu des diagrammes)
├── src/
│   ├── main/
│   │   ├── java/fr/insa/optimod/
│   │   │   ├── Main.java      point d'entrée
│   │   │   ├── model/         plan, intersections, tronçons, demandes, tournées
│   │   │   ├── controller/    coordination vue / modèle / algorithmes
│   │   │   ├── view/          interface utilisateur et carte
│   │   │   ├── algo/          graphe des plus courts chemins, TSP
│   │   │   └── xml/           lecture des fichiers XML
│   │   └── resources/data/    XML fournis (plans et demandes de livraison)
│   └── test/
│       ├── java/fr/insa/optimod/   tests JUnit 4 (même arborescence que main)
│       └── resources/              données de test (petits XML...)
├── pom.xml
└── .vscode/                   configuration VS Code partagée
```

## Conventions de code

- Style : [conventions Java d'Oracle](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html)
  (noms en `camelCase` / `PascalCase` / `MAJUSCULES_POUR_LES_CONSTANTES`,
  lignes de 80 caractères maximum si possible).
- Indentation de 4 espaces, UTF-8, fins de ligne LF
  (voir [`.editorconfig`](.editorconfig)).
- Le code est formaté et les imports organisés à la sauvegarde dans VS Code.
- Toute classe et toute méthode publique est documentée en Javadoc ;
  `mvn javadoc:javadoc` doit passer sans erreur.
- Tests unitaires : voir la section [Tests unitaires](#tests-unitaires).

## Tests unitaires

Les choix sont détaillés dans
[l'ADR 0001](docs/adr/0001-organisation-des-tests-unitaires.md).

- Tests avec JUnit 4. Une classe `Foo` est testée par `FooTest`.
- Le test est dans le **même package** que la classe testée, donc dans la même
  arborescence sous `src/test/java` :

  ```text
  src/main/java/fr/insa/optimod/model/Troncon.java
  src/test/java/fr/insa/optimod/model/TronconTest.java
  ```

- Pas de classe qui appelle tous les tests : Maven (Surefire) lance
  automatiquement toutes les classes `*Test`, en local comme en CI.
  `MainTest` est seulement le test de `Main`.
- Les fichiers de test (petits XML, etc.) vont dans `src/test/resources` et se
  chargent avec `getClass().getResourceAsStream("/xml/petitPlan.xml")`.
- Lancer tous les tests : `mvn test` ; une seule classe :
  `mvn test -Dtest=TronconTest`.

## Décisions d'architecture (ADR)

Les décisions techniques importantes sont décrites dans
[`docs/adr/`](docs/adr/README.md), une fiche par décision (contexte, décision,
alternatives, conséquences). Pour en proposer une, copier
[`docs/adr/modele.md`](docs/adr/modele.md) et ouvrir une PR.

## Workflow git

- `main` est la branche stable : on ne pousse pas directement dessus.
- Une branche par issue Linear, nommée `tig-XX-titre`
  (ex. `tig-12-parsing-plan-xml`), créée depuis `main` à jour. Pour l'outillage
  sans issue : `chore/titre`.
- Commits courts et explicites (ex. `feat: parse le plan de ville`).
- Ouvrir une *pull request* vers `main`. Le titre est soit `feat: ...` / `fix: ...`
  / `docs: ...` (etc.), soit contient l'ID Linear (`TIG-12`).
- Au moins une personne de l'équipe relit la PR avant le merge ; la revue
  Copilot, si elle est activée, est un premier avis et ne remplace pas cette
  approbation.
- Avant de proposer une PR : `mvn clean verify` doit passer.
- Une fois mergée, la branche est supprimée.

## Intégration continue (GitHub Actions)

Le check **CI OK** doit être vert avant de merger. Il regroupe :

| Job | Contenu |
| --- | --- |
| Build, tests et couverture | `mvn clean verify` (JDK 27), seuil de couverture JaCoCo, Javadoc, test du jar |
| Qualité | Checkstyle (avertissements) et SpotBugs |
| Hygiène | EditorConfig, lint Markdown, liens, XML bien formés |
| Diagrammes | rendu des `.puml` (échoue sur une erreur de syntaxe ou si les images committées sont périmées) |
| Secrets | détection de secrets (gitleaks) |

Autres workflows : règles de nommage des branches et des titres de PR, analyse
CodeQL, publication de la documentation sur GitHub Pages à chaque push sur
`main` (Javadoc avec diagrammes de classes, couverture, diagrammes), et
publication d'une *release* avec le jar quand on pousse un tag `vX.Y.Z`.

Le seuil de couverture est défini par la propriété `jacoco.line.minimum` du
`pom.xml` ; on le relève à chaque itération.

## Équipe

<!-- À remplir : nom, prénom, rôle -->

| Nom | Prénom | Rôle |
| --- | --- | --- |
| | | |
