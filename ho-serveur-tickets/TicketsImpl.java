import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ho.auth.IAuthService;

public class TicketsImpl extends UnicastRemoteObject implements ITicketsService {

    protected TicketsImpl() throws RemoteException {
        super();
    }
    @Override
    public Ticket creerTicket(String token, String description) throws Exception {
        if (token == null || !token.startsWith("Token -")) {
            throw new Exception("Token d'authentification invalide");
        }
        // Générer un ID de ticket unique
        String id = java.util.UUID.randomUUID().toString();
        return new Ticket(id, "Nouveau ticket", description);
    }
    public Ticket getTicket(String token, String id) throws Exception {
        if (token == null || !token.startsWith("Token -")) {
            throw new Exception("Token d'authentification invalide");
        }
        // A FAIRE // Modifier ceci quand on va creer la BD //
        return new Ticket(id, "Ticket existant", "Description du ticket existant");
    }
}
