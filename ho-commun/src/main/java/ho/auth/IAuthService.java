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

    /**
     * Retourne le nom complet associe au jeton si connu.
     *
     * @param token jeton de session
     * @return nom utilisateur ou "inconnu" si absent
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    String getNomByToken(String token) throws RemoteException;

    
    /**
     * Recuperer l'identifiant de l'utilisateur
     * @param utilisateur la ligne de l'utilisateur dans le fichier json
     * @return l'identifiant de l'utilisateur
     * @throws RemoteException en cas d'erreur RMI
     * 
     */
    String getIdUtilisateur(String token) throws RemoteException;

    /**
     * Recuperer le role de l'utilisateur
     * @param token le token de session
     * @return le role de l'utilisateur
     */
    String getRoleToken(String token) throws RemoteException;

    /**
     * Retourne le nom complet associe a un identifiant utilisateur.
     *
     * @param idUtilisateur identifiant technique utilisateur
     * @return nom utilisateur, ou "inconnu" si introuvable
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    String getNomUtilisateurParId(String idUtilisateur) throws RemoteException;

    /**
     * Cree un nouveau compte utilisateur avec les informations fournies.
     * @param login
     * @param password
     * @param nom
     * @param role
     * @return
     * @throws RemoteException
     */
    boolean creerCompte(String login, String password, String nom, String role) throws RemoteException;
}