package ho.client;

import java.util.List;
import java.util.Scanner;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ho.auth.IAuthService;
import ho.modele.Ticket;
import ho.tickets.ITicketsService;

public class ClientLanceur {

    public static void main(String[] args) {
        System.out.println("--- Client HELP'OPS ---");
        try (Scanner scanner = new Scanner(System.in)) {
            // Authentification // 
            String token = gererAuthentification(scanner);
            if (token == null) {
                System.out.println("Fermeture du client.");
                return;
            }
            
            String roleToken = recupererRoleToken(token);
            
            // Boucle principale //
            lancerMenuPrincipal(scanner, token, roleToken);
            
        } catch (Exception e) {
            System.err.println("Erreur inattendue côté client : " + e.getMessage());
        }
    }

    /**
     * Gere toute la logique de connexion 
     */
    private static String gererAuthentification(Scanner scanner) {
        if (!demanderConnexion(scanner)) return null;

        int tentatives = 0;
        // nb de tentatives limitees pour eviter les boucles infinies //
        while (tentatives < 3) {
            System.out.println("\nConnexion (Tentative " + (tentatives + 1) + "/3)");
            System.out.print("Login : ");
            String login = scanner.nextLine().trim();
            System.out.print("Mot de passe : ");
            String password = scanner.nextLine().trim();

            try {
                // Tentative de connexion au service d'authentification //
                IAuthService authService = connecterAuthService();
                String token = authService.login(login, password);
                if (token != null) {
                    System.out.println("Authentification réussie.");
                    return token;
                }
                System.out.println("Utilisateur inconnu ou mot de passe incorrect.");
                tentatives++;
            } catch (Exception e) {
                System.out.println("Erreur : serveur d'authentification injoignable.");
                if (!demanderReessai(scanner)) return null;
            }
        }
        System.out.println("\n3 échecs consécutifs. Fermeture.");
        return null;
    }

    /**
     * Récupère le role associe au token d'authentification.
     * @param token Le token d'authentification.
     * @return Le rôle de l'utilisateur.
     */
    private static String recupererRoleToken(String token) {
        try {
            // se connecter au service d'authentification pour recuperer le role associe au token //
            IAuthService authService = connecterAuthService();
            return authService.getRoleToken(token);
        } catch (Exception e) {
            // Role par défaut en cas d'erreur //
            return "utilisateur"; 
        }
    }

    /*
     * Lance le menu principal selon le role de l'utilisateur.
     * @param scanner Le scanner pour lire les entrees de l'utilisateur.
     * @param token Le token d'authentification.
     * @param roleToken Le role de l'utilisateur.
     */
    private static void lancerMenuPrincipal(Scanner scanner, String token, String roleToken) {
        boolean quitter = false;
        while (!quitter) {
            // Affichage du menu selon le role //
            afficherMenu(roleToken);
            String choix = scanner.nextLine().trim();

            // Gestion de la sortie selon le role, si c'est un agent il doit saisir le 7 pour quitter, si c'est un simple utilisateur il doit saisir le 3 pour quitter //
            if ((roleToken.equalsIgnoreCase("agent") && "7".equals(choix)) || 
                (!roleToken.equalsIgnoreCase("agent") && "3".equals(choix))) {
                quitter = true;
                continue;
            }

            try {
                // Se connecter au service de tickets pour traiter l'action choisie //
                ITicketsService ticketsService = connecterTicketsService();
                traiterAction(choix, scanner, token, roleToken, ticketsService);
            } catch (Exception e) {
                System.out.println("Erreur : " + (e.getMessage() != null ? e.getMessage() : "Serveur injoignable"));
            }
        }
    }

    /*
     * Affiche le menu principal selon le role de l'utilisateur.
     * @param roleToken Le role de l'utilisateur.
     */
    private static void afficherMenu(String roleToken) {
        System.out.println("\nMenu :");
        System.out.println("1. Créer un ticket");
        System.out.println("2. Lister mes tickets");
        if (roleToken.equalsIgnoreCase("agent")) {
            System.out.println("3. Lister les tickets à ma charge");
            System.out.println("4. Afficher tous les tickets / Prendre en charge un ticket");
            System.out.println("5. Afficher les statistiques"); 
            System.out.println("6. Créer un compte");
            System.out.println("7. Quitter");
        } else {
            System.out.println("3. Quitter");
        }
        System.out.print("Choix : ");
    }

    /*
     * Traite l'action choisie par l'utilisateur en fonction de son role.
     * @param choix Le choix de l'utilisateur.
     * @param scanner Le scanner pour lire les entrees de l'utilisateur.
     * @param token Le token d'authentification.
     * @param roleToken Le role de l'utilisateur.
     * @param ticketsService Le service de tickets pour effectuer les actions liées aux tickets.
    */
    private static void traiterAction(String choix, Scanner scanner, String token, String roleToken, ITicketsService ticketsService) throws Exception {
        switch (choix) {
            case "1":
                // creation d'un ticket //
                creerTicket (scanner, token, ticketsService);
                break;

            case "2":
                // Liste des tickets de l'utilisateur //
                listerTicketsUtilisateur(scanner, token, ticketsService);
                break;

            case "3":
                // Liste des tickets assignes à l'agent //
                listerTicketsAssignes(scanner, token, ticketsService, roleToken);
                break;

            case "4":
                // Liste de tous les tickets pour les agents //
                listerTicketsAgent(scanner, token, ticketsService, roleToken);
                break;

            case "5":
                // Affichage des statistiques pour les agents //
                afficherStats(scanner, token, ticketsService, roleToken);
                break;

            case "6":
                // Creation d'un compte par  les agents //
                creerCompte(scanner, token, ticketsService, roleToken);
                break;
        }
    }
    /*
    * Gere la creation d'un ticket en demandant les informations necessaires à l'utilisateur et en appelant le service de tickets pour creer le ticket.
    * @param scanner Le scanner pour lire les entrees de l'utilisateur.
    * @param token Le token d'authentification.
    * @param ticketsService Le service de tickets pour creer le ticket.
    * @throws Exception En cas d'erreur lors de la création du ticket.
    */
    private static void creerTicket(Scanner scanner, String token, ITicketsService ticketsService) throws Exception {
        System.out.print("Titre du ticket : ");
        String titre = scanner.nextLine();
        String categorie = demanderCategorie(scanner);
        String priorite = demanderPriorite(scanner);
        System.out.print("Description du ticket : ");
        String description = scanner.nextLine();
        // Appel du service de tickets pour creer le ticket //
        Ticket nouveauTicket = ticketsService.declarerTicket(token, titre, categorie, description, priorite);
        System.out.println("Ticket créé avec succès ! ID : " + nouveauTicket.getId());
    }

    /*
    * Gere la liste des tickets d'un utilisateur en appelant le service de tickets pour recuperer les tickets et en affichant la liste avec la possibilite de voir les details d'un ticket selectionne.
    * @param scanner Le scanner pour lire les entrees de l'utilisateur.
    */
    private static void listerTicketsUtilisateur(Scanner scanner, String token, ITicketsService ticketsService) throws Exception {
        // Appel du service de tickets pour recuperer les tickets de l'utilisateur //
        List<Ticket> tickets = ticketsService.listerTickets(token);
        if (tickets == null || tickets.isEmpty()) {
            System.out.println("Vous n'avez aucun ticket.");
        } else {
            afficherListeEtGererDetails(scanner, tickets, token, ticketsService);
        }
        
    }

    /*
    * Gere la liste des tickets assignes a un agent .
    */
    private static void listerTicketsAssignes(Scanner scanner, String token, ITicketsService ticketsService, String roleToken) throws Exception {
        if (roleToken.equalsIgnoreCase("agent")) {
            // Appel du service de tickets pour recuperer les tickets assignes à l'agent //
            List<Ticket> ticketsAssignes = ticketsService.listerTicketsAssignes(token);
            if (ticketsAssignes == null || ticketsAssignes.isEmpty()) {
                System.out.println("Aucun ticket assigné.");
            } else {
                afficherListeEtGererDetails(scanner, ticketsAssignes, token, ticketsService);
            }
        }
    }

    /*
    * Gere la liste de tous les tickets pour les agents.
    * @param scanner Le scanner pour lire les entrees de l'utilisateur.
     */
    
    private static void listerTicketsAgent(Scanner scanner, String token, ITicketsService ticketsService, String roleToken) throws Exception {
        if (roleToken.equalsIgnoreCase("agent")) {
                    // Appel du service de tickets pour recuperer tous les tickets //
                    List<Ticket> tousLesTickets = ticketsService.listerTousTickets(token);
                    if (tousLesTickets == null || tousLesTickets.isEmpty()) {
                        System.out.println("Aucun ticket disponible.");
                    } else {
                        afficherListeEtGererDetails(scanner, tousLesTickets, token, ticketsService);
                    }
                }
    }

    /*
    * Gerer l'affichage des statistiques pour les agents //
    * @param scanner Le scanner pour lire les entrees de l'utilisateur.
    */
    private static void afficherStats(Scanner scanner, String token, ITicketsService ticketsService, String roleToken) throws Exception {
        if (roleToken.equalsIgnoreCase("agent")) {
                    // Appel du service de tickets pour recuperer les statistiques //
                    String statsResultat = ticketsService.afficherStatistiques(token);
                    System.out.println(statsResultat);
                }
    }


    /*
    * Gerer la creation d'un compte par les agents //
    * @param scanner Le scanner pour lire les entrees de l'utilisateur.
    */
    public static void creerCompte(Scanner scanner, String token, ITicketsService ticketsService, String roleToken) throws Exception {
        if (roleToken.equalsIgnoreCase("agent")) {
                    System.out.print("Login du nouvel utilisateur : ");
                    String loginNew = scanner.nextLine().trim();
                    System.out.print("Mot de passe : ");
                    String mdpNew = scanner.nextLine().trim();
                    System.out.print("Rôle (utilisateur/agent) : ");
                    String roleNew = scanner.nextLine().trim().toLowerCase();
                    if (!"utilisateur".equals(roleNew) && !"agent".equals(roleNew)) {
                        System.out.println("Rôle invalide.");
                        return;
                    }
                    System.out.print("Nom : ");
                    String nomNew = scanner.nextLine().trim();
                    // Appel du service d'authentification pour creer le compte //
                    IAuthService authService = connecterAuthService();
                    boolean success = authService.creerCompte(loginNew, mdpNew, nomNew, roleNew);
                    if (success) System.out.println("Compte créé !");
                    else System.out.println("Échec (login déjà pris).");
                }
    }
    /*
     * Affiche une liste de tickets et permet à l'utilisateur de voir les details d'un ticket selectionne.
     * @param scanner Le scanner pour lire les entrees de l'utilisateur.
     * @param liste La liste des tickets à afficher.
     * @param token Le token d'authentification.
     * @param ticketsService Le service de tickets.
     * @throws Exception En cas d'erreur lors de l'affichage des détails.
     */
    private static void afficherListeEtGererDetails(Scanner scanner, List<Ticket> liste, String token, ITicketsService ticketsService) throws Exception {
        System.out.println("\nListe des tickets :");
        for (int i = 0; i < liste.size(); i++) {
            Ticket t = liste.get(i);
            System.out.println((i + 1) + ". [" + t.getEtat() + "] [Priorité: " + valeurOuDefaut(t.getPriorite()) + "] " + t.getTitre() + " (ID: " + t.getId() + ")");
        }

        while (true) {
            System.out.print("\nDétail ? (numéro, 0 pour retour) : ");
            String saisie = scanner.nextLine().trim();
            if ("0".equals(saisie)) break;

            try {
                int index = Integer.parseInt(saisie) - 1;
                if (index >= 0 && index < liste.size()) {
                    Ticket selection = liste.get(index);
                    IAuthService authService = connecterAuthService();
                    String roleConnecte = authService.getRoleToken(token);
                    String idConnecte = authService.getIdUtilisateur(token);

                    System.out.println("\n--- Détail du ticket ---");
                    System.out.println("ID         : " + selection.getId());
                    System.out.println("Titre      : " + selection.getTitre());
                    System.out.println("État       : " + selection.getEtat());
                    System.out.println("Priorité   : " + valeurOuDefaut(selection.getPriorite()));
                    System.out.println("Créateur   : " + resoudreNomUtilisateur(authService, selection.getIdCreateur()));
                    System.out.println("Description: " + selection.getDescription());
                    
                    // Si un agent est assigne, on affiche son nom //
                    if (selection.getIdAgent() != null && !selection.getIdAgent().isBlank()) {
                        System.out.println("Agent      : " + resoudreNomUtilisateur(authService, selection.getIdAgent()));
                    }

                    if ("agent".equalsIgnoreCase(roleConnecte)) {
                        // Le ticket est OPEN //
                        if ("OPEN".equals(selection.getEtat())) {
                            System.out.println("\n1. Prendre en charge ce ticket");
                            System.out.println("2. Retour");
                            System.out.print("Choix : ");
                            if ("1".equals(scanner.nextLine().trim())) {
                                if (ticketsService.prendreEnCharge(token, selection.getId())) {
                                    System.out.println("Ticket pris en charge !");
                                }
                            }
                        } 
                        // Le ticket est ASSIGNED à l'agent actuel //
                        else if ("ASSIGNED".equals(selection.getEtat()) && idConnecte.equals(selection.getIdAgent())) {
                            System.out.println("\n1. Libérer ce ticket");
                            System.out.println("2. Résoudre ce ticket");
                            System.out.println("3. Retour");
                            System.out.print("Choix : ");
                            String choixAction = scanner.nextLine().trim();
                            
                            if ("1".equals(choixAction)) {
                                if (ticketsService.libererTicket(token, selection.getId())) {
                                    System.out.println("Ticket libéré !");
                                }
                            } else if ("2".equals(choixAction)) {
                                System.out.print("Message de résolution : ");
                                String msg = scanner.nextLine();
                                if (ticketsService.resoudreTicket(token, selection.getId(), msg)) {
                                    System.out.println("Ticket résolu !");
                                }
                            }
                        }
                    }
                    break;
                } else {
                    System.out.println("Numéro invalide.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Saisie invalide (entrez un nombre).");
            }
        }
    }

    /*
     * Connecte au service d'authentification.
     * @return L'instance du service d'authentification.
     * @throws Exception En cas d'erreur de connexion.
     */
    private static IAuthService connecterAuthService() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        return (IAuthService) registry.lookup("AuthService");
    }

    /*
     * Connecte au service de gestion des tickets.
     * @return L'instance du service de gestion des tickets.
     * @throws Exception En cas d'erreur de connexion.
     */
    private static ITicketsService connecterTicketsService() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        return (ITicketsService) registry.lookup("TicketsService");
    }

    /*
     * Demande à l'utilisateur s'il souhaite reessayer.
     * @param scanner Le scanner pour lire les entrees de l'utilisateur.
     * @return true si l'utilisateur souhaite réessayer, false sinon.
     */
    private static boolean demanderReessai(Scanner scanner) {
        System.out.println("1. Réessayer\n2. Quitter\nChoix : ");
        return "1".equals(scanner.nextLine().trim());
    }

    /*
     * Demande à l'utilisateur s'il souhaite se connecter.
     * @param scanner Le scanner pour lire les entrees de l'utilisateur.
     * @return true si l'utilisateur souhaite se connecter, false sinon.
     */
    private static boolean demanderConnexion(Scanner scanner) {
        while (true) {
            System.out.println("1. Se connecter\n2. Quitter\nChoix : ");
            String choix = scanner.nextLine().trim();
            if ("1".equals(choix)) return true;
            if ("2".equals(choix)) return false;
        }
    }

    /*
     * Demande à l'utilisateur la categorie du ticket.
     * @param scanner Le scanner pour lire les entrees de l'utilisateur.
     * @return La categorie choisie.
     */
    private static String demanderCategorie(Scanner scanner) {
        System.out.println("Catégorie : 1. Incident, 2. Demande");
        return "1".equals(scanner.nextLine().trim()) ? "incident" : "demande";
    }

    /*
     * Demande à l'utilisateur la priorite du ticket.
     * @param scanner Le scanner pour lire les entrees de l'utilisateur.
     * @return La priorité choisie.
     */
    private static String demanderPriorite(Scanner scanner) {
        while (true) { 
            System.out.println("\nPriorité du ticket :");
            System.out.println("1. Haute");
            System.out.println("2. Moyenne");
            System.out.println("3. Basse");
            System.out.print("Votre choix (1, 2 ou 3) : ");
            
            String choix = scanner.nextLine().trim();

            if ("1".equals(choix)) {
                return "HAUTE";
            } else if ("2".equals(choix)) {
                return "MOYENNE";
            } else if ("3".equals(choix)) {
                return "BASSE";
            } else {
                System.out.println("Choix invalide ! Veuillez saisir 1, 2 ou 3.");
            }
        }
    }
    /*
     * Résout le nom d'un utilisateur a partir de son ID.
     * @param authService Le service d'authentification.
     * @param id L'ID de l'utilisateur.
     * @return Le nom de l'utilisateur.
     */
    private static String resoudreNomUtilisateur(IAuthService authService, String id) {
        try { return authService.getNomUtilisateurParId(id); } 
        catch (Exception e) { return "inconnu"; }
    }
    /*
     * Retourne la valeur d'une chaîne de caracteres ou une valeur par defaut si elle est nulle.
     * @param v La chaîne de caracteres a verifier.
     * @return La chaîne de caracteres ou la valeur par defaut.
     */
    private static String valeurOuDefaut(String v) {
        if (v == null) return "Non définie";
        return v;
    }
}