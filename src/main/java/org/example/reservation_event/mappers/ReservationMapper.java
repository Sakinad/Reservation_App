package org.example.reservation_event.mappers;

import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.dtos.ReservationDTO;
import org.example.reservation_event.dtos.ReservationSummaryDTO;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public ReservationDTO toDTO(Reservation reservation) {
        if (reservation == null) return null;

        ReservationDTO dto = new ReservationDTO();
        dto.setId(reservation.getId());
        dto.setCodeReservation(reservation.getCodeReservation());
        dto.setNombrePlaces(reservation.getNombrePlaces());
        dto.setMontantTotal(reservation.getMontantTotal());
        dto.setDateReservation(reservation.getDateReservation());
        dto.setStatut(reservation.getStatut());
        dto.setCommentaire(reservation.getCommentaire());

        // Infos utilisateur
        if (reservation.getUtilisateur() != null) {
            dto.setUtilisateurId(reservation.getUtilisateur().getId());
            dto.setUtilisateurNom(reservation.getUtilisateur().getNom());
            dto.setUtilisateurPrenom(reservation.getUtilisateur().getPrenom());
            dto.setUtilisateurEmail(reservation.getUtilisateur().getEmail());
        }

        // Infos événement
        if (reservation.getEvenement() != null) {
            dto.setEvenementId(reservation.getEvenement().getId());
            dto.setEvenementTitre(reservation.getEvenement().getTitre());
            dto.setEvenementDateDebut(reservation.getEvenement().getDateDebut());
            dto.setEvenementLieu(reservation.getEvenement().getLieu());
            dto.setEvenementVille(reservation.getEvenement().getVille());
            dto.setEvenementPrixUnitaire(reservation.getEvenement().getPrixUnitaire());
        }

        return dto;
    }

    public ReservationSummaryDTO toSummaryDTO(Reservation reservation) {
        if (reservation == null) return null;

        ReservationSummaryDTO dto = new ReservationSummaryDTO();
        dto.setId(reservation.getId());
        dto.setCodeReservation(reservation.getCodeReservation());
        dto.setNombrePlaces(reservation.getNombrePlaces());
        dto.setMontantTotal(reservation.getMontantTotal());
        dto.setDateReservation(reservation.getDateReservation());
        dto.setStatut(reservation.getStatut());

        // Seulement les infos essentielles de l'événement
        if (reservation.getEvenement() != null) {
            dto.setEvenementTitre(reservation.getEvenement().getTitre());
            dto.setEvenementDateDebut(reservation.getEvenement().getDateDebut());
            dto.setEvenementLieu(reservation.getEvenement().getLieu());
        }

        return dto;
    }
}