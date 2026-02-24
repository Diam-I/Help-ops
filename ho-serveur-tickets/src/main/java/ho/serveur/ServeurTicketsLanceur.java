package ho.serveur;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ho.tickets.ITicketsService;

/**
 * Point d'entrée du serveur de tickets.
 *
 * <p>Publie le service `TicketsService` dans le registre RMI.</p>
 */
public class ServeurTicketsLanceur {
    /**
     * Démarre le registre RMI et publie le service de gestion des tickets.
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
                reg.list(); 
                System.out.println(">>> Registre RMI existant trouvé.");
            } catch (Exception e) {
                reg = LocateRegistry.createRegistry(1099);
                System.out.println(">>> Registre RMI créé sur le port 1099.");
            }

            ITicketsService tickets = new TicketsImpl();
            reg.rebind("TicketsService", tickets);

            System.out.println(">>> Serveur de Tickets démarré.");
        } catch (Exception e) {
            System.err.println("Erreur lors démarrage du serveur de tickets :");
            e.printStackTrace();
        }
    }
}