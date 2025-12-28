package org.example.reservation_event.services;

import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.Enums.ReservationStatut;
import org.example.reservation_event.Exceptions.*;
import org.example.reservation_event.dtos.ReservationSummaryDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public interface ReservationService {

    Reservation createReservation(Event event, User user, int nombrePlaces, String commentaire)
            throws BadRequestException, ConflictException, BusinessException;

    Reservation confirmReservation(Reservation reservation) throws BusinessException;

    void cancelReservation(Long reservationId) throws BusinessException;

    Reservation getReservationByCode(String code) throws ResourceNotFoundException;

    List<Reservation> getReservationsByUser(User user);

    List<Reservation> getReservationsByEvent(Event event, ReservationStatut statut);

    double calculateTotalRevenueByUser(User user);

    Reservation getReservationByIdWithRelations(Long id);

    Map<String, Object> getUserStats(User user);

    List<Reservation> getReservationsBetweenDates(LocalDateTime start, LocalDateTime end);
    List<ReservationSummaryDTO> getReservationsByUserAsDTO(User user);

    List<ReservationSummaryDTO> getUpcomingReservationsByUser(User user);
    Reservation getReservationById(Long id);
    int getTotalReservedPlaces(Long eventId);
    List<Reservation> getReservationsByEvent(Event event);
    Reservation saveReservation(Reservation reservation);
    boolean hasUserReservedEvent(Event event, User user);
}