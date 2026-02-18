package ho.tickets;
import java.rmi.Remote;
import ho.modele.Ticket;

public interface ITicketsService extends Remote {
    Ticket creerTicket(String token, String description) throws Exception;
    
    Ticket getTicket(String token, String id) throws Exception;
}
