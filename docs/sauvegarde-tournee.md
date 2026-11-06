# Sauvegarde d'une tournée — version 1

Cette version correspond au modèle actuel à **un livreur**. Les tickets
TIG-47 et TIG-56 sont indiqués comme annulés par le PO dans leur description
Linear du 2 novembre ; l'implémentation existante reste disponible.
Le format multi-livreurs dépend de TIG-95. Les demandes rejetées ne sont pas
encore représentées. Les plages annoncées sont sauvegardées par arrêt.
Un fichier annonçant plusieurs livreurs est refusé, sans perte silencieuse.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<sauvegardeTournee version="1" nombreLivreurs="1" plan="plan.xml"
                   entrepot="1" heureDepart="08:00">
    <demande collecte="3" livraison="2"
             dureeCollecte="120" dureeLivraison="240"/>
    <arret noeud="1" duree="0"/>
    <arret noeud="3" duree="120"
           debutPlage="07:00:24" finPlage="09:00:24"/>
    <arret noeud="2" duree="240"/>
    <arret noeud="1" duree="0"/>
</sauvegardeTournee>
```

Les durées sont en secondes. L'ordre des demandes et celui des arrêts sont
indépendants, chacun est conservé. Le plan est référencé par un chemin
relatif au dossier de la sauvegarde, écrit avec des « / » (chemin absolu
seulement s'il est sur un autre disque). En lecture, ce chemin (relatif, ou
absolu dans les anciennes sauvegardes) est essayé d'abord ; s'il n'existe pas,
le plan est cherché par son nom (après le dernier « / » ou « \ ») dans le
dossier de la sauvegarde. Une sauvegarde déplacée avec son plan, ou écrite
sous Windows, se restaure donc encore. Le plan n'est pas recopié.
Une tournée sans demande contient seulement l'arrêt de départ de l'entrepôt.

Les attributs `debutPlage` et `finPlage` conservent les engagements annoncés,
indépendamment des heures recalculées. Ils sont écrits ensemble pour chaque
arrêt. Les fichiers version 1 antérieurs, sans ces attributs, restent lisibles :
la plage est alors fixée à l'arrivée recalculée ± 1 heure. Une seule borne
présente ou une heure invalide entraîne un refus du fichier.

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
