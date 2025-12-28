package org.example.reservation_event.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryDTO {

    private Long id;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    // Event info
    private Long eventId;
    private String eventTitle;

    // Reservation info
    private Long reservationId;
    private String reservationCode;

    // User info (pour admin/organizer)
    private Long userId;
    private String userName;

    /**
     * Formater la date de création
     */
    public String getFormattedDate() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
    }

    /**
     * Obtenir le texte des étoiles (ex: "⭐⭐⭐⭐⭐")
     */
    public String getStarsText() {
        if (rating == null) return "";
        return "⭐".repeat(rating);
    }

    /**
     * Vérifier si un commentaire existe
     */
    public boolean hasComment() {
        return comment != null && !comment.trim().isEmpty();
    }
}
