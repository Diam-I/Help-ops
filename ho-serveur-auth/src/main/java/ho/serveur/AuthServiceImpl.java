package ho.serveur;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
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
        /// erreur ici, le fichier json n'est pas trouvé /// 
        try {
            InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("ho/bd/utilisateurs.json");

            if (is == null) {
                System.out.println("Fichier utilisateurs.json non trouvée");
                return null;
            }

            String contenu = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            if (contenu.contains("\"login\": \"" + login + "\"") &&
                contenu.contains("\"password\": \"" + password + "\"")) {

                return "TOKEN-" + UUID.randomUUID();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    

}