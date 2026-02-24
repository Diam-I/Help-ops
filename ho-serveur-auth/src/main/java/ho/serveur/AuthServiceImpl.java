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
 *
 * <p>Gère l'authentification et la validation centralisée des tokens.</p>
 */
public class AuthServiceImpl extends UnicastRemoteObject implements IAuthService {

    // Tokens actifs conservés en mémoire pour valider les sessions.
    private final Set<String> tokensActifs = Collections.synchronizedSet(new HashSet<>());
    // Association token -> login pour tracer les actions.
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
                    String idUtilisateur = extraireIdUtilisateurDepuisJson(utilisateur);
                    String token = "TOKEN-" + UUID.randomUUID() + "-" + idUtilisateur;
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
    public String getIdUtilisateur(String token) {
        if (token == null || token.isBlank()) {
            return "inconnu";
        }

        String tokenNettoye = token.trim();
        int debut = tokenNettoye.lastIndexOf("-") + 1;
        if (debut > 0 && debut < tokenNettoye.length()) {
            return tokenNettoye.substring(debut);
        }   
        return "inconnu";
    }

    /**
        * Extrait idUtilisateur depuis un bloc JSON utilisateur.
     */
    private String extraireIdUtilisateurDepuisJson(String utilisateurJson) {
        if (utilisateurJson == null || utilisateurJson.isBlank()) {
            return "inconnu";
        }

        String cle = "\"idUtilisateur\"";
        int indexCle = utilisateurJson.indexOf(cle);
        if (indexCle < 0) {
            return "inconnu";
        }

        int indexDeuxPoints = utilisateurJson.indexOf(':', indexCle);
        if (indexDeuxPoints < 0) {
            return "inconnu";
        }

        int indexGuillemetDebut = utilisateurJson.indexOf('"', indexDeuxPoints + 1);
        if (indexGuillemetDebut < 0) {
            return "inconnu";
        }

        int indexGuillemetFin = utilisateurJson.indexOf('"', indexGuillemetDebut + 1);
        if (indexGuillemetFin < 0) {
            return "inconnu";
        }

        String id = utilisateurJson.substring(indexGuillemetDebut + 1, indexGuillemetFin).trim();
        return id.isEmpty() ? "inconnu" : id;
    }
    /**
        * Valide un token de session.
     *
     * @param token token reçu d'un client ou d'un autre service
     * @return {@code true} si le token est connu et actif, sinon {@code false}
     * @throws RemoteException en cas d'erreur RMI
     */
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

    /**
        * Retourne le login associé à un token.
     *
     * @param token token de session
     * @return login associé, ou {@code null} si introuvable
     * @throws RemoteException en cas d'erreur RMI
     */
    @Override
    public String getLoginByToken(String token) throws RemoteException {
        if (token == null) {
            return null;
        }
        return tokenParLogin.get(token);
    }

    /**
        * Écrit un log horodaté pour suivre les actions.
     *
     * @param message message à afficher
     */
    private void log(String message) {
        String date = LocalDateTime.now().format(LOG_FORMAT);
        System.out.println("[" + date + "] " + message);
    }
}