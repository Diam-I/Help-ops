package ho.supervision;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ISupervisionClient extends Remote {
    void notifierEvenement(String evenement) throws RemoteException ;    
}
