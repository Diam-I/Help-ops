# Projet HELP'OPS

## Architecture Multi-Modules

Le projet est structuré en modules Maven pour séparer les responsabilités :

- **ho-commun** : Contient le contrat d'interface RMI et les classes modèles.

- **ho-serveur-auth** : Service d'authentification (Gestion des jetons).

- **ho-serveur-tickets** : Service de gestion des incidents (En cours).

- **ho-client** : Interface utilisateur pour interagir avec les services.

## Rôle des fichiers POM.xml

- **POM Racine** : Centralise la version du projet (`0.0.1-SNAPSHOT`) et liste les modules.

- **POM de Module** : Gère les dépendances. Par exemple, le serveur et le client importent le module `ho-commun` pour accéder aux interfaces.

- **Héritage** : Assuré par la balise `<parent>` et le `<relativePath>`, permettant une configuration uniforme.

## Procédure de lancement (Important)

Pour que le projet fonctionne, il est impératif de respecter l'ordre suivant :

1. **Compilation initiale** :
   Faire un clic droit sur le projet racine `helpops-global` > **Run As** > **Maven install**. *(Cette étape est obligatoire au moins une fois pour que les modules puissent partager le code du module commun).*

2. **Démarrage du Serveur d'Authentification** :
   Dans le module `ho-serveur-auth`, lancer la classe `ServeurAuthLanceur.java` (**Run As > Java Application**). *Le serveur va créer le registre RMI sur le port 1099 et s'y enregistrer.*

3. **Démarrage du Client** :
   Dans le module `ho-client`, lancer la classe `ClientLanceur.java` (**Run As > Java Application**). *Le client va se connecter au registre, récupérer l'objet distant et demander un jeton.*

## Logique de Répartition (RMI)

- **Le QUOI** : Interface `IAuthService` dans `ho-commun`.

- **Le COMMENT** : Classe `AuthServiceImpl` dans `ho-serveur-auth`.

- **La Sérialisation** : La classe `Incident` est sérialisable pour transiter sur le réseau.



## Structure des fichiers et conventions

Vous remarquerez que les chemins de fichiers sont longs (ex: `...\src\main\java\ho\auth\...`). Voici pourquoi :

1. **Standard Maven (`src/main/java`)** : C'est la structure universelle imposée par Maven. Elle permet de séparer proprement le code source (`java`) des fichiers de configuration (`resources`) et des tests.

2. **Gestion des Packages (`ho.auth`)** : En Java, le nom du package doit correspondre exactement à l'arborescence des dossiers

---

### Résumé :

- **Ordre de lancement** : Toujours le Serveur avant le Client.

- **Modification du Commun** : Si vous modifiez une interface dans `ho-commun`, vous **devez** refaire un `Maven install` sur la racine.

- **Erreurs Eclipse** : Si des erreurs rouges apparaissent sans raison, faites `Clic droit sur le projet > Maven > Update Project > Force Update`.
