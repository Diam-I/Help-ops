package ho.modele;

import java.io.Serializable;

/**
 * L'objet Incident doit implémenter Serializable pour être converti en flux 
 * d'octets et circuler sur le réseau entre le serveur de tickets et le client.
 */
public class Incident implements Serializable {
    private static final long serialVersionUID = 1L; // Garantit la compatibilité lors de la désérialisation
    
    public String id;
    public String titre;
    public String description;
    public String etat;

    public Incident(String id, String titre, String description) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.etat = "OPEN"; // État initial imposé par le cahier des charges
    }
}