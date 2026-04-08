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
        System.out.println("Événement  : " + evenement);
    }

    /*
    /* Le client de supervision peut choisir de recupere les evenements passes ou seulement les nouveaux
    */
    public static void main(String[] args) {
        try {
            // se connecter au service de tickets //
            ITicketsService service = (ITicketsService) LocateRegistry.getRegistry(1099).lookup("TicketsService");
            LanceurSupervision supervisionClient = new LanceurSupervision();
            System.out.println("Client de supervision prêt.");
            // choisir si le client veut recuperer les evenements passes ou seulement les nouveaux // 
            Scanner scanner = new Scanner(System.in);
            System.out.println("Voulez-vous recuperer les evenements passes ? (oui/non) : ");
            String reponse = scanner.nextLine().trim().toLowerCase();   
            while (!reponse.equals("oui") && !reponse.equals("non")) {
                System.out.println("Veuillez repondre par 'oui' ou 'non' : ");
                reponse = scanner.nextLine().trim().toLowerCase();
            }
            // s'abonner au service de supervision en fonction du choix de l'utilisateur //
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
