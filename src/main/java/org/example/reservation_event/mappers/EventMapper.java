package org.example.reservation_event.mappers;

import org.example.reservation_event.classes.Event;
import org.example.reservation_event.dtos.EventDTO;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.Enums.ReservationStatut;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class EventMapper {

    public EventDTO toDTO(Event event) {
        if (event == null) return null;

        // Calculer les places réservées
        int placesReservees = event.getReservations() != null ?
                event.getReservations().stream()
                        .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE ||
                                r.getStatut() == ReservationStatut.EN_ATTENTE)
                        .mapToInt(r -> r.getNombrePlaces())
                        .sum() : 0;

        int placesDisponibles = event.getCapaciteMax() - placesReservees;
        double tauxRemplissage = event.getCapaciteMax() > 0 ?
                (double) placesReservees / event.getCapaciteMax() * 100 : 0;

        // Calculer le revenu total
        double revenuTotal = event.getReservations() != null ?
                event.getReservations().stream()
                        .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                        .mapToDouble(r -> r.getMontantTotal())
                        .sum() : 0;

        EventDTO dto = EventDTO.builder()
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
                .build();

        // Infos organisateur (with null checks)
        if (event.getOrganisateur() != null) {
            dto.setOrganisateurId(event.getOrganisateur().getId());
            dto.setOrganisateurNom(event.getOrganisateur().getNom());
            dto.setOrganisateurPrenom(event.getOrganisateur().getPrenom());
            dto.setOrganisateurNomComplet(event.getOrganisateur().getNomComplet());
            dto.setOrganisateurEmail(event.getOrganisateur().getEmail());
            dto.setOrganisateurTelephone(event.getOrganisateur().getTelephone());
            if (event.getOrganisateur().getRole() != null) {
                dto.setOrganisateurRole(event.getOrganisateur().getRole().getLabel());
            }
        }

        return dto;
    }

    public EventDTO toDTOWithStats(Event event, Integer placesReservees) {
        EventDTO dto = toDTO(event);
        if (dto != null && placesReservees != null) {
            dto.setPlacesReservees(placesReservees);
            dto.setPlacesDisponibles(event.getCapaciteMax() - placesReservees);
            dto.setTauxRemplissage(event.getCapaciteMax() > 0 ?
                    (double) placesReservees / event.getCapaciteMax() * 100 : 0);
        }
        return dto;
    }

    public EventDTO toReservationFormDTO(Event event, Integer placesDisponibles) {
        if (event == null) return null;

        EventDTO dto = toDTO(event);
        if (placesDisponibles != null) {
            dto.setPlacesDisponibles(placesDisponibles);
            dto.setIsReservable(isReservable(event, placesDisponibles));
        }
        return dto;
    }

    private boolean isReservable(Event event, Integer placesDisponibles) {
        return event != null &&
                event.getStatut() == EventStatut.PUBLIE &&
                event.getDateDebut().isAfter(LocalDateTime.now()) &&
                placesDisponibles != null && placesDisponibles > 0;
    }
}