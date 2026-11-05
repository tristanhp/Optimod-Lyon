# Conception d'Optimod'Lyon

Cette description porte sur le dépôt livré : une tournée par livreur,
rangée dans le modèle (voir
[ADR 0006](adr/0006-plusieurs-livreurs.md)).

## Architecture MVC

Le modèle contient le plan, les demandes et les tournées immuables. Il lit
et écrit les fichiers XML et calcule les chemins, l'ordre et les horaires.
Il dépend du JDK et de `algo.tsp`, jamais de JavaFX ou du contrôleur.
`Plan.creerLecteurXml()` interdit les DOCTYPE pour toutes les lectures.

Le contrôleur ne connaît l'écran que par `MapScreen`. Les états décident
des actions permises. La vue transmet les actions et observe le programme ;
elle ne lit aucun fichier et ne modifie pas les demandes. Les calculs longs
utilisent un exécuteur en arrière-plan, puis un exécuteur de l'interface.
Les tests remplacent l'écran et ces exécuteurs sans ouvrir JavaFX.

## Patterns et décisions

- **State** : chaque action est déléguée à `Etat`. Le calcul et la
  restauration bloquent les actions concurrentes. Voir
  [ADR 0002](adr/0002-design-pattern-state-controleur.md).
- **Observer** : `ProgrammeLivraisons` notifie après chaque modification.
  La vue retire son inscription au programme précédent. Voir
  [ADR 0003](adr/0003-design-pattern-observer-modele-vue.md).
- **Command** : chaque modification du programme est réversible, avec
  historique. Une commande garde les tournées d'avant et d'après, rangées dans
  `ProgrammeLivraisons`, pour qu'Annuler et Rétablir ne recalculent rien. Voir [ADR 0004](adr/0004-design-pattern-command-annulation.md).
- **Template Method** : `TemplateTSP` fixe la recherche par séparation et
  évaluation ; `TSP3` spécialise l'exploration et sa borne. Le graphe impose
  la collecte avant la livraison. L'intégration du calcul dans `Plan` est
  décrite par [ADR 0005](adr/0005-nettoyage-du-code-et-glossaire.md).

Les ADR 0001 à 0006 sont présents ; l'ADR 0006 décrit le modèle à plusieurs
livreurs et le rangement des tournées dans `ProgrammeLivraisons`.

## Algorithmes

### Plus courts chemins

`A*` parcourt les tronçons orientés. Une file de priorité ordonne les nœuds
par distance déjà parcourue plus estimation restante. L'heuristique est la
distance haversine multipliée par 0,9 pour tenir compte des arrondis des
données fournies ; cette garantie suppose des longueurs cohérentes avec
ces données. Les prédécesseurs reconstruisent la liste des tronçons.
Une adresse inaccessible produit un chemin inaccessible, puis une erreur
de tournée lisible si elle doit être desservie.

### Calcul global

`Plan.calculerTournees` construit, pour chaque livreur, le graphe des plus
courts chemins entre les adresses, cherche l'ordre avec `TSP3`, puis calcule les heures.
La limite de recherche est de 10 secondes : la meilleure solution trouvée
est utilisée ; elle n'est pas déclarée optimale si la recherche est coupée.
Les distances transmises au TSP sont arrondies au mètre. La vitesse est
15 km/h et chaque trajet est arrondi à la seconde. Les durées sur place
s'ajoutent aux trajets. Le retour doit être au plus tard à 13 h : sinon la
tournée est affichée quand même et l'utilisateur est prévenu (arrêts en
rouge, consigne), sans qu'elle soit refusée. Le temps total est vérifié pour
refuser un débordement à minuit.
Le benchmark détaillé de TIG-49 reste à fournir par son responsable.

### Recalcul local et insertion

`Plan.recalculerTournee` conserve l'ordre fourni et calcule seulement les
chemins consécutifs et les horaires. Il ne lance pas le TSP. Le lecteur
de sauvegarde vérifie aussi que les arrêts respectent les demandes.

`Plan.insererDemande` reçoit la position choisie dans le dialogue de
l'utilisateur. La collecte puis la livraison sont ajoutées consécutivement
entre deux arrêts existants, sans recherche automatique d'une autre place.
Trois chemins sont calculés par `A*` : arrêt précédent vers collecte,
collecte vers livraison, livraison vers arrêt suivant. Les autres chemins
sont réutilisés. Les horaires sont propagés sans changer les plages des
anciens arrêts. Le décalage est mesuré par rapport au centre de la plage
initialement annoncée et affiché dans la liste ; un dépassement est rouge.
Le calcul n'appelle jamais le TSP. Une position impossible est refusée sans
mutation. Le nombre de recherches A* est constant (trois) ; le calcul des
horaires est linéaire et la matrice interne des chemins occupe O(n²).

### Plages annoncées

Un arrêt mémorise une plage d'arrivée de ± 1 heure, sans arrondi.
Le calcul global la fixe ; l'insertion conserve les bornes des anciens
arrêts par position, même si plusieurs arrêts partagent une adresse.
Les nouveaux arrêts reçoivent leurs plages à leur insertion. Les bornes
sont inclusives ; le modèle sait aussi tester une plage passant minuit.
Un dépassement est signalé dans la liste et par `MapScreen`, sans empêcher
Annuler. Les sauvegardes conservent les bornes annoncées.

## Diagrammes commentés

Les sources sont dans [l'index des diagrammes](diagrams/README.md).
Les SVG sont publiés sur [GitHub Pages](https://tristanhp.github.io/Optimod-Lyon/)
par la CI ; ils ne sont pas versionnés.

### Packages : architecture-mvc.puml

Le diagramme montre les dépendances autorisées entre les quatre packages.
Le modèle porte le calcul et la persistance ; `algo.tsp` reste indépendant.
L'interface `MapScreen` évite une dépendance du contrôleur vers JavaFX.

### Classes : classes.puml

Le plan possède les nœuds et tronçons et produit des tournées immuables.
Les arrêts portent désormais les bornes annoncées et leur détection de dépassement.
La saisie de demande est un record de l'interface d'écran ; les commandes
restent responsables de toute modification du programme.

### États : etats-controleur.puml

L'ajout commence par le choix de collecte puis de livraison et le dialogue.
Sans tournée calculée, la commande ajoute directement au programme.
Avec une tournée, l'insertion passe par l'état de calcul ; son échec revient
à la tournée précédente, et son succès enregistre la commande et le résultat.

### Chargement du plan et du programme

Sources : `sequence-chargement-plan.puml` et
`sequence-chargement-programme.puml`.

Les deux séquences séparent choix de fichier, lecture du modèle et affichage.
Le plan chargé conditionne les adresses permises dans un programme.
Un import de plan échoué réinitialise la page ; un programme invalide
conserve le programme précédent. L'annulation du choix ne change rien.

### Calcul : sequence-calcul-tournee.puml

Le contrôleur affiche l'attente avant de déléguer au modèle en arrière-plan.
Le modèle enchaîne chemins, recherche de l'ordre puis horaires.
Le résultat revient par l'exécuteur de l'interface ; l'état décide ensuite
de l'affichage de la tournée ou de l'explication de l'échec.

### Ajout : sequence-ajout-demande.puml

Les dialogues sont validés avant toute commande. L'insertion respecte la
position choisie sans recherche TSP, pendant que les actions sont bloquées.
Le succès notifie la vue par Observer puis affiche horaires et dépassements ;
l'échec laisse le programme intact et affiche une erreur.

### Cas d'utilisation : use-cases.puml

Le diagramme situe import, calcul et modifications dans le parcours du livreur.
La sauvegarde et la restauration restent présentes dans le code existant.
Leur annulation dans le backlog ne signifie pas leur suppression du dépôt.
Le README distingue les fonctions livrées des évolutions encore attendues.

## Évolution depuis l'itération 1

Le chargement XML et la projection cartographique ont constitué la première
tranche MVC. State a ensuite centralisé les actions permises, Observer a
retiré les rafraîchissements manuels et Command a apporté l'annulation.
Le calcul a été regroupé dans `Plan` pour limiter les abstractions isolées.
La restauration a introduit un recalcul sans TSP réutilisé pour l'insertion.
Les durées deviennent saisissables et les plages annoncées restent stables
lors d'un ajout, afin de conserver les engagements déjà affichés.

Depuis, le modèle gère plusieurs livreurs (une tournée chacun), le changement
de livreur d'une demande est une commande annulable, et la suppression, le
déplacement d'un point et le changement d'ordre passent par le recalcul local,
sans relancer le TSP. La relecture humaine et les
validations après gel du code restent nécessaires pour les tickets finaux.
