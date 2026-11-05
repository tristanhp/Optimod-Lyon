# AGENTS.md

Instructions pour tout agent IA (Claude Code, Codex, Cursor, Copilot,
Gemini…) qui travaille dans ce dépôt. C'est la source unique : `CLAUDE.md` se
contente de l'importer. Le [README](README.md) explique le projet aux humains
(installation, utilisation, glossaire complet, workflow) ; ce fichier dit
**comment modifier le code** et ce qui est interdit. En cas de doute, suivre
ce fichier, puis le README, puis les [ADR](docs/adr/README.md).

## Projet en bref

Application JavaFX qui calcule des tournées de livraison à vélo à Lyon (PLD
Agile, INSA Lyon). Équipe de 5 étudiants ; issues dans Linear (équipe TIG,
projet *AGILE Optimod'Lyon*, identifiants `TIG-XX`). Code, commentaires,
Javadoc, messages de commit et PR **en français**.

- Java **27** (`.sdkmanrc`), Maven 3.9+, JavaFX, JUnit **4**.
- Aucune nouvelle dépendance Maven sans accord de l'équipe.

## Avant de rendre la main

Lancer, dans cet ordre, et corriger jusqu'à ce que tout passe :

```bash
mvn clean verify               # tests, couverture minimale, jar
mvn compile spotbugs:check     # aucun bug Medium ou plus
mvn checkstyle:check           # pas de nouvel avertissement dans le code touché
mvn javadoc:javadoc            # aucune erreur
./scripts/render-diagrams.sh   # si un .puml a changé : il doit se rendre
```

Ne jamais déclarer une tâche terminée si l'une de ces commandes échoue ;
dire ce qui échoue.

## Architecture (MVC strict)

| Package | Contient | Peut dépendre de |
| --- | --- | --- |
| `model` | `Plan` (lecture du plan **et** calcul des tournées), `ProgrammeLivraisons` (+ `Demande`, tournées des livreurs), `Tournee` (+ `Arret`, `TypeArret`), `Noeud`, `Troncon`, `Chemin`, `Observable`, `Observateur` | `algo.tsp`, JDK |
| `algo.tsp` | Voyageur de commerce : `TemplateTSP`, `TSP3`, `GrapheDepandance` | JDK |
| `controller` | `MainController`, `MapScreen`, états `Etat*`, commandes `Commande*`, `ListeDeCommandes` | `model` |
| `view` | `MainView`, `MapCanvas`, `MapProjection`, `LibellesDemandes`, `MainApp`, `AppLauncher` | `controller`, `model`, JavaFX |

Règles :

- `model` n'importe jamais `controller`, `view` ni JavaFX.
- `view` ne lit aucun fichier et ne modifie jamais le modèle : elle transmet
  les actions au contrôleur et observe le modèle pour se redessiner.
- `controller` ne connaît la vue que par l'interface `MapScreen`. Toute
  nouvelle interaction avec l'écran = une méthode de `MapScreen`, implémentée
  dans `MainView` et dans les faux écrans des tests.
- Le calcul des tournées est `Plan.calculerTournees(programme)`, une tournée
  par livreur (ADR 0006). Ses étapes (A*, graphe des plus courts chemins,
  TSP, heures de passage) sont des méthodes **privées** de `Plan` : ne pas
  recréer de classes `AEtoile`, `CalculHoraires`, `GrapheChemins` ou
  équivalentes.
- Les tournées sont une donnée du modèle, rangées dans `ProgrammeLivraisons`
  et observées par la vue ; une commande qui modifie les demandes garde les
  tournées d'avant et d'après (ADR 0006).
- Le calcul long tourne hors du thread JavaFX ; le contrôleur reçoit le calcul
  (`BiFunction`) et deux `Executor` pour rester testable.
- Lancer l'interface via `AppLauncher`, jamais `MainApp`.

## Design patterns : les utiliser, ne pas les contourner

- **State** (`controller/Etat`, ADR 0002) : `MainController` délègue chaque
  action à son état courant. Nouvelle action = une méthode par défaut dans
  `Etat` (qui ignore l'action), redéfinie dans les états où elle est permise ;
  les boutons actifs viennent de `Etat.actionsPermises`. Jamais de booléen
  d'état ni de `if (plan != null …)` dans le contrôleur. Chaque état dit à
  l'utilisateur quoi faire par `Etat.consigne` (obligatoire, en français),
  que `changerEtat` affiche à l'entrée : ne jamais poser la consigne d'un
  état depuis l'extérieur, seuls les refus et avertissements passent par
  `afficherConsigne`.
- **Observer** (`model/Observable`, ADR 0003) : toute méthode qui modifie un
  objet observable appelle `notifierObservateurs()`. La vue s'inscrit sur le
  modèle affiché et se désinscrit de l'ancien.
- **Command** (`controller/Commande`, ADR 0004) : toute modification du
  programme faite par l'utilisateur est une `Commande` réversible exécutée par
  `MainController.executer(...)`, pour qu'Annuler et Rétablir marchent. Ne
  jamais modifier le modèle directement depuis un état ou la vue. Une
  commande dit ce qu'elle a fait (`confirmation`, affichée après l'action) et
  se nomme (`description`, pour « Annulé : … » et « Rétabli : … »).

## Modifier une tournée déjà calculée (cas 4)

Règle du 26/10 : après le calcul, le voyageur de commerce n'est plus relancé.
Toute modification du programme de livraisons garde l'ordre des autres arrêts
et recalcule seulement les trajets touchés (A*) puis les heures de passage
(*recalcul local*, voir le [README](README.md#architecture)). Chaque
modification est une `Commande` annulable. Le ticket parent est TIG-42 : l'état
et le responsable de chaque sous-issue se lisent dans Linear, pas ici.

Pour brancher une nouvelle modification :

1. **Calcul dans `Plan`** : une méthode publique, sur la tournée **d'un
   livreur**, qui vérifie les règles, puis s'appuie sur le recalcul local
   existant (voir `calculerTourneeSansDemande`, `calculerTourneeOrdreChange`).
   Chaque `Arret` connaît sa demande et son type : on retrouve un arrêt par
   `arret.demande()`, jamais par son nœud. Pas de classe de calcul à part (voir
   *Architecture*).
2. **Règles avant le recalcul** (contrainte d'ordre, arrêt qui ne peut pas
   bouger) : lever `IllegalArgumentException` (demande refusée) ou
   `IllegalStateException` (adresse inaccessible) avec un message lisible par
   l'utilisateur. Un retour après 13 h n'est **pas** une exception : la méthode
   renvoie la tournée, `Tournee.estPossible()` est faux ; c'est au contrôleur de
   refuser, si la règle du ticket le demande.
3. **Une commande** (`Commande`) : elle garde les tournées d'avant et d'après
   (`Map<Integer, Tournee>`, une par livreur) et les range dans
   `ProgrammeLivraisons` avec `definirTournees`, que la vue observe. Si elle
   modifie les demandes (`CommandeAjouterDemande`,
   `CommandeSupprimerDemande`), elle les change aussi. Le calcul se fait
   **avant** la commande, et les tournées des autres livreurs sont reprises
   telles quelles ; annuler et rétablir ne recalculent rien. Elle implémente
   `memoriserTourneesAvant` et `memoriserTourneesApres`, pour qu'un « Calculer »
   fait après coup ne soit pas écrasé.
4. **Une action dans `Etat`** (méthode par défaut qui ignore l'action,
   redéfinie dans l'état qui la permet) et ses boutons dans `MapScreen`
   (implémentés dans `MainView` et dans le faux écran des tests). Une action
   sur un arrêt porte le **livreur** et le rang dans sa tournée.
5. **Un refus** s'affiche par l'écran et n'exécute pas la commande : les
   tournées et l'historique restent intacts.
6. **Un calcul long** tourne hors du thread JavaFX (voir *Architecture*).
7. **La consigne** « ordre non optimisé » s'affiche après une modification qui
   garde un ordre (suppression, changement d'ordre) ; elle nomme le livreur
   s'il y en a plusieurs.
8. Tests d'abord ; README et diagrammes dans la même PR.

Les tournées sont une donnée du modèle (une par livreur) ; il n'y a plus de
cache dans le contrôleur ([ADR 0006](docs/adr/0006-plusieurs-livreurs.md)). Le
voyageur de commerce n'est lancé que par « Calculer » ; `Plan` y garde en
mémoire l'ordre déjà trouvé, ce que ne consulte jamais une modification.

## Glossaire : nommer avec les mots du projet

Le vocabulaire est fixé par le document Linear *AGILE Glossaire du projet*,
repris dans la section [Glossaire du README](README.md#glossaire) avec la
correspondance dans le code. Règles :

- Nommer classes, méthodes, variables, textes d'interface et diagrammes avec
  ces termes : **plan, nœud, tronçon, adresse, entrepôt, programme de
  livraisons, demande, point de collecte, point de livraison, durée sur
  place, tournée, arrêt, heure de passage, contrainte d'ordre**.
- « Livraison » désigne le dépôt d'un colis, pas l'ensemble des demandes
  (c'est le *programme de livraisons*, `ProgrammeLivraisons`).
- Pas de synonymes anglais ou approximatifs dans le code métier : pas de
  `pickup`, `delivery`, `request`, `tour`, `stop`, `lot`, `requête`.
- Un concept absent du glossaire : le signaler à l'humain pour qu'il soit
  ajouté au glossaire, ne pas inventer un terme.

## Conventions de code

- **Tout identifiant en français** : variables, champs, paramètres,
  constantes, méthodes, méthodes de test, classes de test
  (`boutonCalculer`, `dernierDossier`, `LARGEUR`, `EcranFactice`), pas
  `calculateButton`, `lastDirectory`, `WIDTH`, `FakeScreen`.
- Exceptions admises, le vocabulaire de code usuel :
  - noms imposés par Java ou JavaFX (`main`, `start`, `stage`, `scene`,
    `layoutChildren`, `hasNext`, `toString`…) ;
  - variables courtes conventionnelles (`i`, `j`, `k`, `x`, `y`, `dx`, `e`,
    `args`) et suffixes techniques (`Test`, `Exception`) ;
  - termes techniques sans équivalent courant (`record`, `cache`, `XML`).
- Les classes historiques `MainController`, `MainView`, `MainApp`,
  `MapScreen`, `MapCanvas`, `MapProjection` et `AppLauncher` gardent leur nom
  pour l'instant ; toute **nouvelle** classe est nommée en français.
- Données immuables = `record` ; leurs accesseurs portent le nom du champ
  (`noeud.latitude()`). Méthodes des classes : `obtenirX()`, jamais `getX()`.
- Minimum de getters et de setters : n'exposer que ce qu'un appelant utilise
  vraiment. Minimum de classes : pas de classe pour une seule méthode, pas
  d'abstraction « au cas où » (les patterns ci-dessus sont l'exception).
- Renvoyer des collections non modifiables ; champs `final` dès que possible.
- Javadoc en français sur toute classe et méthode publique.
- 4 espaces, UTF-8, LF, lignes de 80 caractères si possible (`.editorconfig`,
  `config/checkstyle.xml`). Suivre le style du code autour.
- Lecture XML : passer par `Plan.creerLecteurXml()` (DOCTYPE interdit). Une
  erreur de fichier devient une `SAXException` au message lisible par
  l'utilisateur ; une adresse inaccessible ou un calcul sans solution, une
  `IllegalStateException` ; un retour après 13 h, une tournée renvoyée avec
  `Tournee.estPossible()` faux.
- `docs/code-fourni/` est en lecture seule. `algo/tsp` ne sera nettoyé (garder
  seulement `TSP3`) qu'à la fin du projet : ne pas le faire sans demande.

## Tests

- JUnit 4 ; `Foo` est testé par `FooTest`, dans le même package sous
  `src/test/java` (ADR 0001). Pas de suite de tests.
- Méthodes de test aux noms descriptifs en français
  (`retourApres13hSignale()`), avec une ligne de Javadoc.
- Tests d'abord pour tout calcul (exigence du cours).
- Données : fichiers fournis via `getResourceAsStream("/data/...")` ; petits
  XML écrits dans le test (voir `PlansPourTests`).
- Contrôleur : faux `MapScreen` + `Executor` synchrones + calcul factice,
  jamais de fenêtre JavaFX.
- Toute correction de bug arrive avec le test qui l'aurait détectée.

## Documentation à tenir à jour

- Diagrammes : modifier la source `docs/diagrams/*.puml` concernée dans la
  même PR que le code (et les blocs Mermaid de `docs/diagrams/README.md` s'ils
  sont touchés). Diagrammes non séquentiels : `!pragma layout smetana`.
- Les SVG ne sont **jamais** committés (`docs/diagrams/out/` est ignoré) : la
  CI les génère et GitHub Pages les publie.
- Décision structurante (nouveau pattern, nouvelle dépendance, changement
  d'architecture) : nouvel ADR à partir de `docs/adr/modele.md`.
- Changement visible par l'utilisateur ou dans la façon de travailler :
  mettre à jour le README (et ce fichier si une règle change).

## Git et pull requests

- Ne jamais pousser sur `main`. Une branche par issue, au nom proposé par
  Linear : `tig-XX-titre-court` ; outillage sans issue : `chore/titre`.
  Tout autre nom fait échouer la CI.
- Commits et titres de PR : `type: description` (`feat`, `fix`, `refactor`,
  `test`, `docs`, `ci`, `chore`, `build`, `perf`, `style`) ou contenant
  `TIG-XX`. Remplir le [modèle de PR](.github/pull_request_template.md).
- **Aucune mention d'IA** dans les commits, PR, commentaires de revue ou
  fichiers : pas de ligne `Co-Authored-By: Claude …` (ni d'un autre
  assistant), pas de « Generated with Claude Code », pas d'emoji robot.
  Le travail est signé par la personne qui le soumet.
- Une PR = un sujet. Ne pas mélanger fonctionnalité et refactor.
- Merge en *squash* après approbation humaine et check **CI OK** vert.
- Ne pas réécrire l'historique d'une branche partagée sans accord ;
  `git push --force-with-lease` uniquement sur ses propres branches.

## Interdits

- Modifier `docs/code-fourni/`.
- Committer des SVG de diagrammes, `target/`, des réglages locaux
  (`JAVA_HOME`, chemins absolus).
- Désactiver ou supprimer un test, baisser `jacoco.line.minimum`, ajouter une
  exclusion SpotBugs ou Checkstyle pour faire passer la CI. Une exclusion
  SpotBugs n'est acceptable que ciblée (une classe, un type de bug) et
  justifiée en commentaire dans `config/spotbugs-exclude.xml`.
- Contourner State, Observer ou Command (voir plus haut).
- Introduire un terme métier hors glossaire.
