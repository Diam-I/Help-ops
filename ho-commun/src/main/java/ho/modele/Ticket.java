package ho.modele;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Représente un ticket métier échangé entre client et serveurs.
 *
 * <p>Objet de transport partagé entre les modules via RMI.</p>
 */
public class Ticket implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String titre;
    private String categorie;
    private String description;
    private String etat;
    private String dateCreation ; 
    private String idCreateur ; 
    private String dateAssignation ;
    private String idAgent ;

    /**
        * Construit un ticket avec l'état initial {@code OPEN}.
        *
        * @param id identifiant unique du ticket
        * @param titre titre court du ticket
        * @param categorie catégorie métier du ticket
        * @param description description détaillée du ticket
        * @param idCreateur identifiant du créateur du ticket
        * @param dateCreation date de création du ticket
        * @param etat état initial du ticket (OPEN, IN_PROGRESS, CLOSED)
        * @param dateAssignation date d'assignation du ticket à un agent
        * @param idAgent identifiant de l'agent assigné au ticket
    */
    public Ticket(String id, String titre, String categorie, String description, String idCreateur, String etat, String idAgent) {
        this.id = id;
        this.titre = titre;
        this.categorie = categorie;
        this.description = description;
        this.dateCreation = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        this.idCreateur = idCreateur;
        this.etat = (etat == null || etat.isBlank()) ? "OPEN" : etat;
        this.idAgent = (idAgent == null || idAgent.isBlank()) ? null : idAgent;
    }

    public String getId() {
        return id;
    }
    public String getTitre() {
        return titre;
    }   
    public String getCategorie() {
        return categorie;
    }
    public String getDescription() {
        return description;
    }
    public String getEtat() {
        return etat;
    }
    public void setEtat(String etat) {
        this.etat = etat;
    }
    public String getDateCreation() {
        return dateCreation;
    }
    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }
    public String getIdCreateur() {
        return idCreateur;
    }
    public void setDateAssignation(String dateAssignation) {
        this.dateAssignation = dateAssignation;
    }
    public String getDateAssignation() {
        return dateAssignation;
    }
    public String getIdAgent() {
        return idAgent;
    }
    public void setIdAgent(String idAgent) {
        this.idAgent = idAgent;
    }
    
}