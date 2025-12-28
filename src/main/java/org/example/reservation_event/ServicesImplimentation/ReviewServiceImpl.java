package org.example.reservation_event.ServicesImplimentation;

import org.example.reservation_event.Enums.ReservationStatut;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.Exceptions.ResourceNotFoundException;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.classes.Review;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.dtos.EventRatingDTO;
import org.example.reservation_event.dtos.ReviewSummaryDTO;
import org.example.reservation_event.repositories.ReviewRepository;
import org.example.reservation_event.repositories.ReservationRepository;
import org.example.reservation_event.services.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Override
    public Review saveOrUpdateReview(Reservation reservation, Integer rating, String comment) {
        System.out.println("🎬 DÉBUT saveOrUpdateReview");
        System.out.println("   Reservation ID: " + reservation.getId());
        System.out.println("   Rating: " + rating);

        // ✅ Validation de la note
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("La note doit être entre 1 et 5 étoiles");
        }

        // ✅ Vérifier que la réservation est CONFIRMÉE
        if (reservation.getStatut() != ReservationStatut.CONFIRMEE) {
            throw new IllegalStateException("Seules les réservations confirmées peuvent être notées");
        }

        // ✅ Vérifier que l'événement est TERMINÉ
        if (reservation.getEvenement().getStatut() != EventStatut.TERMINE) {
            throw new IllegalStateException("Vous ne pouvez noter que les événements terminés");
        }

        // ✅ Vérifier que l'événement est bien dans le passé
        if (reservation.getEvenement().getDateDebut().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("L'événement n'a pas encore eu lieu");
        }

        // ✅ Chercher un avis existant pour CETTE réservation
        Optional<Review> existingReviewOpt = reviewRepository.findByReservation(reservation);

        Review review;
        if (existingReviewOpt.isPresent()) {
            // Mise à jour
            review = existingReviewOpt.get();
            review.setRating(rating);
            review.setComment(comment);
            review.setUpdatedAt(LocalDateTime.now());
            System.out.println("✅ Mise à jour de l'avis existant ID=" + review.getId());
        } else {
            // Création
            review = new Review();
            review.setReservation(reservation); // ✅ Auto-set event et user
            review.setRating(rating);
            review.setComment(comment);
            System.out.println("✅ Création d'un nouvel avis");
        }

        // ✅ Sauvegarde simple
        Review savedReview = reviewRepository.save(review);
        System.out.println("✅ Avis sauvegardé avec succès ID=" + savedReview.getId());

        return savedReview;
    }

    @Override
    public boolean hasUserReviewedForReservation(Long reservationId) {
        return reviewRepository.existsByReservationId(reservationId);
    }

    @Override
    public Optional<Review> getReviewByReservation(Reservation reservation) {
        return reviewRepository.findByReservation(reservation);
    }

    @Override
    public List<Review> getReviewsForEvent(Long eventId) {
        return reviewRepository.findByEventId(eventId);
    }

    @Override
    public EventRatingDTO getEventRating(Long eventId) {
        Double average = reviewRepository.getAverageRatingForEvent(eventId);
        Long count = reviewRepository.countReviewsForEvent(eventId);

        return new EventRatingDTO(
                eventId,
                average != null ? average : 0.0,
                count != null ? count : 0L
        );
    }

    @Override
    @Transactional(readOnly = true) // ✅ AJOUT
    public List<Review> getReviewsByUser(User user) {
        List<Review> reviews = reviewRepository.findByUserOrderByCreatedAtDesc(user);

        // ✅ Forcer le chargement des relations
        reviews.forEach(review -> {
            review.getEvent().getTitre(); // Force le chargement de Event
            review.getReservation().getCodeReservation(); // Force le chargement de Reservation
        });

        return reviews;
    }

    @Override
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable"));

        reviewRepository.delete(review);
        System.out.println("✅ Avis supprimé ID=" + reviewId);
    }
    @Override
    @Transactional(readOnly = true)
    public List<ReviewSummaryDTO> getReviewsByUserAsDTO(Long userId) {
        return reviewRepository.findReviewsByUserAsDTO(userId);
    }
}