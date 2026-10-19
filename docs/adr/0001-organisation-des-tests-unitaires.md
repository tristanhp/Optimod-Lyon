# 0001. Organisation des tests unitaires

- **Statut** : Acceptée
- **Date** : 2026-10-09

## Contexte

Le projet a des tests JUnit 4 dans `src/test/java`, exécutés par le plugin
Maven Surefire (`mvn test`, `mvn clean verify`) en local comme en CI. Il n'y
avait pour l'instant qu'une seule classe de test, `MainTest`, et il fallait
fixer comment ranger les tests des classes à venir (tronçons, plan, lecture
XML, algorithmes...) et comment les lancer tous.

## Décision

1. **Une classe de test par classe testée** : la classe `Foo` est testée par
   `FooTest`.
2. **Même arborescence que `src/main/java`** : le test est dans le même package
   que la classe testée.

   ```text
   src/main/java/fr/insa/optimod/model/Troncon.java
   src/test/java/fr/insa/optimod/model/TronconTest.java
   ```

3. **Pas de classe qui appelle tous les tests** (suite JUnit) : Surefire
   détecte et lance automatiquement toutes les classes `*Test` de
   `src/test/java`. La CI lance donc tous les tests via `mvn clean verify`,
   sans configuration supplémentaire.
4. **Fichiers de données de test dans `src/test/resources`** (par exemple
   `src/test/resources/xml/petitPlan.xml`), chargés avec
   `getClass().getResourceAsStream("/xml/petitPlan.xml")`. Ils ne sont pas
   inclus dans le jar livré.
5. **Méthodes de test aux noms descriptifs**, documentées par une ligne de
   Javadoc (ex. `longueurNegativeRefusee()`).

## Alternatives envisagées

- **Une suite `MainTest` qui appelle toutes les classes de test**
  (`@RunWith(Suite.class)`) : une classe oubliée dans la suite n'est plus
  testée sans que personne ne s'en rende compte, Surefire lance les tests deux
  fois (directement et via la suite), et le fichier crée des conflits de merge
  à chaque PR. `MainTest` reste simplement le test de la classe `Main`.
- **Tous les tests à plat dans un seul dossier** : un test dans un autre
  package que la classe testée n'accède qu'aux méthodes `public`, ce qui pousse
  à tout rendre public pour tester. Le package par défaut (fichiers
  directement dans `src/test/java`) est déconseillé en Java et signalé par
  Checkstyle. Un dossier plat devient aussi peu lisible avec 20 à 30 classes de
  test.

## Conséquences

- Ajouter un test se résume à créer `FooTest` au bon endroit : il est lancé
  automatiquement en local et en CI, et compte dans la couverture JaCoCo.
- Les IDE (VS Code, IntelliJ) passent directement d'une classe à son test, et
  les rapports Surefire et JaCoCo sont groupés par package.
- Les méthodes package-private peuvent être testées sans les rendre publiques.
- Lancer une seule classe de test : `mvn test -Dtest=TronconTest`.
