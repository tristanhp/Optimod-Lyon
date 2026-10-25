# 0003. Design pattern Observer entre le modèle et la vue

- **Statut** : Acceptée
- **Date** : 2026-10-25

## Contexte

Les demandes de livraison vont pouvoir être modifiées après leur import
(ajout, suppression, annulation : TIG-92). Sans mécanisme de notification, le
contrôleur devrait penser à redemander l'affichage de la liste et de la carte
après chaque modification, et chaque nouvelle vue (liste, carte, résumé)
ajouterait un appel à ne pas oublier.

`java.util.Observable` est obsolète depuis Java 9, et les propriétés JavaFX
rendraient le modèle dépendant de JavaFX, ce que l'architecture MVC interdit.

Issue Linear : TIG-91.

## Décision

1. Deux types dans `model`, sans dépendance à JavaFX :
   - `Observateur` : interface fonctionnelle `mettreAJour(Observable source)` ;
   - `Observable` : classe abstraite qui gère la liste des observateurs
     (`ajouterObservateur`, `retirerObservateur`, `notifierObservateurs`).
2. `ProgrammeLivraisons` hérite de `Observable`. Ses demandes deviennent modifiables via
   `ajouterDemande(position, demande)` et `retirerDemande(position)`, qui
   préviennent les observateurs. `obtenirDemandes()` reste non modifiable pour
   l'appelant.
3. `MainView` implémente `Observateur` : quand le contrôleur lui donne des
   demandes à afficher, elle s'y inscrit (et se désinscrit des précédentes),
   puis redessine la liste et la carte à chaque notification.
4. Le contrôleur n'a plus à demander le rafraîchissement après une
   modification : il modifie le modèle, la vue suit.

## Alternatives envisagées

- **`java.util.Observable` / `Observer`** : obsolètes, typage faible (`Object`).
- **Propriétés JavaFX (`ObservableList`) dans le modèle** : moins de code, mais
  le modèle dépendrait de JavaFX et ne serait plus testable sans lui.
- **Le contrôleur rafraîchit la vue après chaque modification** : fonctionne
  avec une seule vue, mais chaque nouvelle modification ou vue ajoute un appel
  à ne pas oublier.

## Conséquences

- Toute modification future du modèle (ex. tournée modifiable) peut réutiliser
  `Observable` sans changer la vue existante.
- `ProgrammeLivraisons` n'est plus immuable : les modifications se font sur le thread de
  l'interface, et l'état `EtatCalculEnCours` (ADR 0002) bloque toute action
  pendant le calcul en arrière-plan, qui lit les demandes.
- La vue doit se désinscrire de l'ancienne `ProgrammeLivraisons` quand elle en affiche
  une autre ; c'est fait dans `MainView.observer(...)`.
