package org.example.reservation_event.dtos;

import lombok.Data;
import org.example.reservation_event.Enums.UserRole;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private UserRole role;
    private LocalDateTime dateInscription;
    private Boolean actif;
    private String telephone;

    // Statistiques (pour le dashboard)
    private Long nombreReservations;
    private Long evenementsAVenir;
    private Double montantTotalDepense;
}

