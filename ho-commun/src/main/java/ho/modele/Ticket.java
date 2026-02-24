package ho.modele;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Représente un incident métier échangé entre client et serveurs.
 *
 * <p>La classe implémente {@link Serializable} pour permettre le transport
 * des instances via RMI.</p>
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

    /**
     * Construit un incident avec l'état initial {@code OPEN}.
     *
     * @param id identifiant unique de l'incident
     * @param titre titre court de l'incident
     * @param categorie catégorie de l'incident
     * @param description description détaillée de l'incident
     * @param dateCreation date de création du ticket
     * @param idCreateur identifiant du créateur du ticket
     */
    public Ticket(String id, String titre, String categorie, String description, String idCreateur) {
        this.id = id;
        this.titre = titre;
        this.categorie = categorie;
        this.description = description;
        this.dateCreation = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        this.idCreateur = idCreateur;
        this.etat = "OPEN";
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
    
}