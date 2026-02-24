package ho.auth;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Contrat du service d'authentification exposé via RMI.
 *
 * <p>Définit les opérations d'authentification partagées entre client et serveurs.</p>
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

    /**
        * Vérifie si un jeton est toujours valide.
     *
     * @param token jeton de session
     * @return {@code true} si le jeton est valide, sinon {@code false}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    boolean verifierToken(String token) throws RemoteException;

    /**
        * Retourne le login associé au jeton si connu.
     *
     * @param token jeton de session
     * @return login utilisateur ou {@code null} si inconnu
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    String getLoginByToken(String token) throws RemoteException;
}