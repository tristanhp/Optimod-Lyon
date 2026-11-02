# Sauvegarde d'une tournée — version 1

Cette version correspond au modèle actuel à **un livreur**. Les tickets
TIG-47 et TIG-56 demandent un format multi-livreurs qui devra être adapté
après intégration de TIG-95. Les plages annoncées et les demandes rejetées
ne sont pas encore représentées par ce modèle et ne sont pas sauvegardées.
Un fichier annonçant plusieurs livreurs est refusé, sans perte silencieuse.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<sauvegardeTournee version="1" nombreLivreurs="1" plan="plan.xml"
                   entrepot="1" heureDepart="08:00">
    <demande collecte="3" livraison="2"
             dureeCollecte="120" dureeLivraison="240"/>
    <arret noeud="1" duree="0"/>
    <arret noeud="3" duree="120"/>
    <arret noeud="2" duree="240"/>
    <arret noeud="1" duree="0"/>
</sauvegardeTournee>
```

Les durées sont en secondes. L'ordre des demandes et celui des arrêts sont
indépendants, chacun est conservé. Le plan est référencé par chemin absolu
lors de l'écriture ; un chemin relatif est aussi accepté en lecture et se
résout par rapport au dossier de la sauvegarde. Le plan n'est pas recopié.
Une tournée sans demande contient seulement l'arrêt de départ de l'entrepôt.

Les chemins et les heures sont recalculés par A* dans `Plan`, uniquement entre
arrêts consécutifs, sans TSP. Les règles actuelles du modèle s'appliquent,
dont le retour avant 13 h. La lecture utilise `Plan.creerLecteurXml()` et
interdit DOCTYPE. Elle construit un résultat indépendant avant de remplacer
le plan, le programme, la tournée et l'historique de l'application.

Vérification manuelle après installation du JDK 27 et de Maven : charger
`petitPlan.xml` et `demandePetit2.xml`, calculer, supprimer une demande,
recalculer, sauvegarder, fermer puis relancer l'application et restaurer.
Vérifier les mêmes arrêts et horaires, et Annuler/Rétablir désactivés.
Essayer ensuite un XML invalide et un plan référencé déplacé : le programme
affiché doit rester inchangé.
