package org.example.reservation_event.classes;

import org.example.reservation_event.Enums.EventCategorie;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.Enums.ReservationStatut;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 5, max = 100, message = "Le titre doit contenir entre 5 et 100 caractères")
    @Column(nullable = false, length = 100)
    private String titre;

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    @Column(length = 1000)
    private String description;

    @NotNull(message = "La catégorie est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategorie categorie;

    @NotNull(message = "La date de début est obligatoire")
    @Future(message = "La date de début doit être dans le futur")
    @Column(nullable = false)
    private LocalDateTime dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    @Column(nullable = false)
    private LocalDateTime dateFin;

    @NotBlank(message = "Le lieu est obligatoire")
    @Column(nullable = false)
    private String lieu;

    @NotBlank(message = "La ville est obligatoire")
    @Column(nullable = false)
    private String ville;

    @NotNull(message = "La capacité maximale est obligatoire")
    @Min(value = 1, message = "La capacité doit être supérieure à 0")
    @Column(nullable = false)
    private Integer capaciteMax;

    @NotNull(message = "Le prix unitaire est obligatoire")
    @Min(value = 0, message = "Le prix doit être supérieur ou égal à 0")
    @Column(nullable = false)
    private Double prixUnitaire;

    private String imageUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organisateur_id")
    private User organisateur;
    @NotNull(message = "Le statut est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventStatut statut = EventStatut.BROUILLON;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private LocalDateTime dateModification;
    @OneToMany(mappedBy = "evenement", fetch = FetchType.EAGER)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();
    // Dans Event.java, ajoute ces deux champs :
    @Column(name = "average_rating", columnDefinition = "double default 0.0")
    private Double averageRating = 0.0;

    @Column(name = "review_count", columnDefinition = "int default 0")
    private Integer reviewCount = 0;

    // Getters et setters
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }
    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
        if (statut == null) {
            statut = EventStatut.BROUILLON;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }

    // Méthodes utilitaires
    // In Event.java
    public int getPlacesReservees() {
        System.out.println("=== CALCUL PLACES RÉSERVÉES ===");
        System.out.println("Nombre total de réservations: " + reservations.size());

        int total = 0;
        for (Reservation r : reservations) {
            if (r.getStatut() == ReservationStatut.CONFIRMEE ||
                    r.getStatut() == ReservationStatut.EN_ATTENTE) {
                System.out.println("  - Comptée: " + r.getCodeReservation() +
                        " - " + r.getNombrePlaces() + " places - " + r.getStatut());
                total += r.getNombrePlaces();
            } else {
                System.out.println("  - Exclue: " + r.getCodeReservation() +
                        " - " + r.getNombrePlaces() + " places - " + r.getStatut());
            }
        }
        System.out.println("Total places réservées: " + total);
        System.out.println("=== FIN CALCUL ===");

        return total;
    }

    public int getPlacesDisponibles() {
        return capaciteMax - getPlacesReservees();
    }

    public boolean hasPlacesDisponibles(int nombrePlaces) {
        return getPlacesDisponibles() >= nombrePlaces;
    }

    public double getTauxRemplissage() {
        return (double) getPlacesReservees() / capaciteMax * 100;
    }

    public boolean isModifiable() {
        return statut != EventStatut.TERMINE && statut != EventStatut.ANNULE;
    }

    public boolean isReservable() {
        return statut == EventStatut.PUBLIE &&
                LocalDateTime.now().isBefore(dateDebut) &&
                getPlacesDisponibles() > 0;
    }

    public double getRevenuTotal() {
        return reservations.stream()
                .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                .mapToDouble(Reservation::getMontantTotal)
                .sum();
    }
}