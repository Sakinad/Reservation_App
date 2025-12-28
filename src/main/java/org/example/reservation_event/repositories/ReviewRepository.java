package org.example.reservation_event.repositories;

import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.classes.Review;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.dtos.ReviewSummaryDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // ✅ Trouver un avis par RÉSERVATION (unicité garantie)
    Optional<Review> findByReservation(Reservation reservation);

    // ✅ Vérifier si un avis existe pour une réservation
    boolean existsByReservationId(Long reservationId);

    // ✅ Tous les avis d'un événement
    List<Review> findByEventOrderByCreatedAtDesc(Event event);

    // ✅ Tous les avis d'un événement (par ID)
    @Query("SELECT r FROM Review r WHERE r.event.id = :eventId ORDER BY r.createdAt DESC")
    List<Review> findByEventId(@Param("eventId") Long eventId);

    // ✅ Calculer la moyenne des notes pour un événement
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.event.id = :eventId")
    Double getAverageRatingForEvent(@Param("eventId") Long eventId);

    // ✅ Compter les avis pour un événement
    @Query("SELECT COUNT(r) FROM Review r WHERE r.event.id = :eventId")
    Long countReviewsForEvent(@Param("eventId") Long eventId);

    // ✅ Tous les avis d'un utilisateur
    List<Review> findByUserOrderByCreatedAtDesc(User user);

    // ✅ Vérifier si un utilisateur a déjà noté un événement
    @Query("SELECT COUNT(r) > 0 FROM Review r WHERE r.event.id = :eventId AND r.user.id = :userId")
    boolean hasUserReviewedEvent(@Param("eventId") Long eventId, @Param("userId") Long userId);

    // ✅ Top événements par note moyenne
    @Query("SELECT r.event.id, AVG(r.rating) as avgRating " +
            "FROM Review r " +
            "GROUP BY r.event.id " +
            "HAVING COUNT(r) >= :minReviews " +
            "ORDER BY avgRating DESC")
    List<Object[]> findTopRatedEvents(@Param("minReviews") Long minReviews);
    @Query("SELECT new org.example.reservation_event.dtos.ReviewSummaryDTO(" +
            "r.id, r.rating, r.comment, r.createdAt, " +
            "e.id, e.titre, " +
            "res.id, res.codeReservation, " +
            "u.id, CONCAT(u.prenom, ' ', u.nom)) " +
            "FROM Review r " +
            "JOIN r.event e " +
            "JOIN r.reservation res " +
            "JOIN r.user u " +
            "WHERE u.id = :userId " +
            "ORDER BY r.createdAt DESC")
    List<ReviewSummaryDTO> findReviewsByUserAsDTO(@Param("userId") Long userId);
    @Query("SELECT r FROM Review r " +
            "LEFT JOIN FETCH r.event e " +
            "LEFT JOIN FETCH r.user u " +
            "WHERE r.event.id = :eventId " +
            "ORDER BY r.createdAt DESC")
    List<Review> findByEventIdWithRelations(@Param("eventId") Long eventId);

    // ✅ Tous les avis d'un événement (par Event) avec JOIN FETCH
    @Query("SELECT r FROM Review r " +
            "LEFT JOIN FETCH r.event e " +
            "LEFT JOIN FETCH r.user u " +
            "WHERE r.event = :event " +
            "ORDER BY r.createdAt DESC")
    List<Review> findByEventWithRelations(@Param("event") Event event);

    // ✅ Tous les avis d'un utilisateur avec relations
    @Query("SELECT r FROM Review r " +
            "LEFT JOIN FETCH r.event e " +
            "LEFT JOIN FETCH r.reservation res " +
            "WHERE r.user.id = :userId " +
            "ORDER BY r.createdAt DESC")
    List<Review> findByUserIdWithRelations(@Param("userId") Long userId);
    @Query("SELECT r FROM Review r " +
            "LEFT JOIN FETCH r.event e " +
            "LEFT JOIN FETCH r.user u " +
            "LEFT JOIN FETCH r.reservation res " +
            "WHERE e.organisateur.id = :organizerId " +
            "ORDER BY r.createdAt DESC")
    List<Review> findReviewsForOrganizerEvents(@Param("organizerId") Long organizerId);
}