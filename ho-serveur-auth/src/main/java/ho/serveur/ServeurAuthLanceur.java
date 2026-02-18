package ho.serveur;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ho.auth.IAuthService;

/**
 * Point d'entrée du serveur d'authentification RMI.
 */
public class ServeurAuthLanceur {
    /**
     * Démarre le registre RMI et publie le service d'authentification.
     *
     * @param args arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        try {
            Registry reg = LocateRegistry.createRegistry(1099);

            IAuthService auth = new AuthServiceImpl();
            reg.rebind("AuthService", auth);

            System.out.println(">>> Serveur d'Authentification démarré.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}