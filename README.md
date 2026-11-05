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
| Plusieurs livreurs : une tournée par livreur (modèle et calcul) | fait |
| Changer l'ordre des arrêts d'une tournée (Monter / Descendre) | fait |
| Saisir les durées et insérer une demande sans relancer le TSP | fait |
| Afficher les plages annoncées (± 1 h) et signaler leurs dépassements après ajout | fait |
| Déplacer un point de collecte ou de livraison | fait |
| Changer le livreur d'une demande (annulable) | fait |
| Régler le nombre de livreurs (annulable) | fait |
| Générer les feuilles de route (un fichier HTML par livreur, imprimable) | fait |
| Sauvegarder et restaurer la tournée d'un livreur (XML) | fait (un livreur) |

## Installer le projet

### Prérequis

- **JDK 27** (fixé dans [`.sdkmanrc`](.sdkmanrc) ; ce n'est pas une version
  LTS) et **Maven 3.9+**.
- **Sous Windows :** le workflow de l'équipe utilise WSL (Ubuntu conseillé),
  avec l'extension *WSL* de VS Code. Une installation native avec PowerShell
  est aussi décrite ci-dessous, notamment pour Windows 10 sans WSLg.
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

Sur un nouveau PC Windows, Java 11 ne suffit pas. Pour travailler directement
dans PowerShell (notamment sans WSLg sous Windows 10) :

1. Installer le **JDK 27**, puis **Apache Maven 3.9+** depuis leurs sites
   officiels : [Java](https://www.oracle.com/java/technologies/downloads/),
   [Maven](https://maven.apache.org/download.cgi).
2. Dans les variables d'environnement utilisateur, définir `JAVA_HOME` vers
   le dossier du JDK 27 (sans `bin`) et ajouter `%JAVA_HOME%\bin` et le dossier
   `bin` de Maven au `Path`. Placer Java 27 avant les anciens Java.
   Ces chemins dépendent du PC et ne doivent pas être committés.
3. Fermer puis rouvrir VS Code. Installer les extensions recommandées via
   l'onglet Extensions, filtre `@recommended`.
4. Ouvrir le dossier contenant `pom.xml`, puis exécuter `Java: Configure
   Java Runtime` dans la palette de commandes pour vérifier le JDK du projet.
5. Dans un **nouveau terminal PowerShell**, vérifier `java -version`,
   `javac -version` (27) et `mvn -version` (Maven 3.9+ utilisant Java 27).
   Lancer ensuite `mvn clean verify`, puis `mvn javafx:run`.

JavaFX est téléchargé par Maven ; aucun SDK JavaFX séparé n'est nécessaire.

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

Maven télécharge JavaFX au premier lancement. Le lancement utilise le
classpath pour rendre accessibles les bibliothèques natives du système,
même si le projet embarque plusieurs variantes de JavaFX. Sous Windows,
la fenêtre s'affiche directement avec Java natif ; sous WSL, elle nécessite
WSLg ou un serveur d'affichage. Autres façons de lancer :

- VS Code : *Exécuter et déboguer*, configuration **Interface JavaFX** ;
- archive exécutable : `mvn clean package` puis
  `java -jar target/optimod-lyon-0.1.0-SNAPSHOT.jar` (avec `--version`, le jar
  affiche seulement son nom). Le jar embarque JavaFX pour Linux, Windows et
  macOS Apple Silicon.

Ne pas lancer `MainApp` directement : JavaFX exige de passer par
`AppLauncher`.

### Utiliser

**Guidage** : sous la barre d'outils, une bande bleue dit à chaque étape ce
qu'il faut faire (importer un plan, importer les demandes, calculer, modifier
la tournée…) ; elle change toute seule quand on passe à l'étape suivante.
En bas de la fenêtre, une phrase en vert confirme ce qui vient d'être fait
(« Demande ajoutée au livreur 2. », « Annulé : suppression d'une demande du
livreur 1. »). Survoler un bouton affiche une infobulle ; un bouton grisé n'en
affiche pas, c'est la consigne qui dit alors quoi faire d'abord.

**Feuilles de route** : une fois la tournée calculée, *Générer les feuilles de
route* demande un dossier et y écrit un fichier `feuille-de-route-livreur-N.html`
par livreur : heures de départ et de retour, arrêts dans l'ordre (type, adresse,
plage annoncée, heures, durée sur place) et itinéraire rue par rue entre deux
arrêts. La page s'imprime depuis le navigateur.

**Sauvegarde et restauration (modèle actuel : un livreur)** : après un calcul,
cliquer sur *Sauvegarder* et choisir un fichier XML. *Restaurer*, disponible
dès l'ouverture et hors opération en cours, recharge le plan référencé et le
programme, puis recalcule les trajets et les heures sans changer l'ordre des
arrêts et sans relancer le TSP. La restauration réussie vide Annuler/Rétablir.
Un fichier invalide laisse le travail courant intact. Garder le fichier du
plan à son emplacement ; il n'est pas inclus dans la sauvegarde.
Le [format XML](docs/sauvegarde-tournee.md) décrit les limites de cette version.
Avec plusieurs livreurs (champ *Livreurs*), *Sauvegarder* affiche un message et
n'écrit rien : seule la tournée d'un livreur est gérée pour l'instant.

Les fichiers fournis sont dans `src/main/resources/data/` : trois plans
(`petitPlan.xml`, `moyenPlan.xml`, `grandPlan.xml`) et des fichiers de
demandes (`demandePetit1.xml` … `demandeGrand9.xml`), chacun prévu pour le plan
de même taille.

Pour tester les arrêts rouges après 13 h, importer `petitPlan.xml`, puis
`demande_trop_importante.xml` et calculer avec un seul livreur : les douze
heures sur place imposent un retour après 20 h.

1. **Importer le plan** : choisir un fichier de plan. La carte s'affiche
   (tronçons en gris, nœuds en points sombres). Molette pour zoomer,
   glisser pour se déplacer, double-clic pour revenir à la vue d'ensemble.
2. **Importer les demandes** : choisir un fichier de demandes. L'entrepôt
   (losange), les points de collecte (carrés) et de livraison (ronds)
   apparaissent, une couleur par demande, et la liste des demandes s'affiche
   à gauche. Survoler un point met sa demande en évidence.
3. **Calculer** : l'application calcule la meilleure tournée de chaque
   livreur et affiche à droite les arrêts avec leurs heures d'arrivée et de
   départ (un titre par livreur s'il y en a plusieurs). Si une adresse est
   inaccessible, un message l'explique ; si un livreur revient après 13 h,
   sa tournée est affichée quand même, sans message d'erreur. Les arrêts dont
   l'arrivée dépasse 13 h sont rouges dans la liste et le popup, avec la
   mention « Après 13 h » sur sa propre ligne, même s'ils respectent leur
   plage annoncée ; sur la carte, leur numéro est sur fond rouge.
4. **Modifier le programme** :
   - *Ajouter une demande* : cliquer sur le bouton, puis sur le point de
     collecte, puis sur le point de livraison ; saisir les deux durées
     entières strictement positives en minutes (5 par défaut) et choisir le
     livreur ;
     le carré de collecte apparaît dès le premier clic, puis le rond de
     livraison au second, avant validation du dialogue. Recliquer sur le
     bouton, utiliser *Annuler* (Ctrl+Z) avant le second clic ou annuler un
     dialogue efface ces points provisoires sans modifier le programme ;
   - *Supprimer la demande* : sélectionner une demande dans la liste, puis
     cliquer sur le bouton sous la liste. Si la tournée est affichée, elle est
     mise à jour tout de suite : l'ordre des autres arrêts est gardé et seuls
     les trajets et les heures sont recalculés (pas de nouveau calcul de 10 s).
     Une consigne prévient que cet ordre n'est peut-être plus le plus court
     (elle nomme le livreur s'il y en a plusieurs) ;
   - *Monter* / *Descendre* : sélectionner un arrêt dans la liste de la
     tournée (à droite), puis cliquer sur le bouton sous la liste. L'arrêt
     échange sa place avec son voisin et les heures de passage sont recalculées
     tout de suite ; seule la tournée de son livreur change. L'arrêt reste
     sélectionné, on peut donc le déplacer de plusieurs rangs en recliquant.
     L'entrepôt (départ et retour) ne se déplace pas. Si le point de livraison
     d'une demande passerait avant son point de collecte (contrainte d'ordre),
     ou si le changement ferait dépasser 13 h à une tournée qui rentrait à
     l'heure, une fenêtre explique le refus et rien ne change ;
   - *Déplacer* : sélectionner un point de collecte ou de livraison dans la
     liste des tournées, cliquer sur *Déplacer*, puis sur sa nouvelle adresse
     sur la carte (un second clic sur *Déplacer* annule). L'ordre des arrêts ne
     change pas : seuls les deux trajets autour du point sont recalculés, et
     seule la tournée de son livreur change. Si la tournée devient impossible
     (adresse inaccessible, ou retour après 13 h pour une tournée qui rentrait
     à l'heure), une fenêtre explique le refus et rien ne change ;
   - *Changer de livreur* : sélectionner une demande dans la liste de gauche,
     cliquer sur le bouton sous la liste, choisir le nouveau livreur, puis
     l'endroit de sa tournée où insérer la demande (voir plus bas). La demande
     quitte la tournée de son ancien livreur, qui garde l'ordre de ses autres
     arrêts, et entre dans celle du nouveau, sans relancer le TSP ; les autres
     tournées ne changent pas. Si le nouveau livreur, qui rentrait à l'heure,
     rentrerait après 13 h, ou si une adresse est inaccessible, une fenêtre
     explique le refus et rien ne change. Le bouton n'est actif que s'il y a
     plusieurs livreurs (voir le champ *Livreurs*) ;
   - *Livreurs* (champ de la barre d'outils, flèches haut et bas, de 1 à 20) :
     règle le nombre de livreurs de la journée, avant ou après le calcul. Un
     livreur ajouté a une tournée vide, prête à recevoir des demandes (*Ajouter
     une demande*, *Changer de livreur*) ; les autres tournées ne changent pas
     et rien n'est recalculé. On ne retire que le dernier livreur, et seulement
     s'il n'a aucune demande : sinon une consigne explique le refus, le champ
     reprend l'ancienne valeur et rien ne change. Un message confirme le
     réglage, et son annulation. Le réglage s'annule et se rétablit comme les
     autres modifications ; un nouvel import de demandes revient à un seul
     livreur ;
   - *Annuler* (Ctrl+Z) et *Rétablir* (Ctrl+Y) : toute modification peut être
     défaite ; si la tournée correspondante a déjà été calculée, elle
     réapparaît sans nouveau calcul.

Après les clics sur la carte et la saisie des durées, choisir entre quels
arrêts insérer la demande. La collecte puis la livraison sont insérées
consécutivement à cette position, sans modifier l'ordre des anciens arrêts
et sans relancer le TSP. Seuls les trois trajets reliant les voisins à la
nouvelle demande sont recalculés. Annuler ce choix ne modifie rien.
La première entrée du dialogue, *Moindre coût*, laisse l'application proposer
la position qui fait rentrer le livreur le plus tôt (collecte et livraison
côte à côte) : l'administrateur garde la main, le calcul n'a lieu que s'il
choisit cette entrée, et Annuler le défait. Le même dialogue sert à l'ajout
d'une demande et au changement de livreur.
Si l'insertion fait rentrer le livreur après 13 h, l'application cherche les
livreurs qui peuvent prendre la demande avant 13 h (en essayant la position
*Moindre coût* chez chacun, sans rien insérer) et les propose, le livreur
d'origine compris : une autre position chez lui peut suffire. L'administrateur
choisit un livreur, puis la position dans sa tournée ; un nouveau refus rouvre
la proposition. S'il annule, ou si aucun livreur ne peut la prendre, l'ajout est
refusé avec un message et le programme n'est pas modifié. Une tournée qui rentre
déjà après 13 h accepte une demande de plus, sans proposition.
Le calcul global fixe une plage annoncée de ± 1 heure autour de l'arrivée.
L'insertion conserve les plages des anciens arrêts ; la liste signale les
dépassements en rouge et affiche le décalage en minutes et secondes.
Annuler reste disponible. Une fois les tournées calculées, le bouton *Calculer*
devient *Recalculer* : il n'est jamais lancé automatiquement et demande une
confirmation, car les modifications manuelles et les plages annoncées seront
perdues. *Oui* relance le TSP pour chaque livreur et fixe de nouvelles plages ;
*Non* ne change rien. Annuler n'annule pas ce recalcul : il défait la dernière
modification du programme. La suppression retire encore la tournée jusqu'au
prochain calcul volontaire.

Une fois la tournée calculée, elle est tracée sur la carte : le trait est clair
au départ de l'entrepôt et foncé au retour, une flèche blanche au milieu de
chaque tronçon indique le sens, et les arrêts sont numérotés 1, 2, 3… dans
l'ordre de passage. Cliquer sur un arrêt (ou le choisir dans la liste de
droite, les deux sont synchronisés) ouvre un popup avec son type (entrepôt,
point de collecte, point de livraison), son adresse (noms des rues), ses heures
d'arrivée et de départ et sa plage annoncée. Si le dessin échoue (plan sans
nœud ni tronçon, adresse de la tournée absente du plan, exception), un message
remplace la carte et l'application revient à l'état initial.

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
| Livreur | Cycliste qui fait une tournée (un seul par défaut), numéroté à partir de 1 | `Demande.livreur()`, `ProgrammeLivraisons.obtenirNombreLivreurs()` |
| Demande de livraison | Un colis à récupérer puis à déposer | `ProgrammeLivraisons.Demande` |
| Programme de livraisons | Toutes les demandes à traiter, lues dans un fichier ou saisies | `ProgrammeLivraisons` |
| Point de collecte | Où l'on récupère le colis (carré sur la carte) | `Demande.collecte()` |
| Point de livraison | Où l'on dépose le colis (rond sur la carte) | `Demande.livraison()` |
| Collecte / Livraison | Récupération / dépôt du colis par le livreur | — |
| Durée sur place | Temps passé à récupérer ou déposer, en secondes | `dureeCollecte`, `dureeLivraison` |
| Tournée | Trajet d'un livreur : départ à 8 h, retour avant 13 h ; une par livreur | `Tournee`, `ProgrammeLivraisons.obtenirTournees()` |
| Arrêt, heure de passage | Un point de la tournée (entrepôt, collecte ou livraison) et ses heures d'arrivée et de départ | `Tournee.Arret` |
| Feuille de route | Page HTML imprimable d'un livreur : arrêts dans l'ordre, heures de passage et itinéraire rue par rue | `FeuilleDeRoute` |
| Plage horaire annoncée | Heure d'arrivée ± 1 h, fixée au calcul et conservée après insertion | `Arret.debutPlage()`, `Arret.finPlage()` |
| Meilleure tournée | Celle qui revient à l'entrepôt le plus tôt | `Plan.calculerTournees` |
| Tournée possible | Tournée qui respecte toutes les règles, dont le retour avant 13 h | `Tournee.estPossible()` |
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
  annulable, qui garde les tournées d'avant et d'après
  ([ADR 0004](docs/adr/0004-design-pattern-command-annulation.md),
  [ADR 0006](docs/adr/0006-plusieurs-livreurs.md)).

Le calcul des tournées vit dans `Plan.calculerTournees`, une tournée par
livreur : A* entre chaque couple d'adresses, ordre de passage par séparation
et évaluation (`TSP3`), puis heures de passage
([ADR 0005](docs/adr/0005-nettoyage-du-code-et-glossaire.md),
[ADR 0006](docs/adr/0006-plusieurs-livreurs.md)). Les tournées sont rangées
dans le `ProgrammeLivraisons`, que la vue observe.

Le voyageur de commerce ne sert qu'au bouton *Calculer*. Quand on modifie une
tournée déjà calculée, on fait un **recalcul local** : l'ordre des autres
arrêts est gardé, les trajets des jonctions créées (A*) sont les seuls
recalculés, les autres trajets sont repris de la tournée actuelle, et les
heures de passage sont refaites. La suppression d'une demande passe par
`Plan.calculerTourneeSansDemande` et le changement d'ordre d'un arrêt par
`Plan.calculerTourneeOrdreChange`. « Recalcul local » est un terme technique,
pas du vocabulaire métier.

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
