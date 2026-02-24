# Projet HELP'OPS

## 1) Objectif du projet

HELP'OPS est une application distribuée de gestion de tickets d’assistance.
Elle permet à un utilisateur de :
- s’authentifier,
- créer un ticket,
- lister les tickets.

Le projet est basé sur Java RMI, avec séparation claire des responsabilités entre les services.

## 2) Architecture globale

Le workspace est organisé en modules Maven :

- `ho-commun` : interfaces distantes RMI + modèle de données partagé (`Ticket`).
- `ho-serveur-auth` : authentification, génération et validation des tokens.
- `ho-serveur-tickets` : opérations métier sur les tickets et persistance JSON.
- `ho-client` : interface console utilisateur.

Cette architecture permet d’éviter le couplage fort entre client et implémentations serveurs.

## 3) Contrats RMI (module commun)

Le client et les serveurs communiquent uniquement via des interfaces distantes.

Exemple d’interface d’authentification (`IAuthService`) :

```java
public interface IAuthService extends Remote {
    String login(String login, String motDePasse) throws RemoteException;
    boolean verifierToken(String token) throws RemoteException;
    String getLoginByToken(String token) throws RemoteException;
}
```

Exemple d’interface tickets (`ITicketsService`) :

```java
public interface ITicketsService extends Remote {
    Ticket declarerTicket(String token, String titre, String categorie, String description) throws RemoteException;
    List<Ticket> listerTickets(String token) throws RemoteException;
    Ticket getTicket(String token, String id) throws RemoteException;
}
```

Point important : les signatures sont centralisées dans `ho-commun`, ce qui garantit un contrat unique entre client et serveurs.

## 4) Démarrage des serveurs et registre RMI

Chaque serveur publie son service dans le registre RMI sur le port `1099`.

Exemple de démarrage d’un serveur :

```java
System.setProperty("java.rmi.server.hostname", "localhost");
Registry reg;
try {
    reg = LocateRegistry.getRegistry(1099);
    reg.list();
} catch (Exception e) {
    reg = LocateRegistry.createRegistry(1099);
}
reg.rebind("AuthService", authServiceImpl);
```

Pourquoi `localhost` est important : cela évite les références réseau non joignables dans les stubs RMI pendant les tests locaux.

## 5) Authentification et gestion des tokens

Le serveur d’authentification est la seule autorité de validation.

Principe :
- `login()` valide les identifiants,
- génère un token de type `TOKEN-{UUID}`,
- conserve les tokens actifs en mémoire,
- expose `verifierToken()` et `getLoginByToken()` pour les autres services.

Extrait simplifié :

```java
String token = "TOKEN-" + UUID.randomUUID();
tokensActifs.add(token);
tokenParLogin.put(token, login);
```

Validation :

```java
public boolean verifierToken(String token) throws RemoteException {
    if (token == null) {
        log("Verification token (null)");
        return false;
    }
    boolean valide = tokensActifs.contains(token);
    String auteur = tokenParLogin.getOrDefault(token, "inconnu");
    log("Verification token pour " + auteur);
    log("token " + (valide ? "valide" : "non valide"));
    return valide;
}
```

## 6) Validation inter-serveurs (point clé d’architecture)

Le serveur tickets ne valide pas localement le token :
il appelle le serveur auth via RMI.

Flux réel :
1. Le client envoie son token au serveur tickets.
2. Le serveur tickets appelle `authService.verifierToken(token)`.
3. Le serveur auth renvoie vrai/faux et journalise la décision.
4. Le serveur tickets continue ou rejette la requête.

Extrait simplifié côté tickets :

```java
IAuthService authService = connecterAuthService();
boolean valide = authService.verifierToken(tokenNettoye);
String login = authService.getLoginByToken(tokenNettoye);
log("Serveur Ticket verifie le token de " + (login == null ? "inconnu" : login) + " aupres du Serveur Auth");
if (!valide) {
    throw new RemoteException("Token d'authentification invalide");
}
```

Cette séparation est importante pour la cohérence et la sécurité du système.

## 7) Métier ticket

Un ticket contient les informations suivantes :
- identifiant,
- titre,
- catégorie,
- description,
- état,
- date de création,
- identifiant créateur.

Catégories autorisées :
- `incident`
- `demande`

Validation de catégorie côté serveur :

```java
private String normaliserCategorie(String categorie) {
    if (categorie == null) {
        return "incident";
    }
    String c = categorie.trim().toLowerCase();
    if (!"incident".equals(c) && !"demande".equals(c)) {
        return "incident";
    }
    return c;
}
```

## 8) Persistance JSON

Les tickets sont persistés dans :
`ho-commun/src/main/ressources/ho/bd/tickets.json`

Le serveur tickets :
- charge le contenu JSON,
- ajoute/modifie les entrées,
- réécrit le fichier complet.

Extrait d’écriture :

```java
Files.writeString(
    chemin,
    nouveauContenu.toString(),
    StandardCharsets.UTF_8,
    StandardOpenOption.CREATE,
    StandardOpenOption.TRUNCATE_EXISTING
);
```

Remarque : la persistance actuelle est fichier (pas base SQL), adaptée à une démonstration pédagogique.

## 9) Génération des IDs et format des dates

### ID ticket
Format : 10 chiffres, incrémental.
Exemples : `0000000001`, `0000000002`, `0000000003`.

Principe simplifié :

```java
long prochain = dernierId + 1;
String id = String.format("%010d", prochain);
```

### Dates
- Ticket : `dd-MM-yyyy`
- Logs serveur : `dd-MM-yyyy HH:mm:ss`

Exemple :

```java
DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
String horodatage = LocalDateTime.now().format(fmt);
```

## 10) Client console

Le client est conçu pour rester utilisable même si un serveur est temporairement indisponible.

Comportement important :
- menu de connexion initial,
- authentification avec 3 tentatives max,
- appels au serveur tickets au moment des actions,
- messages clairs si serveur injoignable.

Extrait (création ticket) :

```java
System.out.print("Titre du ticket : ");
String titre = scanner.nextLine();
String categorie = demanderCategorie(scanner);
System.out.print("Description du ticket : ");
String description = scanner.nextLine();
Ticket nouveauTicket = ticketsService.declarerTicket(token, titre, categorie, description);
System.out.println("Ticket créé avec succès ! ID : " + nouveauTicket.getId());
```

Affichage liste tickets :

```java
System.out.println("- " + t.getId() + " " + t.getTitre() + " [" + t.getEtat() + "] : " + t.getDescription());
```

## 11) Logs et observabilité

Le logging sert à expliquer “qui fait quoi” pendant la démo.

Exemples de logs attendus :

Auth :
```text
[24-02-2026 20:23:45] Tentative connexion login='user1'
[24-02-2026 20:23:45] user1 - authentifie - token genere
[24-02-2026 20:23:49] Verification token pour user1
[24-02-2026 20:23:49] token valide
```

Tickets :
```text
[24-02-2026 20:23:50] user1 - declarerTicket titre='Mon ticket'
[24-02-2026 20:23:50] Serveur Ticket verifie le token de user1 aupres du Serveur Auth
[24-02-2026 20:23:50] user1 - token valide pour declarerTicket
[24-02-2026 20:23:50] user1 - ticket cree id='0000000001' categorie='incident'
```

## 12) Procédure de lancement pour la présentation

Ordre recommandé :

1. Compiler toute la solution (racine) :
   - `mvn install`
2. Lancer `ServeurAuthLanceur`.
3. Lancer `ServeurTicketsLanceur`.
4. Lancer `ClientLanceur`.

Si vous modifiez `ho-commun`, refaire un `mvn install` à la racine avant relance.

## 13) Scénario de démonstration (métier)

Scénario simple pour le professeur :

1. Connexion utilisateur réussie.
2. Création d’un ticket de catégorie `incident`.
3. Affichage du message de succès avec ID.
4. Listage des tickets avec format :
   - `ID + titre + état + description`.
5. Montrer dans les consoles serveur :
   - validation token par auth,
   - action métier exécutée côté tickets.

Valeur métier démontrée :
- traçabilité des demandes,
- structuration incident/demande,
- base exploitable pour workflow support.

## 14) Limites actuelles et améliorations possibles

Limites de la version actuelle :
- persistance fichier JSON (mono-instance),
- tokens en mémoire (perdus au redémarrage),
- pas de gestion de rôles utilisateurs,
- pas de workflow d’état avancé.

Améliorations possibles :
- base de données relationnelle,
- persistance de session/token,
- rôles (demandeur/technicien/admin),
- états `IN_PROGRESS`, `RESOLVED`, `CLOSED`,
- historisation et audit complet,
- interface web/API REST en complément de RMI.
