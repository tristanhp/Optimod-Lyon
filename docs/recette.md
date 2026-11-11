# Cahier de Recette Manuelle - Optimod'Lyon

## 1. Charger un plan (Cas 1)
- **Action :** Cliquer sur "Importer le plan", sélectionner d'abord un fichier XML invalide, puis sélectionner `grandPlan.xml`.
- **Résultat attendu :** Un message d'erreur clair s'affiche pour le XML invalide (pas de plantage de l'application). Pour `grandPlan.xml`, le plan s'affiche correctement et s'adapte à la fenêtre.

## 2. Spécifier le nombre de livreurs (Cas 2)
- **Action :** Ajuster le champ "Livreurs" à 2 via les flèches de l'interface avant de lancer un calcul.
- **Résultat attendu :** L'interface prend en compte 2 livreurs (les listes et calculs futurs utiliseront ce paramètre).

## 3. Charger des demandes (Cas 3)
- **Action :** Sur le `grandPlan.xml` chargé, cliquer sur "Importer les demandes" et choisir `demandeGrand9.xml`.
- **Résultat attendu :** Les points de collecte (carrés) et de livraison (ronds) apparaissent sur la carte. La liste de gauche se remplit avec les demandes.

## 4. Éditer la demande (Cas 4)
- **Action :** Ajouter un arrêt, le déplacer sur la carte, le réordonner via les boutons "Monter" / "Descendre" dans la liste, puis supprimer une demande. Effectuer ensuite `Ctrl+Z` (Annuler) puis `Ctrl+Y` (Rétablir).
- **Résultat attendu :** Mise à jour instantanée de la carte et de la liste à chaque étape. Les commandes `Ctrl+Z` et `Ctrl+Y` restaurent parfaitement l'état visuel et les données.

## 5. Calcul de tournée & Plages horaires (Cas 5)
- **Action :** Avec `grandPlan.xml`, `demandeGrand9.xml` et 2 livreurs sélectionnés, cliquer sur "Calculer".
- **Résultat attendu :** Les tournées sont calculées. Les arrêts dont l'arrivée dépasse la plage horaire annoncée s'affichent distinctement en rouge.

## 6. Réaffectation & Tournée impossible / Rejet (Cas 6)
- **Action :** Sélectionner une demande du Livreur 1, cliquer sur "Changer de livreur" vers le Livreur 2. Essayer ensuite d'ajouter une demande physiquement impossible à réaliser avant 13h00.
- **Résultat attendu :** La demande réaffectée change immédiatement de couleur sur la carte. L'ajout de la demande impossible génère un message de rejet explicite (refus d'ajout).

## 7. Affichage Carte et Popup (Cas 7)
- **Action :** Observer les tracés de la tournée, survoler un point avec la souris, puis cliquer sur un arrêt de la carte.
- **Résultat attendu :** Une couleur unique par livreur est affichée, et le sens de parcours (flèches) est lisible. Le survol met en évidence le point. Le clic affiche un popup interactif avec l'adresse, le type et l'heure de passage.

## 8. Sauvegarder (Cas 8)
- **Action :** Après avoir effectué des modifications manuelles (déplacement, changement de livreur), cliquer sur "Sauvegarder" et enregistrer le fichier XML sur l'ordinateur.
- **Résultat attendu :** Création d'un fichier XML valide contenant toutes les données de la tournée modifiée.

## 9. Restaurer (Cas 9)
- **Action :** Effacer la carte (en rechargeant un plan vierge par exemple), puis cliquer sur "Restaurer" et charger le fichier XML sauvegardé à l'étape précédente.
- **Résultat attendu :** L'état exact de la tournée modifiée, avec ses livreurs, ses couleurs et ses horaires, est rechargé à l'identique.

## 10. Générer les feuilles de route (Cas 10)
- **Action :** Cliquer sur "Générer les feuilles de route" et choisir un dossier d'export sur l'ordinateur.
- **Résultat attendu :** Création d'un document (format HTML ou texte) par livreur, contenant les instructions de navigation détaillées étape par étape.
