# AGENTS.md

Instructions pour les agents IA (Claude Code, Codex, Cursor, Copilot, Gemini…)
et pour les humains qui travaillent sur Optimod'Lyon. Ce fichier est la source
unique : `CLAUDE.md` se contente de l'importer. Le [README](README.md) détaille
l'installation et l'utilisation ; ici, on ne garde que ce qu'il faut savoir
pour modifier le code.

## Projet

Projet PLD Agile (INSA Lyon) : application JavaFX qui calcule des tournées de
livraison à vélo (*Pickup & Delivery*) sur un plan de Lyon lu en XML. Équipe de
5 personnes, issues suivies dans Linear (projet *AGILE Optimod'Lyon*, équipe
TIG, identifiants `TIG-XX`).

- Java **27** (`.sdkmanrc`), Maven 3.9+, JavaFX, JUnit **4**.
- Pas de dépendance nouvelle sans en parler à l'équipe (le `pom.xml` est relu
  par tous, voir `.github/CODEOWNERS`).

## Commandes

| But | Commande |
| --- | --- |
| Tout vérifier avant une PR (tests, couverture, jar) | `mvn clean verify` |
| Tests seuls / une classe | `mvn test` / `mvn test -Dtest=PlanTest` |
| Style | `mvn checkstyle:check` |
| Bugs | `mvn compile spotbugs:check` |
| Javadoc | `mvn javadoc:javadoc` |
| Lancer l'interface | `mvn javafx:run` |
| Rendre les diagrammes | `./scripts/render-diagrams.sh` |

`mvn clean verify` doit passer avant de proposer un commit ou une PR. Le seuil
de couverture JaCoCo est `jacoco.line.minimum` dans le `pom.xml`.

## Architecture (MVC strict)

```text
src/main/java/fr/insa/optimod/
├── Main.java     point d'entrée du jar (--version : pas d'interface)
├── model/        données métier et lecture XML : Plan, Noeud, Troncon,
│                 Livraison, Demande, Entrepot, Etape, Passage,
│                 Observable, Observateur
├── algo/         plus courts chemins (AEtoile), GrapheChemins, CalculHoraires,
│   └── tsp/      TSP avec précédence (TemplateTSP et ses variantes)
├── controller/   MainController, MapScreen (ce que le contrôleur attend
│                 de l'écran), états (Etat*) et commandes (Commande*)
└── view/         JavaFX : MainView, MapCanvas, MapProjection,
                  LibellesDemandes, MainApp, AppLauncher
```

Règles de dépendance, à ne pas casser :

- `model` n'importe ni `controller`, ni `view`, ni JavaFX.
- `view` ne lit aucun fichier et ne modifie pas le modèle : elle transmet les
  actions de l'utilisateur au contrôleur.
- `controller` ne connaît la vue qu'à travers l'interface `MapScreen` : c'est
  ce qui permet de tester le contrôleur sans fenêtre (`MainControllerTest`
  utilise un faux écran). Toute nouvelle méthode d'écran passe par
  `MapScreen`.
- Le calcul long tourne hors du thread JavaFX : le contrôleur reçoit deux
  `Executor` (arrière-plan, interface) pour rester testable.
- Lancer l'interface via `AppLauncher`, jamais `MainApp` directement.

### Design patterns

Trois patterns structurent l'application (ADR 0002 à 0004 dans
[`docs/adr/`](docs/adr/README.md)). Toute nouvelle fonctionnalité passe par
eux plutôt que de les contourner.

- **State** (`controller/Etat` et `Etat*`) : `MainController` délègue chaque
  action de l'utilisateur à son état courant, qui décide si l'action est
  permise, quels boutons sont actifs (`MapScreen.activerActions`) et quel est
  l'état suivant. Une nouvelle action = une méthode par défaut dans `Etat`
  (ignorée), redéfinie dans les états où elle est permise. Pas de booléen
  d'état dans le contrôleur.
- **Observer** (`model/Observable`, `model/Observateur`) : le modèle prévient
  la vue quand il change ; la vue se redessine seule. Une méthode qui modifie
  un objet observable appelle `notifierObservateurs()`.
- **Command** (`controller/Commande`, `ListeDeCommandes`) : toute
  modification des demandes faite par l'utilisateur est une `Commande`
  réversible, exécutée par `MainController.executer(...)`, pour qu'Annuler et
  Rétablir fonctionnent. Ne jamais modifier le modèle directement depuis un
  état ou la vue.

Les décisions d'architecture sont dans [`docs/adr/`](docs/adr/README.md) ; une
nouvelle décision structurante = un nouvel ADR (copier `docs/adr/modele.md`).

## Règles de nettoyage du code

Issues du document Linear *Nettoyage Code OPTIMOD* ; à appliquer au code
nouveau ou modifié.

- Code propre, simple à maintenir et à faire évoluer.
- **Respecter le MVC** (règles ci-dessus).
- Minimum de getters et setters : n'exposer que ce dont un appelant a besoin.
- Minimiser le nombre de classes : pas de classe pour une seule méthode
  utilitaire, pas de couche d'abstraction « au cas où ». Les design patterns
  demandés (State, Observer, Command) sont l'exception assumée.

Cible du nettoyage, à faire dans des PR dédiées (pas en passant dans une autre
PR) :

- `AEtoile`, `CalculateurTournee` et `CalculHoraires` deviennent des méthodes
  privées de `Plan` (la tournée se calcule à partir d'une liste de noeuds).
- `GrapheChemins` : le TSP réutilise `GrapheDepandance` ; le reste va dans
  `Plan`.
- `Passage` est supprimé ; `Demande` devient une paire
  (noeud de collecte, noeud de livraison) dans `Livraison`.
- `algo/tsp` : **uniquement à la fin**, ne garder que la dernière version du
  TSP et supprimer les intermédiaires (`TSP1`, `TSP2`…).

## Conventions de code

- **Identifiants en français** (classes, méthodes, variables, tests). Les
  accesseurs s'appellent `obtenirX()`, pas `getX()`. Seuls les noms imposés
  par Java ou JavaFX restent en anglais (`main`, `start`, `layoutChildren`…).
  Du code ancien en anglais (`showTour`, `setOnCalculate`…) existe : le
  franciser quand on y touche.
- Javadoc en français sur toute classe et toute méthode publique
  (`mvn javadoc:javadoc` sans erreur).
- 4 espaces, UTF-8, LF, lignes de 80 caractères si possible
  (`.editorconfig`, `config/checkstyle.xml`).
- Préférer les `record` et les champs `final` ; renvoyer des collections non
  modifiables.
- Lecture XML : réutiliser la configuration sécurisée existante
  (`FEATURE_SECURE_PROCESSING`, DOCTYPE interdit). Une erreur de fichier se
  traduit par une `SAXException` avec un message lisible par l'utilisateur.

## Tests

Voir [l'ADR 0001](docs/adr/0001-organisation-des-tests-unitaires.md).

- JUnit 4. La classe `Foo` est testée par `FooTest`, dans le **même package**
  sous `src/test/java`.
- Pas de suite de tests : Surefire lance toutes les classes `*Test`.
- Noms de méthodes descriptifs en français (`longueurNegativeRefusee()`).
- Données de test dans `src/test/resources`, chargées avec
  `getClass().getResourceAsStream(...)`. Les XML fournis par les enseignants
  sont dans `src/main/resources/data/`.
- Tester le contrôleur avec un faux `MapScreen` et des `Executor` synchrones,
  jamais en ouvrant une fenêtre JavaFX.
- Toute correction de bug arrive avec le test qui l'aurait détecté.

## Diagrammes

- Seules les sources PlantUML `docs/diagrams/*.puml` sont versionnées. Les SVG
  ne le sont pas (leur rendu dépend des polices de la machine) : ils sont
  publiés sur GitHub Pages et joints en artefact à chaque CI. En local,
  `./scripts/render-diagrams.sh` les génère dans `docs/diagrams/out/` (ignoré
  par git) ; ne jamais les committer.
- Les diagrammes qui ne sont pas de séquence contiennent
  `!pragma layout smetana` (pas de Graphviz).
- Mettre à jour les diagrammes quand l'architecture change (nouvelle classe,
  nouveau pattern, nouvelle séquence), ainsi que les blocs Mermaid de
  `docs/diagrams/README.md` qui en sont la copie lisible sur GitHub.

## Workflow git et PR

- Ne jamais pousser sur `main`. Une branche par issue Linear, nommée comme le
  propose Linear : `tig-XX-titre-court`. Outillage sans issue : `chore/titre`.
  Tout autre nom fait échouer le workflow *Règles de PR*.
- Titre de PR : `type: description` (`feat`, `fix`, `docs`, `refactor`,
  `test`, `ci`, `chore`…) ou contenant `TIG-XX`.
- Remplir le [modèle de PR](.github/pull_request_template.md) : résumé, issue
  Linear, checklist.
- Commits courts au format `type: description`.
- Le check requis est **CI OK** ; une personne de l'équipe relit avant le
  merge.

## À ne pas faire

- Modifier `docs/code-fourni/` (code TSP fourni, en lecture seule).
- Committer des SVG de diagrammes (`docs/diagrams/out/`).
- Committer des réglages locaux (`JAVA_HOME`, chemins absolus, `target/`).
- Désactiver un test, baisser le seuil de couverture ou ajouter une exclusion
  SpotBugs/Checkstyle pour faire passer la CI.
- Mélanger dans une même PR une fonctionnalité et un gros refactor.
