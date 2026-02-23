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
 * Le client se connecte au registre RMI, récupère le service
 * d'authentification puis exécute une tentative de connexion utilisateur.</p>
 */
public class ClientLanceur {
    /**
     * Lance le client d'authentification puis affiche un menu de gestion des tickets après une connexion réussie
     *
     * @param args arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            IAuthService authService = (IAuthService) registry.lookup("AuthService");
            ITicketsService ticketsService = (ITicketsService) registry.lookup("TicketsService");

            System.out.println("--- Client HELP'OPS ---");

            try (Scanner scanner = new Scanner(System.in)) {
                String token = null;
                int tentatives = 0;

                while (tentatives < 3 && token == null) {
                    System.out.println("\n Connexion (Tentative " + (tentatives + 1) + "/3)");
                    System.out.print("Login : ");
                    String loginSaisie = scanner.nextLine().trim();
                    System.out.print("Mot de passe : ");
                    String passwordSaisie = scanner.nextLine().trim();

                    System.out.println("Tentative d'authentification pour l'utilisateur '" + loginSaisie + "'...");
                    token = authService.login(loginSaisie, passwordSaisie);

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
                    switch (choix) {
                        case "1":
                            System.out.print("Titre du ticket : ");
                            String titre = scanner.nextLine();
                            System.out.print("Description du ticket : ");
                            String description = scanner.nextLine();
                            Ticket nouveauTicket = ticketsService.declarerTicket(token, titre, description);
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
                                    System.out.println("- " + t.getTitre() + " [" + t.getEtat() + "] : " + t.getDescription());
                                }
                            }
                            break;
                        case "3":
                            quitter = true;
                            break;
                        default:
                            System.out.println("Choix invalide, veuillez réessayer.");
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Le client n'a pas pu contacter le serveur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}