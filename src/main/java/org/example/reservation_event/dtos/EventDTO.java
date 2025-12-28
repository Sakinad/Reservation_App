
package org.example.reservation_event.dtos;

import org.example.reservation_event.Enums.EventCategorie;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.Enums.ReservationStatut;
import org.example.reservation_event.classes.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class EventDTO {
        private Long id;
        private String titre;
        private String description;
        private EventCategorie categorie;
        private LocalDateTime dateDebut;
        private LocalDateTime dateFin;
        private String lieu;
        private String ville;
        private Integer capaciteMax;
        private Double prixUnitaire;
        private String imageUrl;
        private EventStatut statut;
        private LocalDateTime dateCreation;
        private LocalDateTime dateModification;

        // Informations calculées
        private Integer placesReservees;
        private Integer placesDisponibles;
        private Double tauxRemplissage;
        private Double revenuTotal;
        private Boolean isReservable;
        private Boolean isModifiable;

        // Informations organisateur
        private Long organisateurId;
        private String organisateurNom;
        private String organisateurPrenom;
        private String organisateurNomComplet;
        private String organisateurEmail;
        private String organisateurTelephone;
        private String organisateurRole;

        // Méthode statique pour convertir Event en EventDTO
        public static EventDTO fromEntity(Event event) {
            if (event == null) return null;

            // Calculer les places réservées
            int placesReservees = event.getReservations().stream()
                    .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE ||
                            r.getStatut() == ReservationStatut.EN_ATTENTE)
                    .mapToInt(r -> r.getNombrePlaces())
                    .sum();

            int placesDisponibles = event.getCapaciteMax() - placesReservees;
            double tauxRemplissage = (double) placesReservees / event.getCapaciteMax() * 100;

            // Calculer le revenu total
            double revenuTotal = event.getReservations().stream()
                    .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                    .mapToDouble(r -> r.getMontantTotal())
                    .sum();

            return EventDTO.builder()
                    .id(event.getId())
                    .titre(event.getTitre())
                    .description(event.getDescription())
                    .categorie(event.getCategorie())
                    .dateDebut(event.getDateDebut())
                    .dateFin(event.getDateFin())
                    .lieu(event.getLieu())
                    .ville(event.getVille())
                    .capaciteMax(event.getCapaciteMax())
                    .prixUnitaire(event.getPrixUnitaire())
                    .imageUrl(event.getImageUrl())
                    .statut(event.getStatut())
                    .dateCreation(event.getDateCreation())
                    .dateModification(event.getDateModification())
                    .placesReservees(placesReservees)
                    .placesDisponibles(placesDisponibles)
                    .tauxRemplissage(tauxRemplissage)
                    .revenuTotal(revenuTotal)
                    .isReservable(event.getStatut() == EventStatut.PUBLIE &&
                            LocalDateTime.now().isBefore(event.getDateDebut()) &&
                            placesDisponibles > 0)
                    .isModifiable(event.getStatut() != EventStatut.TERMINE &&
                            event.getStatut() != EventStatut.ANNULE)
                    .organisateurId(event.getOrganisateur().getId())
                    .organisateurNom(event.getOrganisateur().getNom())
                    .organisateurPrenom(event.getOrganisateur().getPrenom())
                    .organisateurNomComplet(event.getOrganisateur().getNomComplet())
                    .organisateurEmail(event.getOrganisateur().getEmail())
                    .organisateurTelephone(event.getOrganisateur().getTelephone())
                    .organisateurRole(event.getOrganisateur().getRole().getLabel())
                    .build();
        }
}