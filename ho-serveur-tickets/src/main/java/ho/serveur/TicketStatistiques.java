package ho.serveur;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ho.auth.IAuthService;

/**
 * Calcul et formatage des statistiques tickets.
 */
public final class TicketStatistiques {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private TicketStatistiques() {
    }

    @FunctionalInterface
    public interface AuthServiceProvider {
        IAuthService get() throws Exception;
    }

    public static String generer(List<String> objets, AuthServiceProvider authProvider) {
        long totalTickets = objets.size();
        long ticketsResolus = 0;
        long ticketsOuverts = 0;
        long ticketsAssignes = 0;
        long totalDureeResolution = 0;
        long nbIncidents = 0;
        long nbDemandes = 0;

        Map<String, Integer> ticketsParAgent = new HashMap<>();
        Set<String> joursUniques = new HashSet<>();

        for (String objet : objets) {
            String etat = TicketJson.lireChamp(objet, "etat");
            String idAgent = TicketJson.lireChamp(objet, "idAgent");
            String dateCreation = TicketJson.lireChamp(objet, "dateCreation");
            String dateResolution = TicketJson.lireChamp(objet, "dateResolution");
            String categorie = TicketJson.lireChamp(objet, "categorie");

            if ("OPEN".equalsIgnoreCase(etat)) {
                ticketsOuverts++;
            } else if ("ASSIGNED".equalsIgnoreCase(etat)) {
                ticketsAssignes++;
            } else if ("RESOLVED".equalsIgnoreCase(etat)) {
                ticketsResolus++;
                if (!dateCreation.isBlank() && !dateResolution.isBlank()) {
                    LocalDateTime debut = LocalDateTime.parse(dateCreation, FORMATTER);
                    LocalDateTime fin = LocalDateTime.parse(dateResolution, FORMATTER);
                    totalDureeResolution += ChronoUnit.HOURS.between(debut, fin);
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
            } else if (categorie.contains("demande")) {
                nbDemandes++;
            }
        }

        int nbJours = joursUniques.isEmpty() ? 1 : joursUniques.size();
        double tempsMoyenGlobal = ticketsResolus == 0 ? 0 : (double) totalDureeResolution / ticketsResolus;

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
            IAuthService authService = null;
            try {
                authService = authProvider.get();
            } catch (Exception e) {
                authService = null;
            }

            for (String agentId : ticketsParAgent.keySet()) {
                int nbTickets = ticketsParAgent.get(agentId);
                double pressionAgent = (double) nbTickets / nbJours;
                String libelleAgent = agentId;

                if (authService != null) {
                    try {
                        String nomAgent = authService.getNomUtilisateurParId(agentId);
                        if (nomAgent != null && !nomAgent.isBlank() && !"inconnu".equalsIgnoreCase(nomAgent)) {
                            libelleAgent = nomAgent;
                        }
                    } catch (Exception e) {
                        libelleAgent = agentId;
                    }
                }

                sb.append("Agent: ").append(libelleAgent)
                        .append(" | Tickets: ").append(nbTickets)
                        .append(" | Moyenne: ").append(String.format("%.2f", pressionAgent)).append(" tickets/jour\n");
            }
        }

        int nbAgents = ticketsParAgent.size();
        double pressionGlobale = nbAgents == 0 ? 0 : (double) totalTickets / (nbAgents * nbJours);
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

        String nomAgentTop = "aucun";
        if (!idTopAgent.equals("inconnu")) {
            try {
                IAuthService authService = authProvider.get();
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

        return sb.toString();
    }
}
