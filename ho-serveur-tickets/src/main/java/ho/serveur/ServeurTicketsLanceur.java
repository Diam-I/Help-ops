package ho.serveur;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ho.tickets.ITicketsService;

/**
 * Point d'entrée du serveur de tickets.
 *
 * Publie le service `TicketsService` dans le registre RMI.
 */
public class ServeurTicketsLanceur {
    /**
     * Publie le service de gestion des tickets dans un registre RMI déjà démarré.
     *
     * @param args arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        try {
            System.setProperty("java.rmi.server.hostname", "localhost");

            Registry reg = LocateRegistry.getRegistry(1099);
            reg.list();
            System.out.println(">>> Registre RMI existant trouvé.");

            ITicketsService tickets = new TicketsImpl();
            reg.rebind("TicketsService", tickets);

            System.out.println(">>> Serveur de Tickets démarré.");
        } catch (Exception e) {
            System.err.println("Erreur lors demarrage du serveur de tickets (registre RMI indisponible ?) :");
            e.printStackTrace();
        }
    }
}