package ho.tickets;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import ho.modele.Ticket;
import ho.supervision.ISupervisionClient;

/**
 * Contrat du service de tickets exposé via RMI.
 *
 * <p>Définit les opérations métier que le client appelle à distance.</p>
 */
public interface ITicketsService extends Remote {

    /**
        * Recupere un ticket precis à partir de son identifiant.
     *
     * @param token jeton d'authentification
     * @param id identifiant du ticket
     * @return le ticket trouvé, ou {@code null} si absent
     * @throws RemoteException en cas d'erreur RMI
     */
    Ticket getTicket(String token, String id) throws RemoteException;

    /**
        * Liste les tickets de l'utilisateur authentifie.
     *
     * @param token jeton d'authentification
     * @return liste des tickets
     * @throws RemoteException en cas d'erreur RMI
     */
    List<Ticket> listerTickets(String token) throws RemoteException;

    /**
     * Liste les tickets assignes à l'agent authentifie.
     *
     * @param token jeton d'authentification
     * @return liste des tickets assignés à l'agent
     * @throws RemoteException en cas d'erreur RMI
     */
    List<Ticket> listerTicketsAssignes(String token) throws RemoteException;

    /**
     * Liste tous les tickets (réservé aux agents).
     *
     * @param token jeton d'authentification
     * @return liste complète des tickets
     * @throws RemoteException en cas d'erreur RMI
     */
    List<Ticket> listerTousTickets(String token) throws RemoteException;

    /**
        * Cree un nouveau ticket a partir des informations du client.
     *
     * @param token jeton d'authentification
     * @param titre titre du ticket
     * @param categorie catégorie métier (incident ou demande)
     * @param description description détaillée
    * @param priorite priorité du ticket (BASSE, MOYENNE, HAUTE)
     * @return ticket créé
     * @throws RemoteException en cas d'erreur RMI
     */
    Ticket declarerTicket(String token, String titre, String categorie, String description, String priorite) throws RemoteException;


    /**
     * Prender en charge un ticket en l'assignant à l'agent .
     *  
     * @param token jeton d'authentification
     * @param id identifiant du ticket à prendre en charge
     * 
     * @return {@code true} si le ticket a été pris en charge avec succès, {@code false} sinon
     * @throws RemoteException en cas d'erreur RMI
     */
    boolean prendreEnCharge(String token, String idTicket) throws RemoteException;

    /**
     * Libèree un ticket deja assigne a l'agent connecte.
     *
     * @param token jeton d'authentification de l'agent
     * @param idTicket identifiant du ticket à libérer
     * @return {@code true} si la libération est réussie, {@code false} sinon
     * @throws RemoteException en cas d'erreur RMI
     */
    boolean libererTicket(String token, String idTicket) throws RemoteException;

    /**
     * Resoudre un ticket en le marquant comme resolu.
     *
     * @param token jeton d'authentification de l'agent
     * @param idTicket identifiant du ticket a resoudre
     * @param messageResolution message saisi par l'agent pour expliquer la resolution
     * @return {@code true} si la résolution est réussie, {@code false} sinon
     * @throws RemoteException en cas d'erreur RMI
     */
    boolean resoudreTicket(String token, String idTicket, String messageResolution) throws RemoteException;

    /**
     * 
     * affiche les statistiques
     * 
     * @param token jeton d'authentification de l'agent
     * @return une chaîne de caractères contenant les statistiques
     */
    String afficherStatistiques(String token) throws RemoteException;
    void sabonner(ISupervisionClient client, boolean rattrapage) throws RemoteException;

}   
