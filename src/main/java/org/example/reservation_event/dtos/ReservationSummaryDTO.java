package org.example.reservation_event.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.Enums.ReservationStatut;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@Builder
public class ReservationSummaryDTO {

    // Reservation info
    private Long id;
    private String codeReservation;
    private String commentaire;
    private LocalDateTime dateReservation;
    private Double montantTotal;
    private Integer nombrePlaces;
    private ReservationStatut statut;

    // Event information
    private Long evenementId;
    private String evenementTitre;
    private LocalDateTime evenementDateDebut;
    private LocalDateTime evenementDateFin;
    private String evenementLieu;
    private String evenementVille;
    private Double evenementPrixUnitaire;
    private String evenementCategorie;

    // User information (for organizer/admin views)
    private Long utilisateurId;
    private String utilisateurNom;
    private String utilisateurPrenom;
    private String utilisateurEmail;
    private EventStatut evenementStatut;
    // Modifie le constructeur @AllArgsConstructor pour inclure evenementStatut
// OU crée un constructeur personnalisé :

    public ReservationSummaryDTO(Long id, String codeReservation, String commentaire,
                                 LocalDateTime dateReservation, Double montantTotal,
                                 Integer nombrePlaces, ReservationStatut statut,
                                 Long evenementId, String evenementTitre,
                                 LocalDateTime evenementDateDebut, LocalDateTime evenementDateFin,
                                 String evenementLieu, String evenementVille,
                                 Double evenementPrixUnitaire, String evenementCategorie,
                                 Long utilisateurId, String utilisateurNom,
                                 String utilisateurPrenom, String utilisateurEmail,
                                 EventStatut evenementStatut) { // ⭐ AJOUT

        this.id = id;
        this.codeReservation = codeReservation;
        this.commentaire = commentaire;
        this.dateReservation = dateReservation;
        this.montantTotal = montantTotal;
        this.nombrePlaces = nombrePlaces;
        this.statut = statut;
        this.evenementId = evenementId;
        this.evenementTitre = evenementTitre;
        this.evenementDateDebut = evenementDateDebut;
        this.evenementDateFin = evenementDateFin;
        this.evenementLieu = evenementLieu;
        this.evenementVille = evenementVille;
        this.evenementPrixUnitaire = evenementPrixUnitaire;
        this.evenementCategorie = evenementCategorie;
        this.utilisateurId = utilisateurId;
        this.utilisateurNom = utilisateurNom;
        this.utilisateurPrenom = utilisateurPrenom;
        this.utilisateurEmail = utilisateurEmail;
        this.evenementStatut = evenementStatut;
    }
    public ReservationSummaryDTO(Long id) {
        this.id = id;
    }
    public EventStatut getEvenementStatut() {
        return evenementStatut;
    }

    public void setEvenementStatut(EventStatut evenementStatut) {
        this.evenementStatut = evenementStatut;
    }

    // Formatted methods
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
        if (utilisateurPrenom != null && utilisateurNom != null) {
            return utilisateurPrenom + " " + utilisateurNom;
        }
        return "";
    }
}
