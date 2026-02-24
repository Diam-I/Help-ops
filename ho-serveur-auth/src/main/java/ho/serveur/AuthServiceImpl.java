package ho.serveur;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.UUID;
import ho.auth.IAuthService;

/**
 * Implémentation serveur du contrat {@link IAuthService}.
 */
public class AuthServiceImpl extends UnicastRemoteObject implements IAuthService {

    private final Set<String> tokensActifs = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, String> tokenParLogin = new ConcurrentHashMap<>();
    private static final DateTimeFormatter LOG_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

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
        log("Tentative de connexion pour login='" + login + "'");
        if (login == null || password == null) {
            log("Identifiants manquants");
            return null;
        }

        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("ho/bd/utilisateurs.json");

            if (is == null) {
                Path chemin = Path.of("ho-commun", "src", "main", "ressources", "ho", "bd", "utilisateurs.json");
                if (Files.exists(chemin)) {
                    is = Files.newInputStream(chemin);
                }
            }

            if (is == null) {
                Path chemin = Path.of("..", "ho-commun", "src", "main", "ressources", "ho", "bd", "utilisateurs.json");
                if (Files.exists(chemin)) {
                    is = Files.newInputStream(chemin);
                }
            }

            if (is == null) {
                log("Fichier utilisateurs.json non trouvé");
                return null;
            }

            String contenu;
            try (InputStream input = is) {
                contenu = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }

            String[] utilisateurs = contenu.split("\\},");
            for (String utilisateur : utilisateurs) {
                boolean loginOk = utilisateur.contains("\"login\": \"" + login + "\"");
                boolean passwordOk = utilisateur.contains("\"password\": \"" + password + "\"");

                if (loginOk && passwordOk) {
                    String token = "TOKEN-" + UUID.randomUUID();
                    tokensActifs.add(token);
                    tokenParLogin.put(token, login);
                    log(login + " - token genere " + token);
                    return token;
                }
            }

        } catch (Exception e) {
            log("Erreur pendant l'authentification: " + e.getMessage());
            e.printStackTrace();
        }
        log("Authentification echouee pour login='" + login + "'");
        return null;
    }

    @Override
    public boolean verifierToken(String token) throws RemoteException {
        if (token == null) {
            log("Verification token (null)");
            return false;
        }

        boolean valide = tokensActifs.contains(token);
        String login = tokenParLogin.get(token);
        String auteur = login == null ? "inconnu" : login;
        log("Verification token pour " + auteur);
        log("token " + (valide ? "valide" : "non valide"));
        return valide;
    }

    @Override
    public String getLoginByToken(String token) throws RemoteException {
        if (token == null) {
            return null;
        }
        return tokenParLogin.get(token);
    }

    private void log(String message) {
        String date = LocalDateTime.now().format(LOG_FORMAT);
        System.out.println("[" + date + "] " + message);
    }

    

}