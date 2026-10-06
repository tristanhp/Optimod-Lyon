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
| Lancer l'application | `java -jar target/optimod-lyon-0.1.0-SNAPSHOT.jar` |

- Rapport de couverture HTML : `target/site/jacoco/index.html`
- Rapport de couverture XML : `target/site/jacoco/jacoco.xml` (lu par Coverage
  Gutters : commande *Coverage Gutters: Display Coverage* dans VS Code)
- Javadoc : `target/reports/apidocs/index.html`

Les mêmes actions sont disponibles dans VS Code via *Terminal > Run Task*
(`.vscode/tasks.json`).

## Structure du projet

```
optimod-lyon/
├── docs/                      diagrammes et livrables
├── src/
│   ├── main/
│   │   ├── java/fr/insa/optimod/
│   │   │   ├── Main.java      point d'entrée
│   │   │   ├── model/         plan, intersections, tronçons, demandes, tournées
│   │   │   ├── controller/    coordination vue / modèle / algorithmes
│   │   │   ├── view/          interface utilisateur et carte
│   │   │   ├── algo/          graphe des plus courts chemins, TSP
│   │   │   └── xml/           lecture des fichiers XML
│   │   └── resources/data/    XML d'exemple
│   └── test/java/fr/insa/optimod/   tests JUnit 4 (même arborescence que main)
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
- Tests unitaires avec JUnit 4, dans `src/test/java`, dans le même package que
  la classe testée. Une classe `Foo` est testée par `FooTest`.

## Workflow git

- `main` est la branche stable : on ne pousse pas directement dessus.
- Une branche par issue Linear, nommée `tig-XX-titre`
  (ex. `tig-12-parsing-plan-xml`), créée depuis `main` à jour.
- Commits courts et explicites (ex. `feat: parse le plan de ville`).
- Ouvrir une *pull request* vers `main` ; au moins une personne de l'équipe la
  relit avant le merge.
- Avant de proposer une PR : `mvn clean verify` doit passer.
- Une fois mergée, la branche est supprimée.

## Équipe

<!-- À remplir : nom, prénom, rôle -->

| Nom | Prénom | Rôle |
| --- | --- | --- |
| | | |
