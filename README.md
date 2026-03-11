# Projet HELP'OPS

## 1) Objectif

HELP'OPS est une application distribuée de gestion de tickets d'assistance basée sur Java RMI.

Fonctionnalités principales :
- authentification utilisateur,
- création de tickets,
- consultation des tickets,
- prise en charge des tickets par les agents,
- libération d'un ticket par l'agent qui en est responsable.

## 2) Architecture

Le projet est découpé en modules Maven :

- `ho-commun` : contrats RMI + modèle métier partagé (`Ticket`).
- `ho-serveur-auth` : authentification, tokens, résolution des identités utilisateur.
- `ho-serveur-tickets` : logique métier des tickets + persistance JSON.
- `ho-client` : interface console.

## 3) Contrats RMI (résumé)

### `IAuthService`
Principales opérations :
- `login(login, password)`
- `verifierToken(token)`
- `getLoginByToken(token)`
- `getIdUtilisateur(token)`
- `getRoleToken(token)`
- `getNomByToken(token)`
- `getNomUtilisateurParId(idUtilisateur)`

### `ITicketsService`
Principales opérations :
- `declarerTicket(...)`
- `listerTickets(token)`
- `listerTicketsAssignes(token)`
- `listerTousTickets(token)`
- `prendreEnCharge(token, idTicket)`
- `libererTicket(token, idTicket)`

## 4) Registre RMI

Règle projet : seul le serveur d'authentification crée le registre RMI.

- `ServeurAuthLanceur` : crée le registre sur `1099` si absent, puis publie `AuthService`.
- `ServeurTicketsLanceur` : ne crée pas le registre, il se connecte à un registre existant puis publie `TicketsService`.

Conséquence : il faut démarrer Auth avant Tickets.

## 5) Authentification et identité

Le serveur d'authentification est l'autorité centrale :
- validation des identifiants,
- génération des tokens (`TOKEN-{UUID}`),
- validation de tokens pour les autres services,
- résolution des informations utilisateur (id, rôle, nom).

Le client ne lit pas directement les JSON utilisateurs : il interroge `AuthService`.

## 6) Métier ticket

Un ticket contient :
- `id`, `titre`, `categorie`, `description`,
- `etat`, `dateCreation`, `dateAssignation`,
- `idCreateur`, `idAgent`.

Catégories autorisées :
- `incident`
- `demande`

## 7) Dates et IDs

- ID ticket : format incrémental sur 10 chiffres (`0000000001`, ...).
- `dateCreation` : `dd-MM-yyyy HH:mm:ss`.
- `dateAssignation` : `dd-MM-yyyy HH:mm:ss`.
- logs serveurs : `dd-MM-yyyy HH:mm:ss`.

## 8) Persistance JSON

Fichiers de données :
- `ho-commun/src/main/ressources/ho/bd/utilisateurs.json`
- `ho-commun/src/main/ressources/ho/bd/tickets.json`

La persistance est réalisée côté serveurs uniquement.

## 9) Comportement client (console)

### Utilisateur
- se connecter,
- créer un ticket,
- lister ses tickets,
- afficher le détail d'un ticket.

### Agent
- lister ses tickets assignés,
- afficher le détail d'un ticket assigné,
- lister tous les tickets,
- afficher le détail d'un ticket,
- selon le contexte :
  - prendre en charge un ticket non assigné,
  - libérer un ticket assigné à lui-même.

Dans le détail d'un ticket, le client affiche :
- date de création,
- date d'assignation,
- nom du créateur,
- nom de l'agent assigné.

Les noms sont résolus via `AuthService.getNomUtilisateurParId(...)`.

## 10) Règles de prise en charge / libération

### Prendre en charge
- réservé au rôle `agent`,
- refusé si le ticket est déjà assigné à un autre agent.

### Libérer
- réservé au rôle `agent`,
- autorisé seulement si le ticket est assigné à l'agent connecté,
- remet le ticket en `OPEN`, supprime `idAgent` et vide `dateAssignation`.

## 11) Procédure de lancement

1. Compiler à la racine : `mvn clean install`
2. Lancer `ServeurAuthLanceur`
3. Lancer `ServeurTicketsLanceur`
4. Lancer `ClientLanceur`

Si `ho-commun` est modifié, recompiler et relancer les modules concernés.

## 12) Limites actuelles

- persistance JSON fichier (pas de base de données),
- tokens en mémoire (perdus au redémarrage),
- pas de workflow avancé de résolution (`IN_PROGRESS`, `CLOSED`, etc.),
- pas d'historique détaillé des changements de ticket.

## 13) Pistes d'amélioration

- stockage en base relationnelle,
- persistance des sessions/tokens,
- historique d'actions ticket,
- API REST complémentaire au client console,
- tests automatisés plus poussés (intégration RMI + métier).
