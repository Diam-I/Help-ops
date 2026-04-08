package ho.serveur;

/**
 * Validation et normalisation des champs ticket.
 */
public final class TicketValidation {
    private TicketValidation() {
    }

    /**
     * Normalise la categorie d'un ticket. 
     * @return
     */
    public static String normaliserCategorie(String categorie) {
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
     * Normalise la priorite d'un ticket.
     * @param priorite
     * @return
     */
    public static String normaliserPriorite(String priorite) {
        if (priorite == null) {
            return "";
        }

        String valeur = priorite.trim().toUpperCase();
        if ("BASSE".equals(valeur) || "MOYENNE".equals(valeur) || "HAUTE".equals(valeur)) {
            return valeur;
        }

        return "";
    }
}
