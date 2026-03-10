package ho.tickets;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import ho.modele.Ticket;

/**
 * Contrat du service de tickets exposé via RMI.
 *
 * <p>Définit les opérations métier que le client appelle à distance.</p>
 */
public interface ITicketsService extends Remote {

    /**
        * Récupère un ticket précis à partir de son identifiant.
     *
     * @param token jeton d'authentification
     * @param id identifiant du ticket
     * @return le ticket trouvé, ou {@code null} si absent
     * @throws RemoteException en cas d'erreur RMI
     */
    Ticket getTicket(String token, String id) throws RemoteException;

    /**
        * Liste les tickets de l'utilisateur authentifié.
     *
     * @param token jeton d'authentification
     * @return liste des tickets
     * @throws RemoteException en cas d'erreur RMI
     */
    List<Ticket> listerTickets(String token) throws RemoteException;

    /**
     * Liste les tickets assignés à l'agent authentifié.
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
        * Crée un nouveau ticket à partir des informations du client.
     *
     * @param token jeton d'authentification
     * @param titre titre du ticket
     * @param categorie catégorie métier (incident ou demande)
     * @param description description détaillée
     * @return ticket créé
     * @throws RemoteException en cas d'erreur RMI
     */
    Ticket declarerTicket(String token, String titre, String categorie, String description) throws RemoteException;


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
}   
