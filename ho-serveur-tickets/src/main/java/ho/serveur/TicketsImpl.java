package ho.serveur;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import ho.modele.Ticket;
import ho.tickets.ITicketsService;

public class TicketsImpl extends UnicastRemoteObject implements ITicketsService {

    protected TicketsImpl() throws RemoteException {
        super();
    }

    @Override
    public Ticket getTicket(String token, String id) throws RemoteException {
        if (token == null || !token.startsWith("Token -")) {
            throw new RemoteException("Token d'authentification invalide");
        }
        /////// BD :  Modifier ceci quand on va creer la BD !!!!!!!!!! ////////
        return new Ticket(id, "Probleme MDP", "materiels", "Mdp ne veut pas se modifier", "1");
    }

    public void afficherTicket(Ticket ticket) {
        System.out.println("ID: " + ticket.getId());
        System.out.println("Titre: " + ticket.getTitre());
        System.out.println("Catégorie: " + ticket.getCategorie());
        System.out.println("Description: " + ticket.getDescription());
        System.out.println("Etat: " + ticket.getEtat());
        System.out.println("Date de création: " + ticket.getDateCreation());
        System.out.println("ID créateur: " + ticket.getIdCreateur());
    }

    public Ticket declarerTicket(String token, String titre, String description) throws RemoteException {
        if (token == null || !token.startsWith("Token -")) {
            throw new RemoteException("Token d'authentification invalide");
        }
        // Generer un ID de ticket unique // 
        ////// BD :  Modifier ceci quand on va creer la BD !!!!!!!!!! ////////
        String id = java.util.UUID.randomUUID().toString();
        return new Ticket(id, titre, description, "materiels", "1");
    }

    public List<Ticket> listerTickets(String token) throws RemoteException {
        if (token == null || !token.startsWith("Token -")) {
            throw new RemoteException("Token d'authentification invalide");
        }
        ////// BD :  Modifier ceci quand on va creer la BD !!!!!!!!!! ////////
        return List.of(
            new Ticket("1", "Ticket 1", "Description du ticket 1", "materiels", "1"),
            new Ticket("2", "Ticket 2", "Description du ticket 2", "materiels", "1")
        );
    }
}