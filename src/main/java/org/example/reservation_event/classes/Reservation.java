package org.example.reservation_event.classes;

import org.example.reservation_event.Enums.ReservationStatut;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Random;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"utilisateur", "evenement"}) // ADD THIS LINE
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    @NotNull(message = "L'utilisateur est obligatoire")
    @ToString.Exclude
    private User utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id", nullable = false)
    @NotNull(message = "L'événement est obligatoire")
    @ToString.Exclude
    private Event evenement;

    @NotNull(message = "Le nombre de places est obligatoire")
    @Min(value = 1, message = "Le nombre de places doit être au moins 1")
    @Max(value = 10, message = "Le nombre de places ne peut pas dépasser 10")
    @Column(nullable = false)
    private Integer nombrePlaces;

    @NotNull(message = "Le montant total est obligatoire")
    @Min(value = 0, message = "Le montant doit être positif")
    @Column(nullable = false)
    private Double montantTotal;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateReservation;

    @NotNull(message = "Le statut est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatut statut = ReservationStatut.EN_ATTENTE;

    @NotBlank(message = "Le code de réservation est obligatoire")
    @Column(nullable = false, unique = true, length = 9)
    private String codeReservation;

    @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    @Column(length = 500)
    private String commentaire;

    @PrePersist
    protected void onCreate() {
        dateReservation = LocalDateTime.now();
        if (statut == null) {
            statut = ReservationStatut.EN_ATTENTE;
        }
        if (codeReservation == null || codeReservation.isEmpty()) {
            codeReservation = generateCodeReservation();
        }
        if (montantTotal == null && evenement != null && nombrePlaces != null) {
            calculerMontantTotal();
        }
    }

    // Génération du code de réservation unique
    private String generateCodeReservation() {
        Random random = new Random();
        int number = 10000 + random.nextInt(90000);
        return "EVT-" + number;
    }

    // Calcul automatique du montant total
    public void calculerMontantTotal() {
        if (evenement != null && nombrePlaces != null) {
            this.montantTotal = evenement.getPrixUnitaire() * nombrePlaces;
        }
    }

    // Méthodes utilitaires
    public boolean peutEtreAnnulee() {
        if (statut == ReservationStatut.ANNULEE || evenement == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        long heuresRestantes = Duration.between(now, evenement.getDateDebut()).toHours();

        return heuresRestantes >= 48;
    }

    public long getHeuresAvantEvenement() {
        if (evenement == null) {
            return -1;
        }
        LocalDateTime now = LocalDateTime.now();
        return Duration.between(now, evenement.getDateDebut()).toHours();
    }

    public boolean isActive() {
        return statut == ReservationStatut.CONFIRMEE || statut == ReservationStatut.EN_ATTENTE;
    }

    public boolean isAVenir() {
        return evenement != null &&
                LocalDateTime.now().isBefore(evenement.getDateDebut()) &&
                isActive();
    }

    public boolean isPassee() {
        return evenement != null &&
                LocalDateTime.now().isAfter(evenement.getDateFin());
    }

    public String getRecapitulatif() {
        StringBuilder recap = new StringBuilder();
        recap.append("Code: ").append(codeReservation).append("\n");
        if (evenement != null) {
            recap.append("Événement: ").append(evenement.getTitre()).append("\n");
            recap.append("Date: ").append(evenement.getDateDebut()).append("\n");
            recap.append("Lieu: ").append(evenement.getLieu()).append(", ").append(evenement.getVille()).append("\n");
        }
        recap.append("Places: ").append(nombrePlaces).append("\n");
        recap.append("Montant: ").append(String.format("%.2f DH", montantTotal)).append("\n");
        recap.append("Statut: ").append(statut.getLabel()).append("\n");
        if (commentaire != null && !commentaire.isEmpty()) {
            recap.append("Commentaire: ").append(commentaire).append("\n");
        }
        return recap.toString();
    }
}
