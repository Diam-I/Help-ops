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
     */
    public static void main(String[] args) {
        System.out.println("--- Client HELP'OPS ---");

        try (Scanner scanner = new Scanner(System.in)) {
            if (!demanderConnexion(scanner)) {
                System.out.println("Fermeture du client.");
                return;
            }

            String token = null;
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
                System.out.println("3. Quitter");
                System.out.print("Choix : ");

                String choix = scanner.nextLine();
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
                                    System.out.println((i + 1) + ". " + t.getId() + " " + t.getTitre() + " [" + t.getEtat() + "]");
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
                                            System.out.println("\n--- Détail du ticket ---");
                                            System.out.println("ID : " + selection.getId());
                                            System.out.println("Titre : " + selection.getTitre());
                                            System.out.println("Catégorie : " + selection.getCategorie());
                                            System.out.println("État : " + selection.getEtat());
                                            System.out.println("Date de création : " + selection.getDateCreation());
                                            System.out.println("ID créateur : " + selection.getIdCreateur());
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
                            quitter = true;
                            break;
                        default:
                            System.out.println("Choix invalide, veuillez réessayer.");
                    }
                } catch (Exception e) {
                    System.out.println("Erreur : serveur de tickets injoignable.");
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur inattendue côté client : " + e.getMessage());
        }
    }

    /**
        * Récupère la référence distante du service d'authentification.
     */
    private static IAuthService connecterAuthService() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        return (IAuthService) registry.lookup("AuthService");
    }

    /**
        * Récupère la référence distante du service de tickets.
     */
    private static ITicketsService connecterTicketsService() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        return (ITicketsService) registry.lookup("TicketsService");
    }

    /**
        * Demande à l'utilisateur s'il souhaite retenter l'opération.
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
}