# 0005. Nettoyage du code et vocabulaire du glossaire

- **Statut** : Acceptée
- **Date** : 2026-10-25

## Contexte

Le document Linear *Nettoyage Code OPTIMOD* fixe les règles du code : code
propre et facile à maintenir, minimum de getters et setters, minimum de
classes, MVC respecté. Le code avait accumulé des classes à une seule
responsabilité technique (`AEtoile`, `CalculHoraires`, `GrapheChemins`,
l'interface `CalculateurTournee`), des structures en double (`Passage`,
`Etape`, `Entrepot` qui répétait l'heure de départ) et des noms qui ne
correspondaient pas au glossaire du projet (document Linear *AGILE Glossaire
du projet*) : la classe `Livraison` représentait en fait le *programme de
livraisons*, alors que « livraison » désigne le dépôt d'un colis.

Le calcul de la tournée (branche `tig-46`, en cours) n'était branché nulle part
dans l'application.

## Décision

1. **Le plan calcule la tournée.** `Plan.calculerTournee(programme)` est la
   seule méthode publique du calcul. Plus courts chemins (A*), graphe des plus
   courts chemins, ordre de passage (TSP) et heures de passage sont des
   méthodes privées de `Plan`. `AEtoile`, `CalculHoraires`, `GrapheChemins` et
   `CalculateurTournee` sont supprimées.
2. **Le TSP réutilise `GrapheDepandance`** (contrainte d'ordre : la livraison
   d'un colis dépend de sa collecte) et la dernière version, `TSP3`. Les
   versions intermédiaires (`TSP1`, `TSP2`) seront supprimées à la fin du
   projet, comme prévu par le document de nettoyage.
3. **Contraintes horaires (TIG-46)** : départ à l'heure du fichier de demandes,
   15 km/h, retour à l'entrepôt avant 13 h ; sinon la tournée est impossible
   (`IllegalStateException`, affichée par la vue).
4. **Modèle simplifié** :
   - `Noeud`, `Troncon`, `Chemin` et `Tournee` sont des `record` (pas de
     getters écrits à la main) ;
   - `ProgrammeLivraisons` remplace `Livraison` et `Entrepot` : entrepôt (un
     `Noeud`), heure de départ (`LocalTime`) et demandes ;
   - `Demande` devient un `record` imbriqué dans `ProgrammeLivraisons` : la
     paire (point de collecte, point de livraison) et les deux durées sur
     place ; les adresses sont des `Noeud` du plan, vérifiés à la lecture ;
   - `Tournee` (arrêts avec leurs heures de passage, et chemins entre arrêts)
     remplace `Passage` et `Etape`.
5. **Vocabulaire du glossaire** dans le code et l'interface : programme de
   livraisons, demande, point de collecte, point de livraison, durée sur
   place, tournée, arrêt, heure de passage, entrepôt, nœud, tronçon, adresse.
   Les méthodes de `MapScreen` sont francisées (`afficherTournee`,
   `afficherAttente`, `definirActionCalculer`...).
6. Le contrôleur reçoit le calcul sous forme de fonction
   (`Plan::calculerTournee` par défaut) pour rester testable sans calcul réel.

## Alternatives envisagées

- **Garder une classe par algorithme** (`AEtoile`, `CalculHoraires`...) :
  testable isolément, mais contraire à la règle « minimiser le nombre de
  classes » et à la décision de l'équipe.
- **`Demande` comme pure paire de nœuds** (`Map.Entry<Noeud, Noeud>`) avec les
  durées dans une autre structure : plus proche du texte du document, mais deux
  structures à garder synchronisées ; le `record` imbriqué est la paire plus
  ses durées.
- **Garder les noms existants** avec une table de correspondance : moins de
  modifications, mais un écart permanent entre le code et le glossaire.

## Conséquences

- La branche `tig-46` est reprise dans ce nettoyage (corrigée : liste
  d'arrêts initialisée, sens de la contrainte d'ordre) et devient obsolète.
- `plusCourtChemin` et `heuristique` restent visibles dans le package `model`
  pour les tests (`PlanTest`).
- Les accesseurs des `record` portent le nom du champ (`noeud.latitude()`) ;
  les méthodes des classes gardent le préfixe `obtenir`.
