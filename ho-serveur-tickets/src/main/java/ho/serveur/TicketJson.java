package ho.serveur;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ho.modele.Ticket;

/**
 * Utilitaires JSON pour les tickets.
 */
public final class TicketJson {
    private TicketJson() {
    }

    /**
     * Extrait les objets JSON individuels d'une chaîne JSON representant une liste de tickets.
     * @param json
     * @return
     */
    public static List<String> extraireObjetsJson(String json) {
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
     * Lit la valeur d'un champ specifique dans un objet JSON representant un ticket.
     * @param objetJson
     * @param champ
     * @return
     */
    public static String lireChamp(String objetJson, String champ) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(champ) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(objetJson);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * Construit un objet Ticket a partir d'une chaîne JSON representant un ticket.
     * @param objet
     * @return
     */
    public static Ticket construireTicketDepuisObjetJson(String objet) {
        String id = lireChamp(objet, "id");
        String titre = lireChamp(objet, "titre");
        String categorie = lireChamp(objet, "categorie");
        String description = lireChamp(objet, "description");
        String etat = lireChamp(objet, "etat");
        String dateCreation = lireChamp(objet, "dateCreation");
        String dateAssignation = lireChamp(objet, "dateAssignation");
        String dateResolution = lireChamp(objet, "dateResolution");
        String messageResolution = lireChamp(objet, "messageResolution");
        String priorite = lireChamp(objet, "priorite");
        String idAgent = lireChamp(objet, "idAgent");
        String idCreateur = lireChamp(objet, "idCreateur");

        Ticket ticket = new Ticket(id, titre, categorie, description, idCreateur, etat, idAgent, priorite);
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
        return ticket;
    }

    /** 
     * Convertit un objet Ticket en une chaîne JSON representant ce ticket.
     * @param ticket
     */
    public static String ticketVersJson(Ticket ticket) {
        return "{" +
                "\"id\": \"" + echapper(ticket.getId()) + "\", " +
                "\"titre\": \"" + echapper(ticket.getTitre()) + "\", " +
                "\"categorie\": \"" + echapper(ticket.getCategorie()) + "\", " +
                "\"description\": \"" + echapper(ticket.getDescription()) + "\", " +
                "\"etat\": \"" + echapper(ticket.getEtat()) + "\", " +
                "\"priorite\": \"" + echapper(ticket.getPriorite()) + "\", " +
                "\"dateCreation\": \"" + echapper(ticket.getDateCreation()) + "\", " +
                "\"dateAssignation\": \"" + echapper(ticket.getDateAssignation()) + "\", " +
                "\"dateResolution\": \"" + echapper(ticket.getDateResolution()) + "\", " +
                "\"messageResolution\": \"" + echapper(ticket.getMessageResolution()) + "\", " +
                "\"idCreateur\": \"" + echapper(ticket.getIdCreateur()) + "\", " +
                "\"idAgent\": \"" + echapper(ticket.getIdAgent()) + "\"" +
                "}";
    }

    /**
     * Echappe les caracteres speciaux dans une valeur de champ pour la rendre compatible avec JSON.
     * @param valeur
     * @return
     */
    private static String echapper(String valeur) {
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