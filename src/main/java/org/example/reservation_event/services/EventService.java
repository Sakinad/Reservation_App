package org.example.reservation_event.services;

import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.Exceptions.*;
import org.example.reservation_event.dtos.EventDTO;
import org.example.reservation_event.dtos.EventLightDTO;
import org.example.reservation_event.dtos.ReservationDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public interface EventService {

    Event createEvent(Event event, User creator) throws BadRequestException, UnauthorizedException;

    Event updateEvent(Event event, User user) throws ForbiddenException, BusinessException;

    Event publishEvent(Event event) throws BusinessException;

    Event cancelEvent(Event event) throws BusinessException;

    void deleteEvent(Event event) throws BusinessException;

    List<Event> searchEvents(String keyword, String location, LocalDateTime start, LocalDateTime end,
                             Double minPrice, Double maxPrice, EventStatut statut);

    void updateTerminatedEvents();

    List<Event> getPopularEvents(int limit);

    Map<String, Object> getOrganizerStats(User organizer);

    Event getEventById(Long id) throws ResourceNotFoundException;
    boolean isReservable(Event event);
    int getPlacesDisponibles(Event event);
    Optional<EventDTO> getEventDTOById(Long id);
    Optional<EventDTO> getEventForReservation(Long id);
    List<EventLightDTO> getAllClientEvents(
            String search,
            String ville,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Double prixMin,
            Double prixMax
    );
    List<Event> getOrganizerRecentEvents(Long organizerId, int limit);
    List<Event> getOrganizerEvents(User organizer);
    Event saveEvent(Event event);
    Event updateEvent(Event event);
    List<Event> getEventsByOrganizer(User organizer);
    // Dans EventService.java, ajoute :
    List<Event> getPastEventsNotReviewed(Long userId);
}