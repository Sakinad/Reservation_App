package org.example.reservation_event.dtos;

import lombok.Data;
import org.example.reservation_event.Enums.ReservationStatut;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class ReservationDTO {
    private Long id;
    private String codeReservation;
    private String commentaire;
    private LocalDateTime dateReservation;
    private Double montantTotal;
    private Integer nombrePlaces;
    private ReservationStatut statut;

    // Event information (for display purposes)
    private Long evenementId;
    private String evenementTitre;
    private LocalDateTime evenementDateDebut;
    private String evenementLieu;
    private String evenementVille;
    private Double evenementPrixUnitaire;

    // User information (for display purposes)
    private Long utilisateurId;
    private String utilisateurNom;
    private String utilisateurPrenom;
    private String utilisateurEmail;

    // Formatage pour l'affichage
    public String getFormattedDateReservation() {
        return dateReservation != null ?
                dateReservation.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
    }

    public String getFormattedDateEvenement() {
        return evenementDateDebut != null ?
                evenementDateDebut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
    }

    public String getFormattedMontantTotal() {
        return String.format("%.2f DH", montantTotal != null ? montantTotal : 0.0);
    }

    public String getFormattedPrixUnitaire() {
        return String.format("%.2f DH", evenementPrixUnitaire != null ? evenementPrixUnitaire : 0.0);
    }

    public String getUtilisateurComplet() {
        return utilisateurPrenom + " " + utilisateurNom;
    }
}