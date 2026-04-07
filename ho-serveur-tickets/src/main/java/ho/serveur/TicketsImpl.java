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
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ho.auth.IAuthService;
import ho.modele.Ticket;
import ho.supervision.ISupervisionClient;
import ho.tickets.ITicketsService;

/**
 * Implémentation du service de tickets exposé via RMI.
 *
 * <p>Gère la création, la lecture et la persistance des tickets.</p>
 */
public class TicketsImpl extends UnicastRemoteObject implements ITicketsService {
    private List<ISupervisionClient> superviseurs = new ArrayList<>(); // la liste des clients qui sont en supprevision
    private List <String> historique= new ArrayList<>(); // l'historique des actions pour le ratrapage
     
    private static final DateTimeFormatter LOG_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    protected TicketsImpl() throws RemoteException {
        super();
    }

    /**
        * Récupère un ticket par son identifiant.
        * 
        * @param token jeton d'authentification
        * @param id identifiant du ticket à récupérer
        * @return le ticket correspondant à l'identifiant, ou {@code null} s'il n'existe pas ou n'appartient pas à l'utilisateur
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
        * Affiche un ticket dans la console serveur
        * @param ticket le ticket à afficher
        * @return void
        * @throws RemoteException en cas d'erreur RMI
        * 
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

    /**
        * Crée un nouveau ticket à partir des informations fournies par le client.
        * 
        * @param token jeton d'authentification
        * @param titre titre du ticket
        * @param categorie catégorie  (incident ou demande)
        * @param description description détaillée du problème ou de la demande
        * @return le ticket créé avec son identifiant généré
        * @throws RemoteException en cas d'erreur RMI ou de validation des données
    */
    public Ticket declarerTicket(String token, String titre, String categorie, String description) throws RemoteException {
        String login = recupererLogin(token);
        log(login + " - declarerTicket titre='" + titre + "'");
        if (!tokenValide(token)) {
            log(login + " - token invalide pour declarerTicket");
            throw new RemoteException("Token d'authentification invalide");
        }
        log(login + " - token valide pour declarerTicket");
        String utilisateurId;
        try {
            IAuthService authService = connecterAuthService();
            utilisateurId = authService.getIdUtilisateur(token);
        } catch (Exception e) {
            log("Serveur auth injoignable pour recuperer id utilisateur");
            throw new RemoteException("Serveur d'authentification injoignable", e);
        }
        if (utilisateurId == null || "inconnu".equals(utilisateurId)) {
            log("Utilisateur inconnu pour declarerTicket");
            throw new RemoteException("Utilisateur inconnu");
        }

        String id = genererIdTicket();
        String categorieFinale = normaliserCategorie(categorie);
        Ticket ticket = new Ticket(id, titre, categorieFinale, description, utilisateurId, "OPEN", null);
        ticket.setDateAssignation(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        sauvegarderTicket(ticket);
        emettreEvenement("Nouveau Ticket : \nID du ticket " + id + ".\nTitre du ticket : " + titre + ".\n Categorie du ticket : " + categorieFinale + ".\nCreateur du ticket : " + utilisateurId);
        log(login + " - ticket cree id='" + id + "' categorie='" + categorieFinale + "'");
        return ticket;
    }

    /**
        * Liste les tickets après validation du token.
        * 
        * @param token jeton d'authentification
        * @return liste des tickets créés par l'utilisateur authentifié
        * @throws RemoteException en cas d'erreur RMI ou de validation du token
        * 
     */
    public List<Ticket> listerTickets(String token) throws RemoteException {
        String idUtilisateur;
        try {
            IAuthService authService = connecterAuthService();
            idUtilisateur = authService.getIdUtilisateur(token);
        } catch (Exception e) {
            log("Serveur auth injoignable pour recuperer id utilisateur");
            throw new RemoteException("Serveur d'authentification injoignable", e);
        }

        log(idUtilisateur + " - listerTickets");
        if (!tokenValide(token)) {
            log(idUtilisateur + " - token invalide pour listerTickets");
            throw new RemoteException("Token d'authentification invalide");
        }
        if (idUtilisateur == null || "inconnu".equals(idUtilisateur)) {
            log("Utilisateur inconnu pour listerTickets");
            throw new RemoteException("Utilisateur inconnu");
        } 
        log(idUtilisateur + " - token valide pour listerTickets");

        return chargerTickets(idUtilisateur);
    }


    /**
     * Liste les tickets assignés à l'agent après validation du token et du rôle.
     * 
     * @param token jeton d'authentification
     * @return liste des tickets assignés à l'agent authentifié
     * @throws RemoteException en cas d'erreur RMI, de validation du token ou de rôle insuffisant
     * 
     */
    @Override
    public List<Ticket> listerTicketsAssignes(String token) throws RemoteException {
        String idUtilisateur;
        String role;
        try {
            IAuthService authService = connecterAuthService();
            idUtilisateur = authService.getIdUtilisateur(token);
            role = authService.getRoleToken(token);
        } catch (Exception e) {
            log("Serveur auth injoignable pour recuperer id utilisateur");
            throw new RemoteException("Serveur d'authentification injoignable", e);
        }

        if (!tokenValide(token)) {
            throw new RemoteException("Token d'authentification invalide");
        }
        if (idUtilisateur == null || "inconnu".equals(idUtilisateur)) {
            throw new RemoteException("Utilisateur inconnu");
        }
        if (!"agent".equalsIgnoreCase(role)) {
            throw new RemoteException("Action reservee aux agents");
        }

        return chargerTicketsParAgent(idUtilisateur);
    }

    /**
     * Liste tous les tickets après validation du token et du rôle.
     * 
     * @param token jeton d'authentification
     * @return liste de tous les tickets
     * @throws RemoteException en cas d'erreur RMI, de validation du token ou de rôle insuffisant
     * 
     */
    @Override
    public List<Ticket> listerTousTickets(String token) throws RemoteException {
        String idUtilisateur;
        String role;
        try {
            IAuthService authService = connecterAuthService();
            idUtilisateur = authService.getIdUtilisateur(token);
            role = authService.getRoleToken(token);
        } catch (Exception e) {
            log("Serveur auth injoignable pour recuperer id utilisateur");
            throw new RemoteException("Serveur d'authentification injoignable", e);
        }

        if (!tokenValide(token)) {
            throw new RemoteException("Token d'authentification invalide");
        }
        if (idUtilisateur == null || "inconnu".equals(idUtilisateur)) {
            throw new RemoteException("Utilisateur inconnu");
        }
        if (!"agent".equalsIgnoreCase(role)) {
            throw new RemoteException("Action reservee aux agents");
        }

        return chargerTousTickets();
    }

    /**
        * Délègue la validation du token au serveur d'authentification.
        * 
        * @param token jeton d'authentification
        * @return {@code true} si le token est valide, sinon {@code false}
        * @throws RemoteException en cas d'erreur de communication RMI avec le serveur d'authentification
        * 
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
        * @return référence distante vers IAuthService
        * @throws Exception en cas d'erreur de connexion au registre RMI ou de lookup du service
        * 
     */
    private IAuthService connecterAuthService() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        return (IAuthService) registry.lookup("AuthService");
    }

    /**
        * Normalise la catégorie pour garantir les valeurs métier attendues.
        * 
        * @param categorie catégorie fournie par le client
        * @return catégorie normalisée ("incident" ou "demande"), ou "incident" par défaut
        * 
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
        * 
        * @param idConnecte identifiant de l'utilisateur connecté pour filtrer les tickets qui lui appartiennent
        * @return liste des tickets appartenant à l'utilisateur connecté
        * @throws RemoteException en cas d'erreur de lecture ou de parsing du fichier JSON
        * 
     */
    private List<Ticket> chargerTickets(String idConnecte) throws RemoteException {
        try {
            String contenu = lireContenuTicketsJson();
            List<String> objets = extraireObjetsJson(contenu);
            List<Ticket> tickets = new ArrayList<>();

            for (String objet : objets) {
                String id = lireChamp(objet, "id");
                
                String idCreateur = lireChamp(objet, "idCreateur");
                
                if (id.isEmpty()) {
                    continue;
                }

                if (idConnecte.equals(idCreateur)) {
                    String titre = lireChamp(objet, "titre");
                    String categorie = lireChamp(objet, "categorie");
                    String description = lireChamp(objet, "description");
                    String etat = lireChamp(objet, "etat");
                    String dateCreation = lireChamp(objet, "dateCreation");
                    String dateAssignation = lireChamp(objet, "dateAssignation");
                    String dateResolution = lireChamp(objet, "dateResolution");
                    String messageResolution = lireChamp(objet, "messageResolution");
                    String idAgent = lireChamp(objet, "idAgent");
                    Ticket ticket = new Ticket(id, titre, categorie, description, idCreateur, etat, idAgent);
                    if (!etat.isEmpty()) {
                        ticket.setEtat(etat);
                    }
                    if (!dateCreation.isEmpty()) {
                        ticket.setDateCreation(dateCreation);
                    }
                    if (!dateAssignation.isEmpty()) {
                        ticket.setDateAssignation(dateAssignation);
                    }
                    if (!dateResolution.isEmpty()) {
                        ticket.setDateResolution(dateResolution);
                    }
                    if (!messageResolution.isEmpty()) {
                        ticket.setMessageResolution(messageResolution);
                    }
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
        * Charge les tickets assignés à un agent.
        * 
        * @param idAgentConnecte identifiant de l'agent connecté pour filtrer les tickets qui lui sont assignés
        * @return liste des tickets assignés à l'agent connecté
        * @throws RemoteException en cas d'erreur de lecture ou de parsing du fichier JSON
        *  
     */
    private List<Ticket> chargerTicketsParAgent(String idAgentConnecte) throws RemoteException {
        try {
            String contenu = lireContenuTicketsJson();
            List<String> objets = extraireObjetsJson(contenu);
            List<Ticket> tickets = new ArrayList<>();

            for (String objet : objets) {
                String id = lireChamp(objet, "id");
                String idAgent = lireChamp(objet, "idAgent");
                String idCreateur = lireChamp(objet, "idCreateur");

                if (id.isEmpty()) {
                    continue;
                }

                if (idAgentConnecte.equals(idAgent)) {
                    String titre = lireChamp(objet, "titre");
                    String categorie = lireChamp(objet, "categorie");
                    String description = lireChamp(objet, "description");
                    String etat = lireChamp(objet, "etat");
                    String dateCreation = lireChamp(objet, "dateCreation");
                    String dateAssignation = lireChamp(objet, "dateAssignation");
                    String dateResolution = lireChamp(objet, "dateResolution");
                    String messageResolution = lireChamp(objet, "messageResolution");

                    Ticket ticket = new Ticket(id, titre, categorie, description, idCreateur, etat, idAgent);
                    if (!etat.isEmpty()) {
                        ticket.setEtat(etat);
                    }
                    if (!dateCreation.isEmpty()) {
                        ticket.setDateCreation(dateCreation);
                    }
                    if (!dateAssignation.isEmpty()) {
                        ticket.setDateAssignation(dateAssignation);
                    }
                    if (!dateResolution.isEmpty()) {
                        ticket.setDateResolution(dateResolution);
                    }
                    if (!messageResolution.isEmpty()) {
                        ticket.setMessageResolution(messageResolution);
                    }
                    tickets.add(ticket);
                }
            }

            return tickets;
        } catch (Exception e) {
            log("Erreur lecture tickets assignes: " + e.getMessage());
            throw new RemoteException("Impossible de lire les tickets assignes", e);
        }
    }

    /**
        * Charge tous les tickets sans filtrage.
        * 
        * @param idConnecte identifiant de l'utilisateur connecté (non utilisé pour filtrage dans cette méthode)
        * @return liste de tous les tickets
        * @throws RemoteException en cas d'erreur de lecture ou de parsing du fichier JSON
        * 
     */
    private List<Ticket> chargerTousTickets() throws RemoteException {
        try {
            String contenu = lireContenuTicketsJson();
            List<String> objets = extraireObjetsJson(contenu);
            List<Ticket> tickets = new ArrayList<>();

            for (String objet : objets) {
                String id = lireChamp(objet, "id");
                String idAgent = lireChamp(objet, "idAgent");
                String idCreateur = lireChamp(objet, "idCreateur");

                if (id.isEmpty()) {
                    continue;
                }

                String titre = lireChamp(objet, "titre");
                String categorie = lireChamp(objet, "categorie");
                String description = lireChamp(objet, "description");
                String etat = lireChamp(objet, "etat");
                String dateCreation = lireChamp(objet, "dateCreation");
                String dateAssignation = lireChamp(objet, "dateAssignation");
                String dateResolution = lireChamp(objet, "dateResolution");
                String messageResolution = lireChamp(objet, "messageResolution");

                Ticket ticket = new Ticket(id, titre, categorie, description, idCreateur, etat, idAgent);
                if (!etat.isEmpty()) {
                    ticket.setEtat(etat);
                }
                if (!dateCreation.isEmpty()) {
                    ticket.setDateCreation(dateCreation);
                }
                if (!dateAssignation.isEmpty()) {
                    ticket.setDateAssignation(dateAssignation);
                }
                if (!dateResolution.isEmpty()) {
                    ticket.setDateResolution(dateResolution);
                }
                if (!messageResolution.isEmpty()) {
                    ticket.setMessageResolution(messageResolution);
                }
                tickets.add(ticket);
            }

            return tickets;
        } catch (Exception e) {
            log("Erreur lecture tous tickets: " + e.getMessage());
            throw new RemoteException("Impossible de lire les tickets", e);
        }
    }

    /**
        * Ajoute un ticket dans le JSON en conservant les tickets existants.
        * 
        * @param nouveauTicket le ticket à ajouter
        * @throws RemoteException en cas d'erreur d'écriture dans le fichier JSON
        * 
     */
    private synchronized void sauvegarderTicket(Ticket nouveauTicket) throws RemoteException {
        try {
            Path chemin = trouverCheminTicketsJson();
            Files.createDirectories(chemin.getParent());

            String contenu = "[]";
            if (Files.exists(chemin)) {
                contenu = Files.readString(chemin, StandardCharsets.UTF_8);
            }

            List<String> objets = extraireObjetsJson(contenu);
            boolean ticketMisAJour = false;

            for (int i = 0; i < objets.size(); i++) {
                String idExistant = lireChamp(objets.get(i), "id");
                if (nouveauTicket.getId() != null && nouveauTicket.getId().equals(idExistant)) {
                    objets.set(i, ticketVersJson(nouveauTicket));
                    ticketMisAJour = true;
                    break;
                }
            }

            if (!ticketMisAJour) {
                objets.add(ticketVersJson(nouveauTicket));
            }

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
        * 
        * @return contenu du fichier tickets.json, ou "[]" si le fichier n'existe pas
        * @throws Exception en cas d'erreur de lecture du fichier ou de ressource
        * 
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
        * 
        * @return nouvel identifiant de ticket unique
        * @throws RemoteException en cas d'erreur de lecture ou de parsing du fichier JSON
        * 
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
        * 
        * @param message message à logger
        * 
     */
    private void log(String message) {
        String date = LocalDateTime.now().format(LOG_FORMAT);
        System.out.println("[" + date + "] " + message);
    }



    /**
        * Cherche le chemin de tickets.json selon le dossier de lancement.
        * 
        * @return Path vers tickets.json, ou chemin par défaut si aucun des chemins n'existe
        * 
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
        * 
        * @param json contenu JSON brut à parser
        * @return liste de chaînes JSON représentant chaque ticket individuel
        * 
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
        * Récupère le login associé au token pour améliorer la lisibilité des logs.
        * 
        * @param token jeton d'authentification
        * @return login de l'utilisateur associé au token, ou "inconnu" en cas d'erreur ou si le token est invalide
        * 
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
        * Lit une valeur de champ dans un objet JSON (lecture simple par regex).
        * 
        * @param objetJson chaîne JSON représentant un ticket
        * @param champ nom du champ à lire (ex: "id", "titre", "categorie", etc.)
        * @return valeur du champ, ou chaîne vide si le champ n'est pas trouvé
        * 
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
        * 
        * @param ticket le ticket à convertir en JSON
        * @return chaîne JSON représentant le ticket
        *  
     */
    private String ticketVersJson(Ticket ticket) {
        return "{" +
                "\"id\": \"" + echapper(ticket.getId()) + "\", " +
                "\"titre\": \"" + echapper(ticket.getTitre()) + "\", " +
                "\"categorie\": \"" + echapper(ticket.getCategorie()) + "\", " +
                "\"description\": \"" + echapper(ticket.getDescription()) + "\", " +
                "\"etat\": \"" + echapper(ticket.getEtat()) + "\", " +
                "\"dateCreation\": \"" + echapper(ticket.getDateCreation()) + "\", " +
                "\"dateAssignation\": \"" + echapper(ticket.getDateAssignation()) + "\", " +
            "\"dateResolution\": \"" + echapper(ticket.getDateResolution()) + "\", " +
            "\"messageResolution\": \"" + echapper(ticket.getMessageResolution()) + "\", " +
                "\"idCreateur\": \"" + echapper(ticket.getIdCreateur()) + "\", " +
                "\"idAgent\": \"" + echapper(ticket.getIdAgent()) + "\"" +
                "}";
    }

    /**
        * Échappe les caractères sensibles avant écriture JSON.
        * 
        * @param valeur chaîne à échapper
        * @return chaîne échappée pour être insérée dans un JSON, ou chaîne vide si la valeur est null
        *  
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


    /**
     * Permet à un agent de prendre en charge un ticket en vérifiant son rôle et en mettant à jour le ticket avec l'ID de l'agent et la date d'assignation.
     * 
     * @param token jeton d'authentification de l'agent
     * @param idTicket identifiant du ticket à prendre en charge
     * @return {@code true} si la prise en charge est réussie, sinon {@code false} (par exemple si le rôle ne permet pas l'action ou si le ticket est déjà pris en charge)
     * @throws RemoteException en cas d'erreur RMI ou de communication avec le serveur d'authentification
     *  
     */
    @Override
    public boolean prendreEnCharge(String token, String idTicket) throws RemoteException {
        String role;
        try {
            IAuthService authService = connecterAuthService();
            role = authService.getRoleToken(token);
        } catch (Exception e) {
            throw new RemoteException("Serveur d'authentification injoignable", e);
        }

        if (!"agent".equalsIgnoreCase(role)) {
            log("Prise en charge refusée : rôle '" + role + "' ne permet pas cette action");
            return false;
        }
        else {
            String idAgent;
            try {
                IAuthService authService = connecterAuthService();
                idAgent = authService.getIdUtilisateur(token);
            } catch (Exception e) {
                throw new RemoteException("Serveur d'authentification injoignable", e);
            }
            if (idAgent == null || "inconnu".equals(idAgent)) {
                log("Prise en charge refusée : agent inconnu");
                return false;
            }

            log("Prise en charge acceptée pour rôle '" + role + "'");
            try {
                synchronized (this) {
                // Charger TOUS les tickets //
                String contenu = lireContenuTicketsJson();
                List<String> objets = extraireObjetsJson(contenu);
                
                for (String objet : objets) {
                    String id = lireChamp(objet, "id");
                    if (id.equals(idTicket)) {
                        String titre = lireChamp(objet, "titre");
                        String categorie = lireChamp(objet, "categorie");
                        String description = lireChamp(objet, "description");
                        String etat = lireChamp(objet, "etat");
                        String dateCreation = lireChamp(objet, "dateCreation");
                        String idCreateur = lireChamp(objet, "idCreateur");
                        String idAgentExistant = lireChamp(objet, "idAgent");

                        if (!idAgentExistant.isBlank() && !idAgent.equals(idAgentExistant)) {
                            throw new RemoteException("Ce ticket est déjà pris en charge par un autre agent (" + idAgentExistant + ").");
                        }
                        
                        Ticket ticket = new Ticket(id, titre, categorie, description, idCreateur, etat, idAgent);
                        if (!etat.isEmpty()) {
                            ticket.setEtat(etat);
                        }
                        if (!dateCreation.isEmpty()) {
                            ticket.setDateCreation(dateCreation);
                        }
                        ticket.setEtat("ASSIGNED");
                        ticket.setIdAgent(idAgent);
                        ticket.setDateAssignation(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
                        sauvegarderTicket(ticket);
                        emettreEvenement("Ticket :  " + id + " pris en charge par l'agent " + idAgent + ".");
                        log("Ticket " + idTicket + " pris en charge par agent " + idAgent);
                        return true;
                    }
                }
                log("Ticket " + idTicket + " non trouvé");
                return false;
            } 
            }catch (Exception e) {
                log("Erreur lors de la prise en charge du ticket: " + e.getMessage());
                throw new RemoteException("Erreur lors de la prise en charge", e);
            }
        }
    }

    @Override
    public boolean libererTicket(String token, String idTicket) throws RemoteException {
        String role;
        String idAgentConnecte;
        try {
            IAuthService authService = connecterAuthService();
            role = authService.getRoleToken(token);
            idAgentConnecte = authService.getIdUtilisateur(token);
        } catch (Exception e) {
            throw new RemoteException("Serveur d'authentification injoignable", e);
        }

        if (!"agent".equalsIgnoreCase(role)) {
            log("Libération refusée : rôle '" + role + "' ne permet pas cette action");
            return false;
        }
        if (idAgentConnecte == null || "inconnu".equals(idAgentConnecte)) {
            log("Libération refusée : agent inconnu");
            return false;
        }

        try {
            String contenu = lireContenuTicketsJson();
            List<String> objets = extraireObjetsJson(contenu);

            for (String objet : objets) {
                String id = lireChamp(objet, "id");
                if (!id.equals(idTicket)) {
                    continue;
                }

                String titre = lireChamp(objet, "titre");
                String categorie = lireChamp(objet, "categorie");
                String description = lireChamp(objet, "description");
                String etat = lireChamp(objet, "etat");
                String dateCreation = lireChamp(objet, "dateCreation");
                String idCreateur = lireChamp(objet, "idCreateur");
                String idAgentExistant = lireChamp(objet, "idAgent");

                if (idAgentExistant == null || idAgentExistant.isBlank()) {
                    throw new RemoteException("Ce ticket n'est pas assigné.");
                }
                if (!idAgentConnecte.equals(idAgentExistant)) {
                    throw new RemoteException("Ce ticket est assigné à un autre agent (" + idAgentExistant + ").");
                }

                Ticket ticket = new Ticket(id, titre, categorie, description, idCreateur, etat, null);
                if (!dateCreation.isEmpty()) {
                    ticket.setDateCreation(dateCreation);
                }

                ticket.setEtat("OPEN");
                ticket.setIdAgent(null);
                ticket.setDateAssignation("");
                sauvegarderTicket(ticket);
                log("Ticket " + idTicket + " libéré par agent " + idAgentConnecte);
                return true;
            }

            log("Ticket " + idTicket + " non trouvé pour libération");
            return false;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            log("Erreur lors de la libération du ticket: " + e.getMessage());
            throw new RemoteException("Erreur lors de la libération", e);
        }
    }

    @Override
    public boolean resoudreTicket(String token, String idTicket, String messageResolution) throws RemoteException {
        String role;
        String idAgentConnecte;
        try {
            IAuthService authService = connecterAuthService();
            role = authService.getRoleToken(token);
            idAgentConnecte = authService.getIdUtilisateur(token);
        } catch (Exception e) {
            throw new RemoteException("Serveur d'authentification injoignable", e);
        }
        if (!"agent".equalsIgnoreCase(role)) {
            log("Résolution refusée : rôle '" + role + "' ne permet pas cette action");
            return false;
        }
        try {
            String contenu = lireContenuTicketsJson();
            List<String> objets = extraireObjetsJson(contenu);

            for (String objet : objets) {
                String id = lireChamp(objet, "id");
                if (!id.equals(idTicket)) {
                    continue;
                }

                String titre = lireChamp(objet, "titre");
                String categorie = lireChamp(objet, "categorie");
                String description = lireChamp(objet, "description");
                String etat = lireChamp(objet, "etat");
                String dateCreation = lireChamp(objet, "dateCreation");
                String dateAssignation = lireChamp(objet, "dateAssignation");
                String idCreateur = lireChamp(objet, "idCreateur");
                String idAgentExistant = lireChamp(objet, "idAgent");

                if (idAgentExistant == null || idAgentExistant.isBlank()) {
                    throw new RemoteException("Ce ticket n'est pas assigné.");
                }
                if (!idAgentConnecte.equals(idAgentExistant)) {
                    throw new RemoteException("Ce ticket est assigné à un autre agent (" + idAgentExistant + ").");
                }
                if (!etat.equalsIgnoreCase("ASSIGNED")) {
                    throw new RemoteException("Ce ticket n'est pas assigné.");
                }
                else {
                    Ticket ticket = new Ticket(id, titre, categorie, description, idCreateur, etat, idAgentExistant);
                    if (!dateCreation.isEmpty()) {
                        ticket.setDateCreation(dateCreation);
                    }
                    if (!dateAssignation.isEmpty()) {
                        ticket.setDateAssignation(dateAssignation);
                    }
                    ticket.setEtat("RESOLVED");

                    log("Ticket " + idTicket + " résolu par agent " + idAgentConnecte);
                    ticket.setMessageResolution(messageResolution == null ? "" : messageResolution.trim());
                    ticket.setDateResolution(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
                    sauvegarderTicket(ticket);  
                    emettreEvenement("Ticket " + idTicket + " résolu par l'agent " + idAgentConnecte + ".\nMessage de résolution : " + messageResolution);
                    return true; 
                }
            }
            
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            log("Erreur lors de la résolution du ticket: " + e.getMessage());
            throw new RemoteException("Erreur lors de la résolution", e);
        }
        return false;

    }

    @Override
    public String afficherStatistiques(String token) throws RemoteException {
        try {
            String contenu = lireContenuTicketsJson();
            List<String> objets = extraireObjetsJson(contenu);
            
            long totalTickets = objets.size();
            long ticketsResolus = 0;
            long ticketsOuverts = 0;
            long ticketsAssignes = 0;
            long totalDureeResolution = 0;
            long nbIncidents = 0;
            long nbDemandes = 0;    

            java.util.Map<String, Integer> ticketsParAgent = new java.util.HashMap<>();
            java.util.Set<String> joursUniques = new java.util.HashSet<>();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            for (String objet : objets) {
                String etat = lireChamp(objet, "etat");
                String idAgent = lireChamp(objet, "idAgent");
                String dateCreation = lireChamp(objet, "dateCreation");
                String dateResolution = lireChamp(objet, "dateResolution");
                String categorie = lireChamp(objet, "categorie");
                if ("OPEN".equalsIgnoreCase(etat)) ticketsOuverts++;
                else if ("ASSIGNED".equalsIgnoreCase(etat)) ticketsAssignes++;
                else if ("RESOLVED".equalsIgnoreCase(etat)) {
                    ticketsResolus++;
                    if (!dateCreation.isBlank() && !dateResolution.isBlank()) {
                        java.time.LocalDateTime debut = java.time.LocalDateTime.parse(dateCreation, formatter);
                        java.time.LocalDateTime fin = java.time.LocalDateTime.parse(dateResolution, formatter);
                        totalDureeResolution += java.time.temporal.ChronoUnit.HOURS.between(debut, fin); 
                
                    }
                }
                if (idAgent != null && !idAgent.isBlank()) {
                    ticketsParAgent.put(idAgent, ticketsParAgent.getOrDefault(idAgent, 0) + 1);
                }

                if (!dateCreation.isBlank()) {
                    joursUniques.add(dateCreation.split(" ")[0]);
                }
                if (categorie.contains("incident")) {
                    nbIncidents++;
                }
                else if (categorie.contains("demande")) {
                    nbDemandes++;
                }
            }

            int nbJours ; 
            double tempsMoyenGlobal ;
            if (ticketsResolus == 0) {
                tempsMoyenGlobal = 0;
            }
            else {
                tempsMoyenGlobal = (double) totalDureeResolution / ticketsResolus;
            }
            if (joursUniques.isEmpty()) {
                nbJours = 1;
            }
            else {
                nbJours = joursUniques.size();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== STATISTIQUES ===\n");
            sb.append("Total tickets : ").append(totalTickets).append("\n");
            sb.append("Par état : OPEN(").append(ticketsOuverts).append("), ASSIGNED(")
            .append(ticketsAssignes).append("), RESOLVED(").append(ticketsResolus).append(")\n\n");
            sb.append("Temps moyen de résolution : ").append(String.format("%.2f", tempsMoyenGlobal)).append(" heures\n\n");
            sb.append("--- DÉTAIL PAR AGENT ---\n");
            if (ticketsParAgent.isEmpty()) {
                sb.append("Aucun agent n'a encore pris de ticket.\n");
            } else {
                for (String agentId : ticketsParAgent.keySet()) {
                    int nbTickets = ticketsParAgent.get(agentId);
                    double pressionAgent = (double) nbTickets / nbJours;
                    sb.append("Agent ID: ").append(agentId)
                    .append(" | Tickets: ").append(nbTickets)
                    .append(" | Moyenne: ").append(String.format("%.2f", pressionAgent)).append(" tickets/jour\n");
                }
            }

            int nbAgents = ticketsParAgent.size();
            double pressionGlobale ; 
            if (nbAgents == 0) {
                pressionGlobale = 0;
            }
            else {
                pressionGlobale = (double) totalTickets / (nbAgents*nbJours);
            }
            sb.append("\nPression globale : ").append(String.format("%.2f", pressionGlobale)).append(" tickets/agent/jour\n");  

            String idTopAgent = "inconnu";
            int maxTickets = 0;
            for (String agentId : ticketsParAgent.keySet()) {
                int nbTickets = ticketsParAgent.get(agentId);
                if (nbTickets > maxTickets) {
                    maxTickets = nbTickets;
                    idTopAgent = agentId;
                }
            }
            String nomAgentTop = "aucun" ; 
            if (!idTopAgent.equals("inconnu")) {
                try {
                    IAuthService authService = connecterAuthService();
                    nomAgentTop = authService.getNomUtilisateurParId(idTopAgent);
                    
                } catch (Exception e) {
                    nomAgentTop = idTopAgent;
                }
            }
            sb.append("L'agent qui prend le plus de tickets : ").append(nomAgentTop).append(" (").append(maxTickets).append(" tickets)\n");

            if (totalTickets > 0) {
                double pourcentageIncidents = (double) nbIncidents / totalTickets * 100;
                double pourcentageDemandes = (double) nbDemandes / totalTickets * 100;
                sb.append("\nRépartition par catégorie :\n");
                sb.append("Incidents : ").append(String.format("%.2f", pourcentageIncidents)).append("% (").append(nbIncidents).append(" tickets)\n");
                sb.append("Demandes : ").append(String.format("%.2f", pourcentageDemandes)).append("% (").append(nbDemandes).append(" tickets)\n");
            }
            log("Statistiques générées.");

            
            return sb.toString();

        } catch (Exception e) {
            throw new RemoteException("Erreur lors du calcul des statistiques", e);
        }
    }

    @Override
    public void sabonner(ISupervisionClient client, boolean rattrapage) throws RemoteException {
        if (rattrapage){
            for (String evenement : historique) {
                try {
                    client.notifierEvenement(evenement);
                } catch (RemoteException e) {
                    log("Erreur lors de la notification d'un événement en rattrapage: " + e.getMessage());
                }
            }
        }
        if (!superviseurs.contains(client)) {
            superviseurs.add(client);
            log("Nouveau superviseur abonné. Total superviseurs: " + superviseurs.size());
        }
    }

    private void emettreEvenement(String message) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        String evenement = "[" + date + "] " + message;
        historique.add(evenement);
        
        if (historique.size() > 20) {
            historique.remove(0);
        }
        Iterator<ISupervisionClient> iterator = superviseurs.iterator();
        while (iterator.hasNext()) {
            // Creation d'une copie de l'evenement pour eviter les problemes de concurrence si la liste est modifiee pendant l'iteration //
            ISupervisionClient client = iterator.next();
            try {
                client.notifierEvenement(evenement);
            } catch (RemoteException e) {
                log("Erreur lors de la notification d'un événement: " + e.getMessage());
                iterator.remove();
                log("Superviseur supprimé en raison d'une erreur de notification.");
            }
        }
    }

    

    

}
