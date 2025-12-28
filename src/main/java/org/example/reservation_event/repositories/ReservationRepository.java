package org.example.reservation_event.repositories;

import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.Enums.ReservationStatut;
import org.example.reservation_event.dtos.ReservationSummaryDTO; // CORRECTION: "dtos" au lieu de "dto"
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // Trouver les réservations d'un utilisateur
    List<Reservation> findByUtilisateur(User utilisateur);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.utilisateur JOIN FETCH r.evenement WHERE r.evenement = :event")
    List<Reservation> findByEvenement(@Param("event") Event event);
    boolean existsByEvenementAndUtilisateurAndStatut(Event evenement, User utilisateur, ReservationStatut statut);
    @Query("SELECT r FROM Reservation r JOIN FETCH r.evenement WHERE r.id = :id")
    Reservation findByIdWithEvent(@Param("id") Long id);

    // Trouver les réservations d'un événement avec un statut donné
    List<Reservation> findByEvenementAndStatut(Event evenement, ReservationStatut statut);

    // Calculer le nombre total de places réservées pour un événement
    @Query("SELECT SUM(r.nombrePlaces) FROM Reservation r WHERE r.evenement = :event AND r.statut = 'CONFIRMEE'")
    Integer countTotalPlacesReserved(@Param("event") Event event);

    // Trouver les réservations par code
    Optional<Reservation> findByCodeReservation(String codeReservation);

    // Trouver les réservations entre deux dates
    List<Reservation> findByDateReservationBetween(LocalDateTime start, LocalDateTime end);

    // Trouver les réservations confirmées d'un utilisateur
    List<Reservation> findByUtilisateurAndStatut(User utilisateur, ReservationStatut statut);

    List<Reservation> findByEvenementId(Long eventId);

    // Calculer le montant total des réservations par utilisateur
    @Query("SELECT SUM(r.montantTotal) FROM Reservation r WHERE r.utilisateur = :user AND r.statut = 'CONFIRMEE'")
    Double totalAmountByUser(@Param("user") User utilisateur);

    @Query("SELECT r FROM Reservation r WHERE r.utilisateur = :user AND r.statut = :statut AND r.evenement.dateDebut > :currentDate")
    List<Reservation> findByUtilisateurAndStatutAndEvenementDateDebutAfter(
            @Param("user") User user,
            @Param("statut") ReservationStatut statut,
            @Param("currentDate") LocalDateTime currentDate);

    // Version avec DTO projection pour éviter les problèmes Lazy
    @Query("SELECT new org.example.reservation_event.dtos.ReservationSummaryDTO(" +
            "r.id, r.codeReservation, r.nombrePlaces, r.montantTotal, " +
            "r.dateReservation, r.statut, r.evenement.titre, r.evenement.dateDebut, r.evenement.lieu) " +
            "FROM Reservation r " +
            "WHERE r.utilisateur = :user AND r.statut = :statut AND r.evenement.dateDebut > :currentDate")
    List<ReservationSummaryDTO> findUpcomingReservationsByUser(
            @Param("user") User user,
            @Param("statut") ReservationStatut statut,
            @Param("currentDate") LocalDateTime currentDate);

    // AJOUTER CETTE MÉTHODE MANQUANTE POUR TOUTES LES RÉSERVATIONS D'UN UTILISATEUR EN DTO
    @Query("SELECT new org.example.reservation_event.dtos.ReservationSummaryDTO(" +
            "r.id, r.codeReservation, r.commentaire, r.dateReservation, " +
            "r.montantTotal, r.nombrePlaces, r.statut, " +
            "e.id, e.titre, e.dateDebut, e.dateFin, e.lieu, e.ville, " +
            "e.prixUnitaire, e.categorie, " +
            "u.id, u.nom, u.prenom, u.email, " +
            "e.statut) " +  // ✅ AJOUT ICI !
            "FROM Reservation r " +
            "JOIN r.evenement e " +
            "JOIN r.utilisateur u " +
            "WHERE u = :user")
    List<ReservationSummaryDTO> findReservationsByUserAsDTO(@Param("user") User user);
    // AJOUTER CES MÉTHODES POUR ÉVITER LES PROBLÈMES LAZY
    @Query("SELECT r FROM Reservation r JOIN FETCH r.evenement WHERE r.utilisateur = :user")
    List<Reservation> findByUtilisateurWithEvent(@Param("user") User user);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.evenement WHERE r.utilisateur = :user AND r.statut = :statut")
    List<Reservation> findByUtilisateurAndStatutWithEvent(
            @Param("user") User user,
            @Param("statut") ReservationStatut statut);

    @Query("SELECT r FROM Reservation r " +
            "JOIN FETCH r.utilisateur " +
            "JOIN FETCH r.evenement " +
            "ORDER BY r.dateReservation DESC")
    List<Reservation> findAllWithUserAndEvent();

    @Query("SELECT r FROM Reservation r " +
            "JOIN FETCH r.utilisateur " +
            "JOIN FETCH r.evenement " +
            "JOIN FETCH r.evenement.organisateur " +
            "WHERE r.id = :id")
    Optional<Reservation> findByIdWithAllRelations(@Param("id") Long id);
    // AJOUTE CETTE MÉTHODE :
    List<Reservation> findByEvenement_DateDebutBetween(LocalDateTime start, LocalDateTime end);
}