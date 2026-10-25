# 0004. Design pattern Command pour annuler les modifications

- **Statut** : Acceptée
- **Date** : 2026-10-25

## Contexte

L'utilisateur doit pouvoir modifier les demandes après leur import (ajouter
une demande en cliquant sur la carte, en supprimer une) et **annuler** ou
**rétablir** ces modifications. Recalculer la tournée (plus courts chemins
puis TSP) prend du temps : annuler une modification doit réafficher la tournée
d'avant sans relancer le calcul.

Issue Linear : TIG-92.

## Décision

1. Chaque modification est une `Commande` (`executer()`, `annuler()`, package
   `controller`) : `CommandeAjouterDemande` (ajout en fin de liste) et
   `CommandeSupprimerDemande` (garde la demande retirée et son rang pour la
   remettre). Les commandes modifient `ProgrammeLivraisons`, qui prévient la vue
   (Observer, ADR 0003).
2. `ListeDeCommandes` garde l'historique : `executer`, `annuler`, `refaire`,
   `vider`. Exécuter une nouvelle commande oublie les commandes annulées.
   L'historique est vidé quand un plan ou des demandes sont importés.
3. **Cache des tournées** : le contrôleur garde les tournées calculées dans une
   `Map<List<Demande>, Tournee>` indexée par la liste des demandes. Après
   une commande, une annulation ou un rétablissement,
   `retrouverEtatProgramme()` réaffiche la tournée en cache
   (`EtatTourneeCalculee`) ou retire la tournée (`EtatProgrammeCharge`). Le
   cache est vidé avec l'historique.
4. Les états décident quand une commande est possible (ADR 0002) :
   modifications, annulation et rétablissement dans `EtatProgrammeCharge` et
   `EtatTourneeCalculee` (qui en hérite), rien pendant le calcul. L'ajout passe
   par deux états, `EtatAjoutCollecte` puis `EtatAjoutLivraison`, l'utilisateur
   cliquant sur deux intersections de la carte.
5. `MapScreen.activerActions` reçoit un `Set<MapScreen.Action>` au lieu de
   booléens, devenus trop nombreux.

## Alternatives envisagées

- **Sauvegarder une copie complète des demandes à chaque modification
  (Memento)** : plus simple à écrire, mais copie tout à chaque action ; les
  commandes ne gardent que ce qui change.
- **Recalculer la tournée après chaque annulation** : pas de cache à gérer,
  mais un calcul de plusieurs secondes sur les grands plans pour une simple
  annulation.
- **Cache attaché à chaque commande** (tournée avant/après) : ne retrouve pas
  une tournée calculée plus tard pour la même liste de demandes ; la clé
  « liste des demandes » couvre tous les cas.

## Conséquences

- Ajouter une nouvelle modification (ex. changer l'ordre de la tournée) =
  écrire une nouvelle `Commande` ; annuler et rétablir fonctionnent sans autre
  changement.
- `Demande` n'a pas d'`equals` : la clé du cache compare les demandes par
  identité, ce qui est voulu (une commande rejouée réutilise les mêmes objets).
- Une demande ajoutée à la main a des durées de collecte et de livraison de
  5 minutes (`EtatAjoutLivraison.DUREE_PAR_DEFAUT`) ; les saisir sera une
  évolution.
- Le cache grandit avec le nombre de listes calculées dans une session ; il est
  vidé à chaque import, ce qui suffit pour l'usage prévu.
