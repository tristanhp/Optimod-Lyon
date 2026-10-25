# Livrables UML actualisés

Ces diagrammes décrivent les deux cas d'utilisation implémentés et les noms
français du code. Les blocs Mermaid sont affichables sur GitHub ou dans un
lecteur Markdown compatible. Les fichiers PlantUML servent de sources d'export.

Les noms des classes et packages sont conservés. Les méthodes imposées par Java
ou JavaFX (`main`, `start`, `layoutChildren`, `hasNext`, `next`, `remove`) et les
appels aux bibliothèques externes gardent leur nom. Le code pédagogique original
de `docs/code-fourni` est conservé ; sa copie applicative est renommée.

## Charger et afficher le plan

Source détaillée : [sequence-chargement-plan.puml](sequence-chargement-plan.puml).

```mermaid
sequenceDiagram
    autonumber
    actor U as Utilisateur
    participant V as MainView
    participant C as MainController
    participant P as Plan
    participant M as MapCanvas
    participant R as MapProjection
    Note over V,C: definirActionImportPlan(action) branche le bouton
    U->>V: Cliquer sur Importer le Plan
    V->>C: importerPlan(ecran) via l'action enregistrée
    C->>V: choisirFichierPlan()
    V->>V: choisirFichierXml(titre)
    V-->>C: Optional de fichier
    alt Annulation
        Note over V,C: État précédent conservé
    else Fichier sélectionné
        C->>P: new Plan(cheminFichierXml)
        P->>P: creerLecteurXml().parse(fichier)
        P->>P: Plan(documentXml)
        Note right of P: Créer les Noeud, puis les Troncon<br/>Résoudre les extrémités avec trouverNoeud(identifiant)
        alt Lecture réussie
            P-->>C: Plan construit
            Note right of C: plan = résultat ; livraison = null
            C->>V: afficherPlan(plan)
            V->>M: afficherPlan(plan)
            M->>M: dessinerCarte()
            M->>P: obtenirNoeuds(), obtenirTroncons()
            P-->>M: Collections du plan
            M->>R: new MapProjection(noeuds, largeur, hauteur, margeInterieure)
            loop Coordonnées des nœuds et des tronçons
                M->>R: calculerAbscisseEcran(longitude)
                M->>R: calculerOrdonneeEcran(latitude)
                R-->>M: Coordonnées en pixels
            end
            M-->>U: Carte affichée
            Note right of V: Effacer messageLivraison<br/>Activer boutonImporterLivraison
            Note right of C: etat = EtatPlanCharge
        else Erreur de lecture
            P-->>C: IOException, SAXException ou IllegalArgumentException
            C->>V: afficherErreurXml(erreur.getMessage())
            V-->>U: Alerte XML invalide
            Note right of C: plan = null ; livraison = null
            C->>V: reinitialiser()
            V->>M: effacerCarte()
            Note right of V: Effacer le message et désactiver l'import des livraisons
            Note right of C: etat = EtatInitial
        end
    end
```

## Charger la livraison

Source détaillée :
[sequence-chargement-livraison.puml](sequence-chargement-livraison.puml).

```mermaid
sequenceDiagram
    autonumber
    actor U as Utilisateur
    participant V as MainView
    participant C as MainController
    participant L as Livraison
    participant E as Entrepot
    participant D as Demande
    Note over V,C: Précondition : plan chargé<br/>definirActionImportLivraison(action) branche le bouton
    U->>V: Cliquer sur Importer la Livraison
    V->>C: importerLivraison(ecran) via l'action enregistrée
    Note right of C: Ignoré dans EtatInitial<br/>Le bouton est désactivé dans cet état
    C->>V: choisirFichierLivraison()
    V->>V: choisirFichierXml(titre)
    V-->>C: Optional de fichier
    alt Annulation
        Note over V,C: Plan, livraison et message précédents conservés
    else Fichier sélectionné
        C->>L: new Livraison(cheminFichierXml)
        L->>L: creerLecteurXml().parse(fichier)
        L->>L: Livraison(documentXml)
        Note right of L: Vérifier la racine et l'unique entrepôt
        L->>L: lireAttributObligatoire(depot, heureDepart)
        L->>E: new Entrepot(identifiant, heureDepart)
        loop Balises livraison dans l'ordre XML
            L->>L: lireAttributObligatoire(element, nom)
            Note right of L: Lire les adresses et les durées<br/>Vérifier les nombres et les durées non négatives
            L->>D: new Demande(identifiantCollecte, identifiantLivraison, dureeCollecte, dureeLivraison)
            D-->>L: Demande
        end
        L->>L: List.copyOf(demandesLues)
        alt Lecture réussie
            L-->>C: Livraison construite
            Note right of C: Mémoriser livraison ; conserver plan
            C->>V: afficherLivraisonChargee()
            V->>V: messageLivraison.setText("livraison chargée")
            V-->>U: livraison chargée
        else Erreur de lecture
            L-->>C: IOException, SAXException ou IllegalArgumentException
            C->>V: afficherErreurXml(erreur.getMessage())
            V-->>U: Alerte XML invalide
            Note over V,C: Plan, livraison et message précédents conservés
        end
    end
```

Les méthodes privées du contrôleur sont invoquées par les actions enregistrées
dans son constructeur, pas par un appel direct depuis la vue. Les imports
n'effectuent aucun calcul de tournée et ne dessinent pas les demandes sur la carte.

## États du contrôleur (design pattern State)

Source : [etats-controleur.puml](etats-controleur.puml). `MainController`
délègue chaque clic à son état courant (`Etat`), qui décide si l'action est
permise, quels boutons sont actifs et quel est l'état suivant. Les règles
communes sont des méthodes par défaut de `Etat` ; chaque état ne redéfinit que
ses exceptions (voir l'[ADR 0002](../adr/0002-design-pattern-state-controleur.md)).

```mermaid
stateDiagram-v2
    [*] --> EtatInitial
    EtatInitial --> EtatPlanCharge : importerPlan [valide]
    EtatPlanCharge --> EtatLivraisonChargee : importerLivraison [valide]
    EtatLivraisonChargee --> EtatCalculEnCours : calculer
    EtatCalculEnCours --> EtatTourneeCalculee : finirCalcul [tournée]
    EtatCalculEnCours --> EtatLivraisonChargee : finirCalcul [erreur]
    EtatTourneeCalculee --> EtatCalculEnCours : calculer
    EtatTourneeCalculee --> EtatLivraisonChargee : importerLivraison [valide]
    EtatPlanCharge --> EtatInitial : importerPlan [invalide]
    EtatLivraisonChargee --> EtatPlanCharge : importerPlan [valide]
```

## Observation des demandes (design pattern Observer)

`Livraison` hérite de `Observable` (package `model`, sans JavaFX) et prévient
ses observateurs à chaque `ajouterDemande` / `retirerDemande`. `MainView`
implémente `Observateur` : elle s'inscrit sur les demandes affichées et
redessine la liste et la carte à chaque changement (voir
l'[ADR 0003](../adr/0003-design-pattern-observer-modele-vue.md)).

```mermaid
sequenceDiagram
    participant X as Appelant (contrôleur, commande)
    participant L as Livraison
    participant V as MainView
    participant M as MapCanvas
    Note over V,L: afficherLivraisonChargee(livraison) : V s'inscrit sur L
    X->>L: retirerDemande(position)
    L->>L: notifierObservateurs()
    L->>V: mettreAJour(livraison)
    V->>M: afficherLivraison(livraison)
    V->>V: reconstruit la liste des demandes
```

## Diagramme de classes

Source détaillée : [classes-chargement.puml](classes-chargement.puml).

```mermaid
classDiagram
    class MapScreen {
        <<interface>>
        +definirActionImportPlan(Runnable action)
        +definirActionImportLivraison(Runnable action)
        +choisirFichierPlan() Optional~File~
        +choisirFichierLivraison() Optional~File~
        +afficherPlan(Plan plan)
        +afficherLivraisonChargee(Livraison livraison)
        +afficherErreurXml(String detail)
        +reinitialiser()
        +activerActions(boolean importPlan, boolean importLivraison, boolean calcul)
    }
    class MainController {
        +MainController(MapScreen ecran)
        +obtenirPlan() Plan
        +obtenirLivraison() Livraison
        ~changerEtat(Etat nouvelEtat)
        ~lirePlan() Lecture
        ~lireLivraison() Lecture
        ~lancerCalcul()
    }
    class Etat {
        <<interface>>
        +importerPlan(MainController controleur)
        +importerLivraison(MainController controleur)
        +calculer(MainController controleur)
        +finirCalcul(MainController controleur, List tournee, String erreur)
        +activerActions(MapScreen ecran)
    }
    class AppLauncher {
        +main(String[] arguments)$
    }
    class MainApp {
        +start(Stage fenetre)
    }
    class MainView {
        -Button boutonImporterPlan
        -Button boutonImporterLivraison
        -Label messageLivraison
        -File dernierDossier
        -choisirFichierXml(String titre) Optional~File~
    }
    class MapCanvas {
        ~afficherPlan(Plan nouveauPlan)
        ~effacerCarte()
        #layoutChildren()
        -dessinerCarte()
    }
    class MapProjection {
        -double echelle
        -double correctionLongitude
        -double decalageHorizontal
        -double decalageVertical
        ~calculerAbscisseEcran(double longitude) double
        ~calculerOrdonneeEcran(double latitude) double
    }
    class Plan {
        +Plan(String cheminFichierXml)
        +Plan(InputStream fluxXml)
        -creerLecteurXml() DocumentBuilder$
        -trouverNoeud(String identifiant) Noeud
        +obtenirNoeuds() Collection~Noeud~
        +obtenirNoeud(long identifiant) Noeud
        +obtenirTroncons() List~Troncon~
        +obtenirTronconsSortants(Noeud noeud) List~Troncon~
    }
    class Noeud {
        -long identifiant
        -double longitude
        -double latitude
        +obtenirIdentifiant() long
        +obtenirLongitude() double
        +obtenirLatitude() double
    }
    class Troncon {
        -double longueur
        -String nomRue
        +obtenirNoeudOrigine() Noeud
        +obtenirNoeudDestination() Noeud
        +obtenirLongueur() double
        +obtenirNomRue() String
    }
    class Livraison {
        -String heureDepart
        +Livraison(String cheminFichierXml)
        +Livraison(InputStream fluxXml)
        -creerLecteurXml() DocumentBuilder$
        -lireAttributObligatoire(Element element, String nom) String$
        +obtenirDemandes() List~Demande~
        +obtenirEntrepot() Entrepot
        +obtenirHeureDepart() String
    }
    class Entrepot {
        -long identifiant
        -String heureDepart
        +obtenirIdentifiant() long
        +obtenirHeureDepart() String
    }
    class Demande {
        -long identifiantCollecte
        -long identifiantLivraison
        -int dureeCollecte
        -int dureeLivraison
        +obtenirIdentifiantCollecte() long
        +obtenirIdentifiantLivraison() long
        +obtenirDureeCollecte() int
        +obtenirDureeLivraison() int
    }
    AppLauncher ..> MainApp : lance
    MainApp --> "1" MainController : controleur
    MainApp ..> MainView : crée
    MainView ..|> MapScreen
    MainController ..> MapScreen : actions et affichage
    MainController --> "1" Etat : etat
    MainView ..|> Observateur
    Livraison --|> Observable
    Observable o--> "0..*" Observateur : observateurs
    MainController --> "0..1" Plan : plan
    MainController --> "0..1" Livraison : livraison
    MainView *-- "1" MapCanvas : carte
    MapCanvas --> "0..1" Plan : plan
    MapCanvas ..> MapProjection : crée
    MapProjection ..> Noeud : coordonnées
    Plan *-- "0..*" Noeud : noeuds
    Plan *-- "0..*" Troncon : troncons / tronconsSortants
    Troncon --> "1" Noeud : noeudOrigine
    Troncon --> "1" Noeud : noeudDestination
    Livraison o-- "0..*" Demande : demandes
    Livraison o-- "1" Entrepot : entrepot
```

`MainView` implémente toutes les opérations de `MapScreen`. Les héritages JavaFX
(`MainApp` → `Application`, `MainView` → `BorderPane`, `MapCanvas` → `Pane`) sont
omis pour alléger le diagramme. Les constructeurs simples du modèle sont omis.
Les adresses sont des identifiants longs, pas des associations directes vers
`Noeud`. Les durées des demandes sont en secondes ; les collections exposées sont
non modifiables.

### Classes du module TSP et point d'entrée console

Le module TSP est représenté séparément pour faciliter la lecture. Le fichier
PlantUML de classes rassemble les deux parties.

```mermaid
classDiagram
    class Main {
        +obtenirMessageDemarrage() String$
        +main(String[] arguments)$
    }
    class Graphe {
        <<interface>>
        +obtenirNombreSommets() int
        +obtenirCoutArc(int indiceSommet, int indiceDestination) int
        +estArc(int indiceSommet, int indiceDestination) boolean
    }
    class GrapheComplet {
        ~int nombreSommets
        ~int[][] coutsArcs
    }
    class TSP {
        <<interface>>
        +chercherSolution(int dureeLimiteMillis, Graphe graphe)
        +obtenirSommetSolution(int indiceSommet) Integer
        +obtenirCoutSolution() int
    }
    class TemplateTSP {
        <<abstract>>
        -Integer[] meilleureSolution
        -int coutMeilleureSolution
        -int dureeLimiteMillis
        -long instantDebutMillis
        -long instantFinMillis
        +obtenirDureeCalcul() long
        #calculerBorneInferieure(sommetCourant, sommetsNonVisites)*
        #creerIterateurSuccesseurs(sommetCourant, sommetsNonVisites, graphe)*
        -explorerParSeparationEtEvaluation(sommetCourant, sommetsNonVisites, sommetsVisites, coutParcours)
    }
    class TSP1 {
        #calculerBorneInferieure(sommetCourant, sommetsNonVisites) int
        #creerIterateurSuccesseurs(sommetCourant, sommetsNonVisites, graphe)
    }
    class IteratorSeq {
        -Integer[] candidats
        -int nombreCandidats
        +hasNext() boolean
        +next() Integer
        +remove()
    }
    class RunTSP {
        +main(String[] arguments)$
    }
    GrapheComplet ..|> Graphe
    TemplateTSP ..|> TSP
    TSP1 --|> TemplateTSP
    TemplateTSP --> "0..1" Graphe : graphe
    TSP1 ..> IteratorSeq : crée
    IteratorSeq ..> Graphe : filtre les successeurs
    RunTSP ..> TSP1 : crée
    RunTSP ..> GrapheComplet : crée
```

`IteratorSeq` implémente `java.util.Iterator<Integer>`, ce qui impose les noms
`hasNext`, `next` et `remove`. Les classes réalisant les interfaces implémentent
leurs opérations, même lorsqu'elles ne sont pas répétées dans le diagramme.

## Diagramme de packages

Source : [packages-actuels.puml](packages-actuels.puml).

```mermaid
flowchart LR
    subgraph principal[fr.insa.optimod]
        Main[Main : entrée console]
    end
    subgraph vue[fr.insa.optimod.view]
        V[AppLauncher / MainApp<br/>MainView / MapCanvas / MapProjection]
    end
    subgraph controleur[fr.insa.optimod.controller]
        C[MainController / MapScreen]
    end
    subgraph modele[fr.insa.optimod.model]
        M[Plan / Livraison<br/>Noeud / Troncon / Demande / Entrepot]
    end
    subgraph algo[fr.insa.optimod.algo.tsp]
        A[TSP / TemplateTSP / TSP1<br/>Graphe / GrapheComplet<br/>IteratorSeq / RunTSP]
    end
    V -->|Crée le contrôleur et implémente MapScreen| C
    V -->|Affiche le plan| M
    C -->|Construit Plan et Livraison| M
    V --> J[JavaFX]
    M --> X[Java XML / DOM / SAX]
```

Les flèches représentent les dépendances du code. Le contrôleur utilise
`MapScreen`, défini dans son propre package, sans dépendance vers les classes
concrètes de la vue. Le module TSP est autonome à ce stade.
Le package `xml` contient uniquement `package-info.java` : la lecture est dans
`Plan` et `Livraison`.

## Principaux renommages

| Ancien nom | Nouveau nom |
| --- | --- |
| setOnImport | definirActionImportPlan |
| setOnImportLivraison | definirActionImportLivraison |
| importPlan | importerPlan |
| chooseXmlFile() | choisirFichierPlan() |
| chooseXmlFile(titre) | choisirFichierXml(titre) |
| chooseLivraisonFile | choisirFichierLivraison |
| showPlan | afficherPlan |
| showLivraisonChargee | afficherLivraisonChargee |
| showInvalidXml | afficherErreurXml |
| reset | reinitialiser |
| draw / clear (carte) | dessinerCarte / effacerCarte |
| toScreenX / toScreenY | calculerAbscisseEcran / calculerOrdonneeEcran |
| getPlan / getLivraison | obtenirPlan / obtenirLivraison |
| getDemandes / getEntrepot | obtenirDemandes / obtenirEntrepot |
| getHeure / getHeureDepart | obtenirHeureDepart |
| nouveauBuilder | creerLecteurXml |
| attributRequis | lireAttributObligatoire |
| bound | calculerBorneInferieure |
| iterator (TemplateTSP) | creerIterateurSuccesseurs |
| branchAndBound | explorerParSeparationEtEvaluation |
| screen / controller / view | ecran / controleur / vue |
| canvas / map | canevas / carte |
| livraisonStatus | messageLivraison |
| xmlFileName / doc | cheminFichierXml / documentXml |

Les autres accesseurs métier suivent le préfixe `obtenir` avec un nom explicite,
par exemple `obtenirIdentifiantCollecte` et `obtenirNombreSommets`.

## Export PlantUML

Seules les sources `.puml` sont versionnées : le rendu SVG dépend des polices
installées et différait d'une machine à l'autre. Les images sont :

- publiées sur [GitHub Pages](https://tristanhp.github.io/Optimod-Lyon/) à
  chaque merge sur `main` ;
- jointes à chaque exécution de la CI (artefact `diagrammes`) ;
- générables en local avec `./scripts/render-diagrams.sh` (sortie dans
  `docs/diagrams/out/`, ignoré par git).

Les anciens fichiers `architecture-mvc` et `sequence-calcul-tournee` décrivent
une conception prévisionnelle ; ils ne font pas partie des présents livrables.
