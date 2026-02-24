package ho.serveur;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ho.auth.IAuthService;

/**
 * Point d'entrée du serveur d'authentification RMI.
 *
 * <p>Publie le service `AuthService` dans le registre RMI.</p>
 */
public class ServeurAuthLanceur {
    /**
     * Démarre le registre RMI et publie le service d'authentification.
     *
     * @param args arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        try {
            System.setProperty("java.rmi.server.hostname", "localhost");

            // Récupère le registre RMI, ou le crée s'il n'existe pas.
            Registry reg;
            try {
                reg = LocateRegistry.getRegistry(1099);
                reg.list(); // Teste si le registre répond vraiment
                System.out.println(">>> Registre RMI existant trouvé.");
            } catch (Exception e) {
                reg = LocateRegistry.createRegistry(1099);
                System.out.println(">>> Registre RMI créé sur le port 1099.");
            }
            IAuthService auth = new AuthServiceImpl();
            reg.rebind("AuthService", auth);

            System.out.println(">>> Serveur d'Authentification démarré.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}