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
 * Implementation serveur du contrat {@link IAuthService}.
 *
 * <p>Gere l'authentification et la validation centralisee des tokens.</p>
 */
public class AuthServiceImpl extends UnicastRemoteObject implements IAuthService {

    // Tokens actifs conservés en mémoire pour valider les sessions.
    private final Map<String, String> tokensActifs = new ConcurrentHashMap<>();
    // Association token -> login pour tracer les actions.
    private final Map<String, String> tokenParLogin = new ConcurrentHashMap<>();
    private static final DateTimeFormatter LOG_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    /**
        * Cree et exporte le service d'authentification RMI.
     *
     * @throws RemoteException en cas d'échec de l'export RMI
     */
    protected AuthServiceImpl() throws RemoteException {
        super();
    }

    /**
        * Verifie les identifiants et retourne un jeton de session si valides.
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
                    String token = "TOKEN-" + UUID.randomUUID() ;
                    tokensActifs.put(token, idUtilisateur);
                    tokenParLogin.put(token, login);
                    log(login + " ID: " + idUtilisateur + " - token genere " + token);
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

    /**
     * Recupere l'identifiant de l'utilisateur associe a un token de session.
     *
     * @param token jeton d'authentification
     * @return l'identifiant de l'utilisateur, ou "inconnu" si le token est invalide ou absent
     * @throws RemoteException en cas d'erreur de communication RMI
     */

    @Override
    public String getIdUtilisateur(String token) throws RemoteException {
        if (token == null || token.isBlank()) {
            return "inconnu";
        }

        String tokenNettoye = token.trim();
        String idUtilisateur = tokensActifs.get(tokenNettoye);
        if (idUtilisateur == null || idUtilisateur.isBlank()) {
            return "inconnu";
        }
        return idUtilisateur;
    }

    /**
        * Extrait idUtilisateur depuis un bloc JSON utilisateur.
        * 
        * @param utilisateurJson bloc JSON représentant un utilisateur
        * @return l'idUtilisateur extrait, ou "inconnu" si absent ou erreur de format
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

    private String extraireNomDepuisJson(String utilisateurJson) {
        if (utilisateurJson == null || utilisateurJson.isBlank()) {
            return "inconnu";
        }

        String cle = "\"nom\"";
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

        String nom = utilisateurJson.substring(indexGuillemetDebut + 1, indexGuillemetFin).trim();
        return nom.isEmpty() ? "inconnu" : nom;
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

        boolean valide = tokensActifs.containsKey(token);
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
     * Recupere le nom de l'utilisateur associe a un token de session.
     * @param token jeton d'authentification
     * @return le nom de l'utilisateur, ou "inconnu" 
     */
    @Override
    public String getNomByToken(String token) throws RemoteException {
        if (token == null || token.isBlank()) {
            return "inconnu";
        }

        String idUtilisateur = getIdUtilisateur(token.trim());
        if (idUtilisateur == null || idUtilisateur.isBlank() || "inconnu".equals(idUtilisateur)) {
            return "inconnu";
        }

        return getNomUtilisateurParId(idUtilisateur);
    }

    /**
     * Recupere le nom de l'utilisateur associe a un identifiant.
     * @param idUtilisateur identifiant de l'utilisateur
     * @return le nom de l'utilisateur, ou "inconnu" si non trouve
     * @throws RemoteException en cas d'erreur RMI
     */
    @Override
    public String getNomUtilisateurParId(String idUtilisateur) throws RemoteException {
        if (idUtilisateur == null || idUtilisateur.isBlank()) {
            return "inconnu";
        }

        String idRecherche = idUtilisateur.trim();
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
                return "inconnu";
            }

            String contenu;
            try (InputStream input = is) {
                contenu = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }

            java.util.List<String> utilisateurs = extraireObjetsJson(contenu);
            for (String utilisateur : utilisateurs) {
                if (utilisateur.contains("\"idUtilisateur\": \"" + idRecherche + "\"")) {
                    String nom = extraireNomDepuisJson(utilisateur);
                    return nom == null || nom.isBlank() ? "inconnu" : nom;
                }
            }
        } catch (Exception e) {
            throw new RemoteException("Impossible de recuperer le nom utilisateur", e);
        }

        return "inconnu";
    }


    /**
        * Ecrit un log horodaté pour suivre les actions.
        *
        * @param message message à afficher
     */
    private void log(String message) {
        String date = LocalDateTime.now().format(LOG_FORMAT);
        System.out.println("[" + date + "] " + message);
    }

    /**
     * Recupere le role de l'utilisateur associé à un token de session.
     * 
     * @param token jeton d'authentification
     * @return le rôle de l'utilisateur ("agent", "utilisateur", ou "inconnu")
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    @Override
    public String getRoleToken(String token) throws RemoteException {
        if (token == null || token.isBlank()) {
            return "inconnu";
        }

        String login = tokenParLogin.get(token.trim());
        if (login == null || login.isBlank()) {
            return "inconnu";
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
                log("Fichier utilisateurs.json non trouvé pour getRoleToken");
                return "inconnu";
            }

            String contenu;
            try (InputStream input = is) {
                contenu = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Extraire chaque objet utilisateur en comptant les accolades //
            java.util.List<String> utilisateurs = extraireObjetsJson(contenu);
            for (String utilisateur : utilisateurs) {
                if (utilisateur.contains("\"login\": \"" + login + "\"")) {
                    if (utilisateur.contains("\"role\": \"agent\"")) {
                        log("Rôle trouvé pour " + login + ": agent");
                        return "agent";
                    }
                    if (utilisateur.contains("\"role\": \"utilisateur\"")) {
                        log("Rôle trouvé pour " + login + ": utilisateur");
                        return "utilisateur";
                    }
                }
            }
        } catch (Exception e) {
            log("Erreur pendant la recuperation du role: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Impossible de recuperer le role utilisateur", e);
        }

        log("Rôle non trouvé pour " + login + ", retour inconnu");
        return "inconnu";
    }

    /**
     * Extrait chaque objet JSON d'un tableau en comptant les accolades (gère les imbrications).
     * 
     * @param json chaîne JSON contenant un tableau d'objets
     * @return liste de chaînes JSON représentant chaque objet individuel
     */
    private java.util.List<String> extraireObjetsJson(String json) {
        java.util.List<String> objets = new java.util.ArrayList<>();
        if (json == null || json.isBlank()) {
            return objets;
        }

        int niveau = 0;
        int debut = -1;

        for (int i = 0; i < json.length(); i++) {
            char caractere = json.charAt(i);
            if (caractere == '{') {
                if (niveau == 0) {
                    debut = i;
                }
                niveau++;
            } else if (caractere == '}') {
                niveau--;
                if (niveau == 0 && debut >= 0) {
                    objets.add(json.substring(debut, i + 1));
                    debut = -1;
                }
            }
        }

        return objets;
    }

    public synchronized boolean creerCompte(String login, String password, String nom, String role) throws RemoteException {
        if (login == null || password == null || nom == null || role == null) {
            return false;
        }
        try {
            Path chemin = Path.of("ho-commun/src/main/ressources/ho/bd/utilisateurs.json");
            if (!Files.exists(chemin)) {
                log("Erreur : Le fichier est introuvable au chemin fixe : " + chemin.toAbsolutePath());
                return false;
            }
            if (chemin == null) {
                log("Fichier utilisateurs.json introuvable pour creerCompte");
                return false;
            }
            String contenu = Files.readString(chemin, StandardCharsets.UTF_8);
            if (contenu.contains("\"login\": \"" + login + "\"")) {
                // Login déjà utilisé //
                log("Tentative de creer compte avec login existant: " + login);
                return false; 

            }
            int prochinIndex = extraireObjetsJson(contenu).size() + 1;
            String nouvelId = String.format("U%03d", prochinIndex); 
            String nouvelUtilisateur = "  {\n" +
                    "    \"idUtilisateur\": \"" + nouvelId + "\",\n" +
                    "    \"login\": \"" + login + "\",\n" +
                    "    \"password\": \"" + password + "\",\n" +
                    "    \"nom\": \"" + nom + "\",\n" +
                    "    \"role\": \"" + role.toLowerCase() + "\"\n" +
                    "  }";
            int dernierCrochet = contenu.lastIndexOf(']');
            if (dernierCrochet == -1) return false;
            String nouveauContenu;
            if (contenu.contains("{")) {
                nouveauContenu = contenu.substring(0, dernierCrochet).trim();
                // Retirer la dernière virgule si elle existe pour éviter les erreurs de format
                if (nouveauContenu.endsWith(",")) {
                    nouveauContenu = nouveauContenu.substring(0, nouveauContenu.length() - 1);
                }
                nouveauContenu += ",\n" + nouvelUtilisateur + "\n]";
            } else {
                nouveauContenu = "[\n" + nouvelUtilisateur + "\n]";
            }

            Files.writeString(chemin, nouveauContenu, StandardCharsets.UTF_8);
            log("Nouveau compte créé : " + login + " avec le rôle " + role);
            return true;
            
        } catch (Exception e) {
            log("Erreur pendant la creation du compte: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Impossible de creer le compte", e);
        }
    }
    
}