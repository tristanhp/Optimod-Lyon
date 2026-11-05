# Diagrammes (schémas du logiciel)

Seules les sources PlantUML (`*.puml`) sont versionnées. Les images sont
publiées sur [GitHub Pages](https://tristanhp.github.io/Optimod-Lyon/) à chaque
merge sur `main`, jointes à chaque exécution de la CI (artefact `diagrammes`)
et générables en local avec `./scripts/render-diagrams.sh` (sortie dans
`docs/diagrams/out/`, ignoré par git : le rendu dépend des polices de la
machine).

| Diagramme | Source | Contenu |
| --- | --- | --- |
| Cas d'utilisation | [use-cases.puml](use-cases.puml) | Ce que l'utilisateur peut faire |
| Architecture MVC | [architecture-mvc.puml](architecture-mvc.puml) | Packages et sens des dépendances |
| Classes | [classes.puml](classes.puml) | Modèle, contrôleur (états, commandes), vue |
| États du contrôleur | [etats-controleur.puml](etats-controleur.puml) | Design pattern State |
| Charger le plan | [sequence-chargement-plan.puml](sequence-chargement-plan.puml) | Séquence |
| Charger le programme | [sequence-chargement-programme.puml](sequence-chargement-programme.puml) | Séquence |
| Calculer la tournée | [sequence-calcul-tournee.puml](sequence-calcul-tournee.puml) | Séquence |
| Ajouter après calcul | [sequence-ajout-demande.puml](sequence-ajout-demande.puml) | Saisie, insertion sans TSP, commande et erreurs |
| Changer le livreur d'une demande | [sequence-changer-livreur.puml](sequence-changer-livreur.puml) | Retrait, insertion sans TSP, commande et refus |

Règles : les diagrammes qui ne sont pas de séquence contiennent
`!pragma layout smetana` (rendu identique partout, sans Graphviz) ; on met à
jour le diagramme concerné dans la même PR que le code. Le vocabulaire est
celui du glossaire (voir le [README](../../README.md#glossaire)).

Les trois schémas ci-dessous résument les design patterns ; ils sont en
Mermaid pour être lisibles directement sur GitHub.

## État du contrôleur (State)

`MainController` délègue chaque action à son état courant, qui décide si
l'action est permise, quels boutons sont actifs et quel est l'état suivant
([ADR 0002](../adr/0002-design-pattern-state-controleur.md)).

```mermaid
stateDiagram-v2
    [*] --> EtatInitial
    EtatInitial --> EtatPlanCharge : importerPlan [valide]
    EtatPlanCharge --> EtatProgrammeCharge : importerProgramme [valide]
    EtatProgrammeCharge --> EtatCalculEnCours : calculer
    EtatCalculEnCours --> EtatTourneeCalculee : finirCalcul [tournées]
    EtatCalculEnCours --> EtatProgrammeCharge : finirCalcul [erreur, sans tournées]
    EtatCalculEnCours --> EtatTourneeCalculee : finirCalcul [erreur d'un recalcul]
    EtatTourneeCalculee --> EtatCalculEnCours : calculer [confirmerRecalcul]
    EtatTourneeCalculee --> EtatTourneeCalculee : calculer [refusé]
    EtatProgrammeCharge --> EtatAjoutCollecte : ajouterDemande
    EtatAjoutCollecte --> EtatAjoutLivraison : clic valide / aperçu collecte
    EtatAjoutLivraison --> EtatAjoutLivraison : second clic / aperçu livraison
    EtatAjoutCollecte --> EtatProgrammeCharge : annuler / effacer aperçu
    EtatAjoutLivraison --> EtatProgrammeCharge : annuler / effacer aperçu
    EtatAjoutLivraison --> EtatProgrammeCharge : cliquerNoeud / CommandeAjouterDemande
    EtatAjoutLivraison --> EtatCalculEnCours : saisie valide et tournées calculées / insertion
    EtatCalculEnCours --> EtatTourneeCalculee : insertion réussie / commande
    EtatCalculEnCours --> EtatTourneeCalculee : insertion impossible / alerte, état conservé
    EtatTourneeCalculee --> EtatProgrammeCharge : supprimerDemande (tournées retirées)
    EtatProgrammeCharge --> EtatTourneeCalculee : annuler [tournées d'avant calculées]
```

## Observation du programme (Observer)

`ProgrammeLivraisons` prévient ses observateurs à chaque ajout ou retrait de
demande ; `MainView` se redessine seule
([ADR 0003](../adr/0003-design-pattern-observer-modele-vue.md)).

```mermaid
sequenceDiagram
    participant X as Commande
    participant L as ProgrammeLivraisons
    participant V as MainView
    participant M as MapCanvas
    Note over V,L: afficherProgramme(programme) : V s'inscrit sur L
    X->>L: retirerDemande(position)
    L->>V: mettreAJour(programme)
    V->>M: afficherProgramme(programme)
    V->>V: reconstruit la liste des demandes
```

## Annuler une modification (Command)

Chaque modification est une `Commande` gardée dans `ListeDeCommandes` ; elle
garde les tournées d'avant et d'après, rangées dans le programme
([ADR 0004](../adr/0004-design-pattern-command-annulation.md),
[ADR 0006](../adr/0006-plusieurs-livreurs.md)).

```mermaid
sequenceDiagram
    actor U as Utilisateur
    participant C as MainController
    participant H as ListeDeCommandes
    participant L as ProgrammeLivraisons
    participant V as MainView
    U->>C: Supprimer la demande
    C->>H: executer(CommandeSupprimerDemande)
    H->>L: effacerTournees() puis retirerDemande(position)
    L->>V: mettreAJour(programme)
    U->>C: Ctrl+Z
    C->>H: annuler()
    H->>L: ajouterDemande(position, demande)
    H->>L: definirTournees(tournées d'avant)
    L->>V: mettreAJour(programme)
```
