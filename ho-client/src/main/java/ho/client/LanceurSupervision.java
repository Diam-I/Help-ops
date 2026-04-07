package ho.client;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

import ho.supervision.ISupervisionClient;
import ho.tickets.ITicketsService;

public class LanceurSupervision extends UnicastRemoteObject implements ISupervisionClient {

    protected LanceurSupervision() throws java.rmi.RemoteException {
        super();
    }

    @Override
    public void notifierEvenement(String evenement) throws java.rmi.RemoteException {
        System.out.println("Événement de supervision  : " + evenement);
    }

    public static void main(String[] args) {
        try {
            ITicketsService service = (ITicketsService) LocateRegistry.getRegistry(1099).lookup("TicketsService");
            LanceurSupervision supervisionClient = new LanceurSupervision();
            System.out.println("Client de supervision prêt.");
            // choisir si le client veut récupérer les événements passés ou seulement les nouveaux // 
            Scanner scanner = new Scanner(System.in);
            System.out.println("Voulez-vous récupérer les événements passés ? (oui/non) : ");
            String reponse = scanner.nextLine().trim().toLowerCase();   
            while (!reponse.equals("oui") && !reponse.equals("non")) {
                System.out.println("Veuillez répondre par 'oui' ou 'non' : ");
                reponse = scanner.nextLine().trim().toLowerCase();
            }
            if (reponse.equals("oui")) {
                service.sabonner(supervisionClient, true);
            } else if (reponse.equals("non")) {
                service.sabonner(supervisionClient, false);
            }   

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
