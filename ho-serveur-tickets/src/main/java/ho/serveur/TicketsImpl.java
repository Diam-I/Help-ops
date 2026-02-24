package ho.serveur;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ho.auth.IAuthService;
import ho.modele.Ticket;
import ho.tickets.ITicketsService;

/**
 * Implémentation du service de tickets exposé via RMI.
 *
 * <p>Gère la création, la lecture et la persistance des tickets.</p>
 */
public class TicketsImpl extends UnicastRemoteObject implements ITicketsService {

    private static final DateTimeFormatter LOG_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    protected TicketsImpl() throws RemoteException {
        super();
    }

    /**
        * Récupère un ticket par son identifiant.
     */
    @Override
    public Ticket getTicket(String token, String id) throws RemoteException {
        String login = recupererLogin(token);
        log(login + " - getTicket id='" + id + "'");
        if (!tokenValide(token)) {
            log(login + " - token invalide pour getTicket");
            throw new RemoteException("Token d'authentification invalide");
        }
        log(login + " - token valide pour getTicket");

        List<Ticket> tickets = chargerTickets(token);
        for (Ticket ticket : tickets) {
            if (ticket.getId().equals(id)) {
                return ticket;
            }
        }
        return null;
    }

    /**
        * Affiche un ticket dans la console serveur (utile en debug/demo).
     */
    public void afficherTicket(Ticket ticket) {
        System.out.println("ID: " + ticket.getId());
        System.out.println("Titre: " + ticket.getTitre());
        System.out.println("Catégorie: " + ticket.getCategorie());
        System.out.println("Description: " + ticket.getDescription());
        System.out.println("Etat: " + ticket.getEtat());
        System.out.println("Date de création: " + ticket.getDateCreation());
        System.out.println("ID créateur: " + ticket.getIdCreateur());
    }

    public Ticket declarerTicket(String token, String titre, String categorie, String description) throws RemoteException {
        String login = recupererLogin(token);
        log(login + " - declarerTicket titre='" + titre + "'");
        if (!tokenValide(token)) {
            log(login + " - token invalide pour declarerTicket");
            throw new RemoteException("Token d'authentification invalide");
        }
        log(login + " - token valide pour declarerTicket");
        String utilisateurId = token.substring(token.lastIndexOf("-") + 1);
        String id = genererIdTicket();
        String categorieFinale = normaliserCategorie(categorie);
        Ticket ticket = new Ticket(id, titre, categorieFinale, description, utilisateurId);
        sauvegarderTicket(ticket);
        log(login + " - ticket cree id='" + id + "' categorie='" + categorieFinale + "'");
        return ticket;
    }

    /**
        * Liste les tickets après validation du token.
     */
    public List<Ticket> listerTickets(String token) throws RemoteException {
        String login = recupererLogin(token);
        log(login + " - listerTickets");
        if (!tokenValide(token)) {
            log(login + " - token invalide pour listerTickets");
            throw new RemoteException("Token d'authentification invalide");
        }
        log(login + " - token valide pour listerTickets");

        return chargerTickets(token);
    }

    /**
        * Délègue la validation du token au serveur d'authentification.
     */
    private boolean tokenValide(String token) throws RemoteException {
        if (token == null) {
            return false;
        }

        String tokenNettoye = token.trim();
        try {
            IAuthService authService = connecterAuthService();
            boolean valide = authService.verifierToken(tokenNettoye);
            String login = authService.getLoginByToken(tokenNettoye);
            String auteur = login == null ? "inconnu" : login;
            log("Serveur Ticket verifie le token de " + auteur + " aupres du Serveur Auth");
            return valide;
        } catch (RemoteException e) {
            log("Serveur auth injoignable pour verification token");
            throw e;
        } catch (Exception e) {
            log("Erreur verification token: " + e.getMessage());
            return false;
        }
    }

    /**
        * Récupère la référence distante vers le service d'authentification.
     */
    private IAuthService connecterAuthService() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        return (IAuthService) registry.lookup("AuthService");
    }

    /**
        * Normalise la catégorie pour garantir les valeurs métier attendues.
     */
    private String normaliserCategorie(String categorie) {
        if (categorie == null) {
            return "incident";
        }

        String valeur = categorie.trim().toLowerCase();
        if ("incident".equals(valeur) || "demande".equals(valeur)) {
            return valeur;
        }

        return "incident";
    }

    /**
        * Charge les tickets depuis le fichier JSON de persistance.
     */
    private List<Ticket> chargerTickets(String token) throws RemoteException {
        try {
            String contenu = lireContenuTicketsJson();
            List<String> objets = extraireObjetsJson(contenu);
            List<Ticket> tickets = new ArrayList<>();
            IAuthService authService = connecterAuthService();
            String idUtilisateur = authService.getIdUtilisateur(token);

            for (String objet : objets) {
                String id = lireChamp(objet, "id");
                String titre = lireChamp(objet, "titre");
                String categorie = lireChamp(objet, "categorie");
                String description = lireChamp(objet, "description");
                String idCreateur = lireChamp(objet, "idCreateur");
                String etat = lireChamp(objet, "etat");
                String dateCreation = lireChamp(objet, "dateCreation");

                if (id.isEmpty()) {
                    continue;
                }

                Ticket ticket = new Ticket(id, titre, categorie, description, idCreateur);
                if (!etat.isEmpty()) {
                    ticket.setEtat(etat);
                }
                if (!dateCreation.isEmpty()) {
                    ticket.setDateCreation(dateCreation);
                }
                if (idUtilisateur.equals(idCreateur)) {
                    tickets.add(ticket);
                }

            }

            return tickets;
        } catch (Exception e) {
            log("Erreur lecture tickets.json: " + e.getMessage());
            throw new RemoteException("Impossible de lire tickets.json", e);
        }
    }

    /**
        * Ajoute un ticket dans le JSON en conservant les tickets existants.
     */
    private void sauvegarderTicket(Ticket nouveauTicket) throws RemoteException {
        try {
            Path chemin = trouverCheminTicketsJson();
            Files.createDirectories(chemin.getParent());

            String contenu = "[]";
            if (Files.exists(chemin)) {
                contenu = Files.readString(chemin, StandardCharsets.UTF_8);
            }

            List<String> objets = extraireObjetsJson(contenu);
            objets.add(ticketVersJson(nouveauTicket));

            StringBuilder nouveauContenu = new StringBuilder();
            nouveauContenu.append("[\n");
            for (int i = 0; i < objets.size(); i++) {
                nouveauContenu.append("  ").append(objets.get(i));
                if (i < objets.size() - 1) {
                    nouveauContenu.append(",");
                }
                nouveauContenu.append("\n");
            }
            nouveauContenu.append("]\n");

            Files.writeString(
                    chemin,
                    nouveauContenu.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            log("Erreur ecriture tickets.json: " + e.getMessage());
            throw new RemoteException("Impossible d'écrire dans tickets.json", e);
        }
    }

    /**
        * Lit le contenu brut du fichier tickets.json.
     */
    private String lireContenuTicketsJson() throws Exception {
        Path chemin = trouverCheminTicketsJson();
        if (Files.exists(chemin)) {
            return Files.readString(chemin, StandardCharsets.UTF_8);
        }

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("ho/bd/tickets.json")) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        return "[]";
    }

    /**
        * Génère un identifiant incrémental sur 10 chiffres.
     */
    private String genererIdTicket() throws RemoteException {
        try {
            String contenu = lireContenuTicketsJson();
            List<String> objets = extraireObjetsJson(contenu);

            long dernierId = 0;
            boolean trouve = false;

            for (String objet : objets) {
                String id = lireChamp(objet, "id");
                if (id.matches("\\d{10}")) {
                    dernierId = Long.parseLong(id);
                    trouve = true;
                }
            }

            if (!trouve) {
                return "0000000001";
            }

            long suivant = dernierId + 1;
            return String.format("%010d", suivant);
        } catch (Exception e) {
            log("Erreur generation id ticket: " + e.getMessage());
            throw new RemoteException("Impossible de generer un id de ticket", e);
        }
    }

    /**
        * Écrit un log horodaté pour suivre les actions métier.
     */
    private void log(String message) {
        String date = LocalDateTime.now().format(LOG_FORMAT);
        System.out.println("[" + date + "] " + message);
    }

    /**
        * Récupère le login associé au token pour améliorer la lisibilité des logs.
     */
    private String recupererLogin(String token) {
        if (token == null) {
            return "inconnu";
        }
        try {
            IAuthService authService = connecterAuthService();
            String login = authService.getLoginByToken(token.trim());
            return login == null ? "inconnu" : login;
        } catch (Exception e) {
            return "inconnu";
        }
    }

    /**
        * Cherche le chemin de tickets.json selon le dossier de lancement.
     */
    private Path trouverCheminTicketsJson() {
        Path chemin1 = Path.of("ho-commun", "src", "main", "ressources", "ho", "bd", "tickets.json");
        Path chemin2 = Path.of("..", "ho-commun", "src", "main", "ressources", "ho", "bd", "tickets.json");

        if (Files.exists(chemin1) || Files.exists(chemin1.getParent())) {
            return chemin1;
        }
        if (Files.exists(chemin2) || Files.exists(chemin2.getParent())) {
            return chemin2;
        }

        return chemin1;
    }

    /**
        * Extrait chaque objet JSON d'un tableau de tickets.
     */
    private List<String> extraireObjetsJson(String json) {
        List<String> objets = new ArrayList<>();
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

    /**
        * Lit une valeur de champ dans un objet JSON (lecture simple par regex).
     */
    private String lireChamp(String objetJson, String champ) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(champ) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(objetJson);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
        * Sérialise un ticket au format JSON.
     */
    private String ticketVersJson(Ticket ticket) {
        return "{" +
                "\"id\": \"" + echapper(ticket.getId()) + "\", " +
                "\"titre\": \"" + echapper(ticket.getTitre()) + "\", " +
                "\"categorie\": \"" + echapper(ticket.getCategorie()) + "\", " +
                "\"description\": \"" + echapper(ticket.getDescription()) + "\", " +
                "\"etat\": \"" + echapper(ticket.getEtat()) + "\", " +
                "\"dateCreation\": \"" + echapper(ticket.getDateCreation()) + "\", " +
                "\"idCreateur\": \"" + echapper(ticket.getIdCreateur()) + "\"" +
                "}";
    }

    /**
        * Échappe les caractères sensibles avant écriture JSON.
     */
    private String echapper(String valeur) {
        if (valeur == null) {
            return "";
        }
        return valeur
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
}