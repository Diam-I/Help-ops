package ho.serveur;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ho.auth.IAuthService;

/**
 * Implémentation serveur du contrat {@link IAuthService}.
 */
public class AuthServiceImpl extends UnicastRemoteObject implements IAuthService {

    /**
     * Crée et exporte le service d'authentification RMI.
     *
     * @throws RemoteException en cas d'échec de l'export RMI
     */
    protected AuthServiceImpl() throws RemoteException {
        super();
    }

    /**
     * Vérifie les identifiants et retourne un jeton de session si valides.
     *
     * @param login identifiant fourni par le client
     * @param password mot de passe fourni par le client
     * @return un jeton de session, ou {@code null} si l'authentification échoue
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    @Override
    public String login(String login, String password) throws RemoteException {
        // a modifier quand on aura les comptes //
        if ("admin".equals(login) && "123".equals(password)) {
            return "Token -" + login.toUpperCase();
        }
        return null;
    }
}