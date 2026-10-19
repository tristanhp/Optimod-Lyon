# Décisions d'architecture (ADR)

Ce dossier contient les *Architecture Decision Records* du projet : une fiche
courte par décision technique importante, qui explique le contexte, la décision
prise et ses conséquences. Le but est qu'un membre de l'équipe (ou un
correcteur) comprenne **pourquoi** le projet est organisé ainsi.

## Index

| N° | Titre | Statut |
| --- | --- | --- |
| [0001](0001-organisation-des-tests-unitaires.md) | Organisation des tests unitaires | Acceptée |

## Ajouter un ADR

1. Copier [`modele.md`](modele.md) en `NNNN-titre-en-kebab-case.md`, avec le
   numéro suivant.
2. Remplir les sections, avec le statut *Proposée*.
3. Ouvrir une PR (titre `docs: ...` ou avec l'ID Linear) : la discussion se fait
   dans la revue.
4. Au merge, passer le statut à *Acceptée* et ajouter la ligne dans l'index
   ci-dessus.

On ne réécrit pas un ADR accepté : si la décision change, on en crée un nouveau
qui le *remplace*, et on passe l'ancien au statut *Remplacée par NNNN*.
