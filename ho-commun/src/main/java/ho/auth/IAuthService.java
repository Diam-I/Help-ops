package ho.auth;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface RMI : définit le contrat d'usage.
 * Chaque méthode doit lever une RemoteException en cas de problème réseau.
 */
public interface IAuthService extends Remote {
    /**
     * @return un jeton (token) String si authentifié, null sinon.
     */
    String login(String login, String password) throws RemoteException;
}