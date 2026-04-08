package ho.serveur;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonTicket {
    private JsonTicket() {
    }

    /**
        * Lit le contenu brut du fichier tickets.json.
        * 
        * @return contenu du fichier tickets.json, ou "[]" si le fichier n'existe pas
        * @throws Exception en cas d'erreur de lecture du fichier ou de ressource
        * 
     */
    public static String lireContenuTicketsJson(Class<?> resourceClass) throws Exception {
        Path chemin = trouverCheminTicketsJson();
        if (Files.exists(chemin)) {
            return Files.readString(chemin, StandardCharsets.UTF_8);
        }

        try (InputStream is = resourceClass.getClassLoader().getResourceAsStream("ho/bd/tickets.json")) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        return "[]";
    }

    
    /**
        * Cherche le chemin de tickets.json selon le dossier de lancement.
        * 
        * @return Path vers tickets.json, ou chemin par défaut si aucun des chemins n'existe
        * 
     */
    public static Path trouverCheminTicketsJson() {
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

}
