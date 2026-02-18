package fr.m1rt.helpops.auth;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Lanceur RMI : crée le registre et enregistre l'objet distant.
 */
public class ServeurAuthLanceur {
    public static void main(String[] args) {
        try {
            // Création de l'annuaire (Registry) sur le port 1099
            Registry reg = LocateRegistry.createRegistry(1099);
            
            // Instanciation et publication du service
            IAuthService auth = new AuthServiceImpl();
            reg.rebind("AuthService", auth);
            
            System.out.println(">>> Serveur d'Authentification démarré.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}