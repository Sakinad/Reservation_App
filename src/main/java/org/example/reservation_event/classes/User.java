package org.example.reservation_event.classes;

import org.example.reservation_event.Enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Column(nullable = false)
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    @Column(nullable = false)
    private String password;

    @NotNull(message = "Le rôle est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private LocalDateTime dateInscription;

    @Column(nullable = false)
    private Boolean actif = true;

    @Pattern(regexp = "^[0-9+\\-\\s()]*$", message = "Format de téléphone invalide")
    private String telephone;

    // Relations
    @OneToMany(mappedBy = "organisateur", cascade = CascadeType.ALL, orphanRemoval = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Event> evenementsOrganises = new ArrayList<>();

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dateInscription = LocalDateTime.now();
        if (actif == null) {
            actif = true;
        }
    }

    // Méthodes utilitaires
    public String getNomComplet() {
        return prenom + " " + nom;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isOrganizer() {
        return role == UserRole.ORGANIZER || role == UserRole.ADMIN;
    }

    public boolean isClient() {
        return role == UserRole.CLIENT;
    }


    public boolean isActif() {
        return actif;
    }
}