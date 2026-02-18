package fr.m1rt.helpops.auth;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implantation du service. Définit le "COMMENT" sur le serveur.
 * Hérite de UnicastRemoteObject pour exporter l'objet sur le réseau.
 */
public class AuthServiceImpl extends UnicastRemoteObject implements IAuthService {

    protected AuthServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public String login(String login, String password) throws RemoteException {
        // Logique de test : admin / 123
        if ("admin".equals(login) && "123".equals(password)) {
            return "TOKEN-SECRET-" + login.toUpperCase();
        }
        return null;
    }
}