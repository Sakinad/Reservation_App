package org.example.reservation_event.services;

import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.classes.Review;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.dtos.EventRatingDTO;
import org.example.reservation_event.dtos.ReviewSummaryDTO;

import java.util.List;
import java.util.Optional;

public interface ReviewService {

    /**
     * Créer ou mettre à jour un avis pour UNE réservation
     */
    Review saveOrUpdateReview(Reservation reservation, Integer rating, String comment);

    /**
     * Vérifier si un avis existe pour une réservation
     */
    boolean hasUserReviewedForReservation(Long reservationId);

    /**
     * Récupérer un avis par réservation
     */
    Optional<Review> getReviewByReservation(Reservation reservation);

    /**
     * Récupérer tous les avis d'un événement
     */
    List<Review> getReviewsForEvent(Long eventId);

    /**
     * Calculer la moyenne et le nombre d'avis pour un événement
     */
    EventRatingDTO getEventRating(Long eventId);

    /**
     * Récupérer tous les avis d'un utilisateur
     */
    List<Review> getReviewsByUser(User user);

    /**
     * Supprimer un avis
     */
    void deleteReview(Long reviewId);
    List<ReviewSummaryDTO> getReviewsByUserAsDTO(Long userId);
}