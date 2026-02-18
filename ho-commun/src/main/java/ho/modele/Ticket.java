package ho.modele;

import java.io.Serializable;

/**
 * Représente un incident métier échangé entre client et serveurs.
 *
 * <p>La classe implémente {@link Serializable} pour permettre le transport
 * des instances via RMI.</p>
 */
public class Ticket implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public String id;
    public String titre;
    public String description;
    public String etat;

    /**
     * Construit un incident avec l'état initial {@code OPEN}.
     *
     * @param id identifiant unique de l'incident
     * @param titre titre court de l'incident
     * @param description description détaillée de l'incident
     */
    public Ticket(String id, String titre, String description) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.etat = "OPEN";
    }
}