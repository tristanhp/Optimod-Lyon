# 0006. Plusieurs livreurs, une tournée par livreur

- **Statut** : Proposée
- **Date** : 2026-11-01

## Contexte

Le sujet prévoit « 1 livreur par défaut, modifiable par l'utilisateur ».
Jusqu'ici le modèle ne connaissait qu'une tournée : `Plan.calculerTournee`
renvoyait une `Tournee`, et le contrôleur gardait les tournées calculées dans
un cache indexé par la liste des demandes (ADR 0004, point 3). Plusieurs
tickets attendent un modèle à plusieurs livreurs : réglage du nombre de
livreurs (TIG-55), affichage par livreur (TIG-83), changement de livreur
(TIG-80), infaisabilité (TIG-45, 70, 71), sauvegarde et restauration (TIG-47,
56). Le recalcul local (TIG-101) fait aussi de l'ordre des arrêts la donnée de
référence, ce qu'un cache indexé par les demandes ne permet pas.

Le cas 6 demande de montrer en rouge les arrêts d'une tournée qui revient
après 13 h : il faut donc pouvoir obtenir cette tournée, alors que le calcul
levait une exception (ADR 0005, point 3).

Issue Linear : TIG-95. Le contrat d'interface correspondant a été rédigé
avant le code et relu dans la PR.

## Décision

1. **Livreur d'une demande** : `Demande` gagne un champ `livreur` (numéro à
   partir de 1). Le constructeur à quatre paramètres confie la demande au
   livreur 1 ; toutes les demandes d'un fichier vont au livreur 1.
   `avecLivreur(n)` renvoie une copie confiée à un autre livreur.
2. **Nombre de livreurs** dans `ProgrammeLivraisons` : 1 par défaut, au moins
   1 (`definirNombreLivreurs`). Seuls les derniers livreurs peuvent être
   retirés, et seulement s'ils n'ont aucune demande. Une demande d'un livreur
   inexistant est refusée.
3. **Les tournées sont une donnée du modèle**, rangées dans
   `ProgrammeLivraisons` (une par livreur, `obtenirTournees`,
   `definirTournees`, `definirTournee`, `effacerTournees`) et observées par la
   vue (ADR 0003). Le cache du contrôleur est supprimé.
4. **Arrêts** : `Tournee.Arret` connaît son type (`ENTREPOT`, `COLLECTE`,
   `LIVRAISON`) et sa demande (`null` à l'entrepôt).
5. **Calcul** : `Plan.calculerTournees(programme)` renvoie une tournée par
   livreur ; le voyageur de commerce tourne sur les seules demandes de chaque
   livreur ; un livreur sans demande a une tournée vide (un seul arrêt,
   l'entrepôt). La limite de 10 s est partagée à parts égales entre les
   livreurs qui ont des demandes.
6. **Retour après 13 h** : la tournée est renvoyée, et
   `Tournee.estPossible()` est faux. Chaque arrêt sait s'il est atteint après
   13 h (`Arret.estApresHeureLimite()`), pour que la vue l'affiche en rouge.
   Le contrôleur range la tournée sans fenêtre d'erreur : « Calculer »
   n'échoue pas à cause de 13 h, et le signal est le rouge des arrêts. Une
   adresse inaccessible ou un calcul sans solution restent une
   `IllegalStateException`.
7. **Annuler et rétablir** : une commande qui modifie les demandes garde les
   tournées d'avant et d'après. `annuler()` remet celles d'avant ; rétablir
   remet celles d'après, sans recalcul. Quand l'utilisateur calcule après une
   modification ou une annulation, `ListeDeCommandes.memoriserTournees`
   transmet les nouvelles tournées aux deux commandes voisines de l'état
   actuel, ce qui garde le comportement de l'ancien cache.
8. **Écran** : `MapScreen.afficherTournee` et `effacerTournee` sont retirées ;
   `MainView` relit les tournées du programme à chaque notification. Avec
   plusieurs livreurs, la liste porte un titre par livreur.
9. **Mémoire de l'ordre de passage** : relancer le voyageur de commerce coûte
   jusqu'à 10 s, donc `Plan` garde l'ordre qu'il a trouvé pour chaque livreur.
   La clé est la liste des sommets du livreur (adresses, durées, types et
   demandes) ; la valeur est l'ordre de passage. Seul « Calculer »
   (`calculerTournees`) la consulte : le recalcul local ne la lit ni ne
   l'écrit, et garde l'ordre affiché. Un ordre n'y est rangé qu'une fois la
   tournée obtenue. Elle vit autant que le `Plan`, donc repart de zéro à chaque
   import, et la table est concurrente puisque le calcul tourne hors du thread
   de l'interface. Ce n'est pas un cache d'affichage : la vue lit toujours les
   tournées du programme.

10. **Changer l'ordre des arrêts (TIG-44)** : « Monter » et « Descendre »
    échangent un arrêt avec son voisin dans la tournée **d'un livreur**. La vue
    transmet le livreur et le rang de la ligne sélectionnée ; `MapScreen`
    reçoit donc les deux, et resélectionne l'arrêt déplacé par (livreur, rang).
    `Plan.calculerTourneeOrdreChange` vérifie la contrainte d'ordre sur les
    arrêts eux-mêmes (une collecte suivie de la livraison de la même demande),
    sans chercher les arrêts par leur nœud. `CommandeChangerOrdre` garde les
    tournées d'avant et d'après, comme les autres commandes. Le contrôleur
    refuse un changement qui rend impossible une tournée possible (retour
    après 13 h) ; il accepte de changer l'ordre d'une tournée déjà en retard.

## Alternatives envisagées

- **Un nouvel objet observable pour les tournées** : sépare demandes et
  tournées, mais ajoute une classe et une seconde inscription de la vue ; le
  programme de livraisons est déjà observé et contient déjà les demandes que
  les tournées servent.
- **`MapScreen.afficherTournees(...)` appelée par le contrôleur** (prévu au
  départ dans le ticket) : deux chemins d'affichage (observation et appel
  direct) qui peuvent diverger après une annulation.
- **Garder l'exception après 13 h** : le cas 6 n'aurait pas la tournée à
  montrer.
- **10 s par livreur** : le temps de « Calculer » grandirait avec le nombre
  de livreurs.
- **Garder le cache indexé par les demandes** : incompatible avec le recalcul
  local, où deux tournées différentes peuvent servir les mêmes demandes. Il
  aurait aussi ressorti un ordre du voyageur de commerce par-dessus un ordre
  choisi par l'utilisateur (changer l'ordre, changer de livreur). La mémoire
  du point 9 évite cela : elle ne garde pas de tournée, seulement le résultat
  du voyageur de commerce, qui ne dépend que des sommets du livreur.

## Conséquences

- Avec un livreur, le comportement est le même qu'avant, sauf le retour après
  13 h, désormais affiché sans message d'erreur (test
  `retourApres13hSignale`).
- Les commandes futures (changer de livreur, déplacer, réordonner, rejeter)
  implémentent `memoriserTourneesAvant` et `memoriserTourneesApres`.
- Le recalcul local (TIG-101) ne touche que la tournée du livreur de la
  demande supprimée. `MainController` le lance hors du thread de l'interface
  (`EtatCalculEnCours`), puis exécute `CommandeSupprimerDemande` avec, comme
  tournées d'après, celles d'avant dont la tournée de ce livreur est remplacée :
  Annuler et Rétablir remettent les deux sans calcul. Si le recalcul échoue,
  les tournées sont retirées et l'utilisateur est prévenu.
- Les arrêts portent leur demande : le recalcul local retrouve les arrêts
  d'une demande sans ambiguïté, même quand deux demandes ont le même point de
  collecte et la même durée.
- Le recalcul local renvoie la tournée même si elle finit après 13 h, comme le
  calcul complet : c'est `Tournee.estPossible()` qui la signale.
- Deux « Calculer » de suite sur les mêmes demandes d'un livreur ne relancent
  pas le voyageur de commerce : la seconde tournée reprend l'ordre de la
  première, au lieu d'un nouveau résultat indépendant.
- La mémoire de l'ordre de passage n'est pas bornée : elle grandit avec le
  nombre de listes de demandes calculées entre deux imports de plan.
- Chaque modification du programme peut notifier plusieurs fois la vue
  (tournées puis demandes) ; l'ordre des appels garde un état cohérent à
  chaque notification.
- `Plan` grossit encore un peu (calcul par livreur) ; son découpage reste à
  décider plus tard si besoin.
