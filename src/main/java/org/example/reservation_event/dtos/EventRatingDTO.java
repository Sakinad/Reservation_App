package org.example.reservation_event.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRatingDTO {

    private Long eventId;
    private Double averageRating; // Moyenne des notes (0.0 à 5.0)
    private Long totalReviews;    // Nombre total d'avis

    /**
     * Formater la moyenne pour l'affichage (ex: "4.3 / 5")
     */
    public String getFormattedRating() {
        if (totalReviews == 0) {
            return "Aucun avis";
        }
        return String.format("%.1f / 5 ⭐ (%d avis)", averageRating, totalReviews);
    }

    /**
     * Obtenir le nombre d'étoiles pleines (pour l'affichage)
     */
    public int getFullStars() {
        return (int) Math.round(averageRating);
    }

    /**
     * Vérifier si l'événement a des avis
     */
    public boolean hasReviews() {
        return totalReviews > 0;
    }
}
