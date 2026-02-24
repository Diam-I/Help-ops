package ho.tickets;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import ho.modele.Ticket;

public interface ITicketsService extends Remote {
    Ticket getTicket(String token, String id) throws RemoteException;
    List<Ticket> listerTickets(String token) throws RemoteException;
    Ticket declarerTicket(String token, String titre, String categorie, String description) throws RemoteException;
}
