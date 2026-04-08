# Projet HELP'OPS

Application de gestion de tickets en Java RMI, avec un client console, un serveur d'authentification et un serveur de tickets.

## Objectif

Permettre a des utilisateurs de creer et suivre des tickets, et a des agents de traiter ces tickets.


## Modules

- ho-commun : interfaces RMI et modele Ticket partages
- ho-serveur-auth : authentification, tokens, roles, informations utilisateur
- ho-serveur-tickets : gestion des tickets, persistance JSON, supervision
- ho-client : client console utilisateur/agent et client supervision

## Fonctionnalites principales

### Cote utilisateur

- connexion
- creation de ticket (categorie + priorite)
- consultation de ses tickets
- affichage du detail d'un ticket

### Cote agent

- lister les tickets assignes
- lister tous les tickets
- prendre en charge un ticket
- liberer un ticket
- resoudre un ticket avec message
- afficher les statistiques
- creer un compte utilisateur

### Supervision

- abonnement a un flux d'evenements du serveur tickets
- mode rattrapage (reception des derniers evenements) ou mode temps reel

## Contrats RMI (resume)

### IAuthService

- login(login, password)
- verifierToken(token)
- getLoginByToken(token)
- getNomByToken(token)
- getIdUtilisateur(token)
- getRoleToken(token)
- getNomUtilisateurParId(idUtilisateur)
- creerCompte(login, password, nom, role)

### ITicketsService

- getTicket(token, id)
- declarerTicket(token, titre, categorie, description, priorite)
- listerTickets(token)
- listerTicketsAssignes(token)
- listerTousTickets(token)
- prendreEnCharge(token, idTicket)
- libererTicket(token, idTicket)
- resoudreTicket(token, idTicket, messageResolution)
- afficherStatistiques(token)
- sabonner(client, rattrapage)

## Donnees et formats

- Fichiers JSON :
  - ho-commun/src/main/ressources/ho/bd/utilisateurs.json
  - ho-commun/src/main/ressources/ho/bd/tickets.json
- ID ticket : incrementale sur 10 chiffres (ex: 0000000001)
- Format date : dd-MM-yyyy HH:mm:ss
- Categories : incident, demande
- Priorites : BASSE, MOYENNE, HAUTE

## Registre RMI

- Le serveur Auth doit etre lance en premier (il initialise le registre sur le port 1099)
- Le serveur Tickets se connecte ensuite au registre existant

## Lancement rapide

1. A la racine du projet : mvn clean install
2. Lancer ho.serveur.ServeurAuthLanceur
3. Lancer ho.serveur.ServeurTicketsLanceur
4. Lancer ho.client.ClientLanceur
5. Optionnel : lancer ho.client.LanceurSupervision

## Limites actuelles

- Persistance fichier JSON (pas de base de donnees)
- Tokens en memoire (perdus au redemarrage)
