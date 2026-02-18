package ho.client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ho.auth.IAuthService;

public class ClientLanceur {
    public static void main(String[] args) {
        try {
            // 1. Connexion à l'annuaire RMI (le Registry) sur le port 1099
            // "localhost" car le serveur tourne sur ta machine
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            // 2. Recherche du service d'authentification par son nom
            // On cast le résultat en IAuthService (le contrat connu via ho-commun)
            IAuthService authService = (IAuthService) registry.lookup("AuthService");

            // 3. Test de connexion
            System.out.println("--- Client HELP'OPS ---");
            System.out.println("Tentative d'authentification pour l'utilisateur 'admin'...");
            
            String token = authService.login("admin", "123");

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