package ho.client;

import java.util.Scanner;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ho.auth.IAuthService;

/**
 * Point d'entrée du client HELP'OPS.
 *
 * <p>Le client se connecte au registre RMI, récupère le service
 * d'authentification puis exécute une tentative de connexion utilisateur.</p>
 */
public class ClientLanceur {
    /**
     * Lance le client d'authentification.
     *
     * @param args arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            IAuthService authService = (IAuthService) registry.lookup("AuthService");

            System.out.println("--- Client HELP'OPS ---");

            String login;
            String password;

            try (Scanner scanner = new Scanner(System.in)) {
                System.out.print("Login : ");
                login = scanner.nextLine();

                System.out.print("Mot de passe : ");
                password = scanner.nextLine();
            }

            System.out.println("Tentative d'authentification pour l'utilisateur '" + login + "'...");

            String token = authService.login(login, password);

            if (token != null) {
                System.out.println("Connexion réussie !");
                System.out.println("Jeton de session reçu : " + token);
            } else {
                System.out.println("Erreur : Identifiants incorrects.");
            }

        } catch (Exception e) {
            System.err.println("Le client n'a pas pu contacter le serveur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}