package ho.auth;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Contrat du service d'authentification exposé via RMI.
 */
public interface IAuthService extends Remote {
    /**
     * Authentifie un utilisateur à partir de ses identifiants.
     *
     * @param login identifiant utilisateur
     * @param password mot de passe utilisateur
     * @return un jeton de session si l'authentification réussit, sinon {@code null}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    String login(String login, String password) throws RemoteException;
}