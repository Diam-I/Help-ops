package ho.client;

import java.util.List;
import java.util.Scanner;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ho.auth.IAuthService;
import ho.modele.Ticket;
import ho.tickets.ITicketsService;

/**
 * Point d'entrée du client HELP'OPS.
 *
 * <p>Lance le menu console et appelle les services RMI
 * d'authentification et de tickets selon les actions de l'utilisateur.</p>
 */
public class ClientLanceur {

    /**
        * Démarre le client puis affiche le menu principal après authentification.
     *
     * @param args arguments de ligne de commande (non utilisés)
     * 
     */
    public static void main(String[] args) {
        System.out.println("--- Client HELP'OPS ---");

        try (Scanner scanner = new Scanner(System.in)) {
            if (!demanderConnexion(scanner)) {
                System.out.println("Fermeture du client.");
                return;
            }

            String token = null;
            String roleToken = "utilisateur";
            int tentatives = 0;

            while (tentatives < 3 && token == null) {
                System.out.println("\nConnexion (Tentative " + (tentatives + 1) + "/3)");
                System.out.print("Login : ");
                String loginSaisie = scanner.nextLine().trim();
                System.out.print("Mot de passe : ");
                String passwordSaisie = scanner.nextLine().trim();

                try {
                    IAuthService authService = connecterAuthService();
                    System.out.println("Tentative d'authentification pour l'utilisateur '" + loginSaisie + "'...");
                    token = authService.login(loginSaisie, passwordSaisie);
                    if (token != null) {
                        try {
                            roleToken = authService.getRoleToken(token);
                        } catch (Exception ex) {
                            roleToken = "utilisateur";
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Erreur : serveur d'authentification injoignable.");
                    if (!demanderReessai(scanner)) {
                        System.out.println("Fermeture du client.");
                        return;
                    }
                    continue;
                }

                if (token != null) {
                    System.out.println("Authentification réussie.");
                } else {
                    System.out.println("Utilisateur inconnu ou mot de passe incorrect.");
                    tentatives++;
                }
            }

            if (token == null) {
                System.out.println("\n3 échecs consécutifs. Fermeture.");
                return;
            }

            boolean quitter = false;
            while (!quitter) {
                System.out.println("\nMenu :");
                System.out.println("1. Créer un ticket");
                System.out.println("2. Lister mes tickets");
                if (roleToken.equalsIgnoreCase("agent")) {
                    System.out.println("3. Lister les tickets à ma charge");
                    System.out.println("4. Afficher tous les tickets / Prendre en charge un ticket");
                    System.out.println("5. Resoudre un ticket");
                    System.err.println("6. Afficher les statistiques"); 
                    System.out.println("7. Quitter");
                } else {
                    System.out.println("3. Quitter");
                }
                System.out.print("Choix : ");

                String choix = scanner.nextLine().trim();
                try {
                    ITicketsService ticketsService = connecterTicketsService();
                    switch (choix) {
                        case "1":
                            System.out.print("Titre du ticket : ");
                            String titre = scanner.nextLine();
                            String categorie = demanderCategorie(scanner);
                            System.out.print("Description du ticket : ");
                            String description = scanner.nextLine();
                            Ticket nouveauTicket = ticketsService.declarerTicket(token, titre, categorie, description);
                            System.out.println("Ticket créé avec succès ! ID : " + nouveauTicket.getId());
                            break;
                        case "2":

                            List<Ticket> tickets = ticketsService.listerTickets(token);
                            if (tickets == null || tickets.isEmpty()) {
                                System.out.println("Vous n'avez aucun ticket.");
                            } else {
                                System.out.println("Vos tickets :");
                                for (int i = 0; i < tickets.size(); i++) {
                                    Ticket t = tickets.get(i);
                                    System.out.println((i + 1) + ". [" + t.getEtat() + "] " + t.getTitre() + " (ID: " + t.getId() + ")");
                                }

                                while (true) {
                                    System.out.print("Quel ticket afficher en détail ? (numéro, 0 pour annuler) : ");
                                    String saisie = scanner.nextLine().trim();

                                    if ("0".equals(saisie)) {
                                        break;
                                    }

                                    try {
                                        int index = Integer.parseInt(saisie) - 1;
                                        if (index >= 0 && index < tickets.size()) {
                                            Ticket selection = tickets.get(index);
                                            IAuthService authService = connecterAuthService();
                                            String nomCreateur = resoudreNomUtilisateur(authService, selection.getIdCreateur());
                                            String nomAgent = "Non assigné";
                                            if (selection.getIdAgent() != null && !selection.getIdAgent().isBlank()) {
                                                nomAgent = resoudreNomUtilisateur(authService, selection.getIdAgent());
                                            }

                                            System.out.println("\n--- Détail du ticket ---");
                                            System.out.println("ID : " + selection.getId());
                                            System.out.println("Titre : " + selection.getTitre());
                                            System.out.println("Catégorie : " + selection.getCategorie());
                                            System.out.println("État : " + selection.getEtat());
                                            System.out.println("Date de création : " + selection.getDateCreation());
                                            String dateAssignation = selection.getDateAssignation();
                                            if (dateAssignation == null || dateAssignation.isBlank()) {
                                                dateAssignation = "Non assigné";
                                            }
                                            System.out.println("Date d'assignation : " + dateAssignation);
                                            System.out.println("Nom du créateur : " + nomCreateur);
                                            System.out.println("Agent assigné : " + nomAgent);
                                            System.out.println("Description : " + selection.getDescription());
                                            break;
                                        }
                                        System.out.println("Numéro invalide, veuillez réessayer.");
                                    } catch (NumberFormatException e) {
                                        System.out.println("Veuillez saisir un numéro valide.");
                                    }
                                }
                            }
                            break;
                        case "3":
                            if (roleToken.equalsIgnoreCase("agent")) {
                                List<Ticket> ticketsAssignes = ticketsService.listerTicketsAssignes(token);
                                if (ticketsAssignes == null || ticketsAssignes.isEmpty()) {
                                    System.out.println("Aucun ticket assigné.");
                                } else {
                                    System.out.println("Tickets assignés :");
                                    for (int i = 0; i < ticketsAssignes.size(); i++) {
                                        Ticket t = ticketsAssignes.get(i);
                                        System.out.println((i + 1) + ". [" + t.getEtat() + "] " + t.getTitre() + " (ID: " + t.getId() + ")");
                                    }

                                    while (true) {
                                        System.out.print("Quel ticket assigné afficher en détail ? (numéro, 0 pour annuler) : ");
                                        String saisie = scanner.nextLine().trim();

                                        if ("0".equals(saisie)) {
                                            break;
                                        }

                                        try {
                                            int index = Integer.parseInt(saisie) - 1;
                                            if (index >= 0 && index < ticketsAssignes.size()) {
                                                Ticket selection = ticketsAssignes.get(index);
                                                IAuthService authService = connecterAuthService();
                                                String idAgentConnecte = authService.getIdUtilisateur(token);
                                                String nomCreateur = resoudreNomUtilisateur(authService, selection.getIdCreateur());
                                                String nomAgent = "Non assigné";
                                                if (selection.getIdAgent() != null && !selection.getIdAgent().isBlank()) {
                                                    nomAgent = resoudreNomUtilisateur(authService, selection.getIdAgent());
                                                }

                                                System.out.println("\n--- Détail du ticket assigné ---");
                                                System.out.println("ID : " + selection.getId());
                                                System.out.println("Titre : " + selection.getTitre());
                                                System.out.println("Catégorie : " + selection.getCategorie());
                                                System.out.println("État : " + selection.getEtat());
                                                System.out.println("Date de création : " + selection.getDateCreation());
                                                String dateAssignation = selection.getDateAssignation();
                                                if (dateAssignation == null || dateAssignation.isBlank()) {
                                                    dateAssignation = "Non assigné";
                                                }
                                                System.out.println("Date d'assignation : " + dateAssignation);
                                                System.out.println("Nom du créateur : " + nomCreateur);
                                                System.out.println("Agent assigné : " + nomAgent);
                                                System.out.println("Description : " + selection.getDescription());

                                                if (selection.getIdAgent() != null
                                                        && !selection.getIdAgent().isBlank()
                                                        && selection.getIdAgent().equals(idAgentConnecte)) {
                                                    System.out.println("1. Libérer ce ticket");
                                                    System.out.println("2. Retour");
                                                    System.out.print("Choix : ");
                                                    String choixAction = scanner.nextLine().trim();
                                                    if ("1".equals(choixAction)) {
                                                        boolean success = ticketsService.libererTicket(token, selection.getId());
                                                        if (success) {
                                                            System.out.println("Ticket '" + selection.getTitre() + "' libéré avec succès !");
                                                        } else {
                                                            System.out.println("Échec de la libération du ticket.");
                                                        }
                                                    }
                                                }
                                                break;
                                            }
                                            System.out.println("Numéro invalide, veuillez réessayer.");
                                        } catch (NumberFormatException e) {
                                            System.out.println("Veuillez saisir un numéro valide.");
                                        }
                                    }
                                }
                            } else {
                                quitter = true;
                            }
                            break;
                        case "4":
                            if (roleToken.equalsIgnoreCase("agent")) {
                                List<Ticket> tousLesTickets = ticketsService.listerTousTickets(token);
                                if (tousLesTickets == null || tousLesTickets.isEmpty()) {
                                    System.out.println("Aucun ticket disponible.");
                                } else {
                                    System.out.println("Tous les tickets :");
                                    for (int i = 0; i < tousLesTickets.size(); i++) {
                                        Ticket t = tousLesTickets.get(i);
                                        System.out.println((i + 1) + ". [" + t.getEtat() + "] " + t.getTitre() + " (ID: " + t.getId() + ")");
                                    }

                                    while (true) {
                                        System.out.print("Quel ticket afficher en détail ? (numéro, 0 pour retour) : ");
                                        String saisie = scanner.nextLine().trim();

                                        if ("0".equals(saisie)) {
                                            break;
                                        }

                                        try {
                                            int index = Integer.parseInt(saisie) - 1;
                                            if (index >= 0 && index < tousLesTickets.size()) {
                                                Ticket selection = tousLesTickets.get(index);
                                                IAuthService authService = connecterAuthService();
                                                String idAgentConnecte = authService.getIdUtilisateur(token);
                                                String nomCreateur = resoudreNomUtilisateur(authService, selection.getIdCreateur());
                                                String nomAgent = "Non assigné";
                                                if (selection.getIdAgent() != null && !selection.getIdAgent().isBlank()) {
                                                    nomAgent = resoudreNomUtilisateur(authService, selection.getIdAgent());
                                                }

                                                System.out.println("\n--- Détail du ticket ---");
                                                System.out.println("ID : " + selection.getId());
                                                System.out.println("Titre : " + selection.getTitre());
                                                System.out.println("Catégorie : " + selection.getCategorie());
                                                System.out.println("État : " + selection.getEtat());
                                                System.out.println("Date de création : " + selection.getDateCreation());
                                                String dateAssignation = selection.getDateAssignation();
                                                if (dateAssignation == null || dateAssignation.isBlank()) {
                                                    dateAssignation = "Non assigné";
                                                }
                                                System.out.println("Date d'assignation : " + dateAssignation);
                                                System.out.println("Nom du créateur : " + nomCreateur);
                                                System.out.println("Agent assigné : " + nomAgent);
                                                System.out.println("Description : " + selection.getDescription());

                                                boolean assigneAAgentConnecte = selection.getIdAgent() != null
                                                        && !selection.getIdAgent().isBlank()
                                                        && selection.getIdAgent().equals(idAgentConnecte);

                                                if (assigneAAgentConnecte) {
                                                    System.out.println("1. Libérer ce ticket");
                                                    System.out.println("2. Retour");
                                                    System.out.print("Choix : ");
                                                    String choixAction = scanner.nextLine().trim();
                                                    if ("1".equals(choixAction)) {
                                                        boolean success = ticketsService.libererTicket(token, selection.getId());
                                                        if (success) {
                                                            System.out.println("Ticket '" + selection.getTitre() + "' libéré avec succès !");
                                                        } else {
                                                            System.out.println("Échec de la libération du ticket.");
                                                        }
                                                    }
                                                } else {
                                                    System.out.println("1. Prendre en charge ce ticket");
                                                    System.out.println("2. Retour");
                                                    System.out.print("Choix : ");
                                                    String choixAction = scanner.nextLine().trim();

                                                    if ("1".equals(choixAction)) {
                                                        boolean success = ticketsService.prendreEnCharge(token, selection.getId());
                                                        if (success) {
                                                            System.out.println("Ticket '" + selection.getTitre() + "' pris en charge avec succès !");
                                                        } else {
                                                            System.out.println("Échec de la prise en charge du ticket.");
                                                        }
                                                    }
                                                }
                                                break;
                                            }
                                            System.out.println("Numéro invalide, veuillez réessayer.");
                                        } catch (NumberFormatException e) {
                                            System.out.println("Veuillez saisir un numéro valide.");
                                        }
                                    }
                                }
                            } else {
                                System.out.println("Choix invalide, veuillez réessayer.");
                            }
                            break;
                        case "5":
                            if (roleToken.equalsIgnoreCase("agent")) {
                                System.out.print("Entrez l'ID du ticket à résoudre : ");
                                String idTicket = scanner.nextLine().trim();
                                boolean success = ticketsService.resoudreTicket(token, idTicket);
                                if (success) {
                                    System.out.println("Ticket ID '" + idTicket + "' résolu avec succès !");
                                } else {
                                    System.out.println("Échec de la résolution du ticket. Vérifiez l'ID et assurez-vous que le ticket est assigné à vous.");
                                }
                            } else {
                                System.out.println("Choix invalide, veuillez réessayer.");
                            }
                            break;
                        case "6":
                            if (roleToken.equalsIgnoreCase("agent")) {
                                quitter = true;
                            } else {
                                System.out.println("Choix invalide, veuillez réessayer.");
                            }
                            break;
                        default:
                            System.out.println("Choix invalide, veuillez réessayer.");
                    }
                } catch (Exception e) {
                    String message = e.getMessage();
                    if (message == null || message.isBlank()) {
                        System.out.println("Erreur : serveur de tickets injoignable.");
                    } else {
                        System.out.println("Erreur : " + message);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur inattendue côté client : " + e.getMessage());
        }
    }

    /**
        * Récupère la référence distante du service d'authentification.
        * 
        * @return référence RMI vers IAuthService
        * @throws Exception si le service n'est pas trouvé ou en cas d'erreur de communication
        * 
     */
    private static IAuthService connecterAuthService() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        return (IAuthService) registry.lookup("AuthService");
    }

    /**
        * Récupère la référence distante du service de tickets.
        * 
        * @return référence RMI vers ITicketsService
        * @throws Exception si le service n'est pas trouvé ou en cas d'erreur de communication
        *
     */
    private static ITicketsService connecterTicketsService() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        return (ITicketsService) registry.lookup("TicketsService");
    }

    /**
        * Demande à l'utilisateur s'il souhaite retenter l'opération.
        * 
        * @param scanner Scanner pour lire la saisie utilisateur
        * @return {@code true} si l'utilisateur souhaite réessayer, sinon {@code false}
        *  
     */
    private static boolean demanderReessai(Scanner scanner) {
        System.out.println("1. Réessayer");
        System.out.println("2. Quitter");
        System.out.print("Choix : ");
        String choix = scanner.nextLine().trim();
        return "1".equals(choix);
    }

    /**
        * Affiche le menu de démarrage du client.
        * 
        * @return {@code true} si l'utilisateur souhaite se connecter, sinon {@code false} pour quitter
        * 
     */
    private static boolean demanderConnexion(Scanner scanner) {
        while (true) {
            System.out.println("1. Se connecter");
            System.out.println("2. Quitter");
            System.out.print("Choix : ");
            String choix = scanner.nextLine().trim();

            if ("1".equals(choix)) {
                return true;
            }
            if ("2".equals(choix)) {
                return false;
            }

            System.out.println("Choix invalide, veuillez réessayer.");
        }
    }

    /**
        * Force le choix d'une catégorie métier valide.
        * 
        * @param scanner Scanner pour lire la saisie utilisateur
        * @return "incident" ou "demande" selon le choix de l'utilisateur
        *
     */
    private static String demanderCategorie(Scanner scanner) {
        while (true) {
            System.out.println("Categorie du ticket :");
            System.out.println("1. Incident");
            System.out.println("2. Demande");
            System.out.print("Choix : ");
            String choix = scanner.nextLine().trim();

            if ("1".equals(choix)) {
                return "incident";
            }
            if ("2".equals(choix)) {
                return "demande";
            }

            System.out.println("Choix invalide, veuillez réessayer.");
        }
    }

    /**
     * Résout le nom utilisateur via le service d'authentification à partir d'un identifiant.
     */
    private static String resoudreNomUtilisateur(IAuthService authService, String idUtilisateur) {
        if (authService == null || idUtilisateur == null || idUtilisateur.isBlank()) {
            return "inconnu";
        }
        try {
            String nom = authService.getNomUtilisateurParId(idUtilisateur);
            return (nom == null || nom.isBlank()) ? "inconnu" : nom;
        } catch (Exception e) {
            return "inconnu";
        }
    }
}