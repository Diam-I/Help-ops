package ho.supervision;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ISupervisionClient extends Remote {
    /**
     * Notifie un evenement a tous les clients de supervision.
     * @param evenement l'evenement a notifier
     * @throws RemoteException en cas d'erreur de communication avec le client de supervision
     */
    void notifierEvenement(String evenement) throws RemoteException ;    
}
