package org.example.reservation_event.ServicesImplimentation;

import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.Enums.ReservationStatut;
import org.example.reservation_event.Exceptions.*;
import org.example.reservation_event.dtos.EventDTO;
import org.example.reservation_event.dtos.EventLightDTO;
import org.example.reservation_event.dtos.ReservationDTO;
import org.example.reservation_event.dtos.ReservationSummaryDTO;
import org.example.reservation_event.mappers.EventMapper;
import org.example.reservation_event.mappers.ReservationMapper;
import org.example.reservation_event.repositories.EventRepository;
import org.example.reservation_event.repositories.ReservationRepository;
import org.example.reservation_event.services.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    @Autowired
    private ReservationMapper reservationMapper;
    @Autowired
    private EventMapper eventMapper;
    // Récupérer les réservations d'un utilisateur avec DTOs
    public List<ReservationSummaryDTO> getReservationsByUserAsDTO(User user) {
        List<Reservation> reservations = reservationRepository.findByUtilisateur(user);
        return reservations.stream()
                .map(reservationMapper::toSummaryDTO)
                .collect(Collectors.toList());
    }

    // Récupérer les réservations à venir pour le dashboard
    public List<ReservationSummaryDTO> getUpcomingReservationsByUser(User user) {
        List<Reservation> reservations = reservationRepository
                .findByUtilisateurAndStatutAndEvenementDateDebutAfter(
                        user,
                        ReservationStatut.CONFIRMEE,
                        LocalDateTime.now()
                );

        return reservations.stream()
                .map(reservationMapper::toSummaryDTO)
                .collect(Collectors.toList());
    }
    public EventServiceImpl(EventRepository eventRepository, ReservationRepository reservationRepository) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public Event createEvent(Event event, User creator) throws BadRequestException, UnauthorizedException {
        if (creator == null || !creator.getRole().canCreateEvents()) {
            throw new UnauthorizedException("Vous n'avez pas le droit de créer un événement");
        }
        if (event.getTitre() == null || event.getTitre().length() < 5) {
            throw new BadRequestException("Le titre de l'événement est invalide");
        }
        event.setOrganisateur(creator);
        return eventRepository.save(event);
    }

    @Override
    public Event updateEvent(Event event, User user) throws ForbiddenException, BusinessException {
        try {
            // Vérifier que l'événement existe
            Event existingEvent = eventRepository.findById(event.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé"));

            // Mettre à jour uniquement les champs modifiables
            existingEvent.setTitre(event.getTitre());
            existingEvent.setDescription(event.getDescription());
            existingEvent.setCategorie(event.getCategorie());
            existingEvent.setDateDebut(event.getDateDebut());
            existingEvent.setDateFin(event.getDateFin());
            existingEvent.setLieu(event.getLieu());
            existingEvent.setVille(event.getVille());
            existingEvent.setCapaciteMax(event.getCapaciteMax());
            existingEvent.setPrixUnitaire(event.getPrixUnitaire());
            existingEvent.setImageUrl(event.getImageUrl());
            existingEvent.setDateModification(LocalDateTime.now());

            // Sauvegarder
            return eventRepository.save(existingEvent);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la mise à jour: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la sauvegarde de l'événement", e);
        }
    }

    @Override
    public Event publishEvent(Event event) throws BusinessException {
        if (!event.getStatut().canBePublished()) {
            throw new BusinessException("Impossible de publier cet événement");
        }
        event.setStatut(EventStatut.PUBLIE);
        return eventRepository.save(event);
    }

    @Override
    public Event cancelEvent(Event event) throws BusinessException {
        if (event.getStatut() == EventStatut.ANNULE || event.getStatut() == EventStatut.TERMINE) {
            throw new BusinessException("Événement ne peut pas être annulé");
        }
        event.setStatut(EventStatut.ANNULE);
        event.getReservations().forEach(res -> res.setStatut(ReservationStatut.ANNULEE));
        return eventRepository.save(event);
    }

    @Override
    public void deleteEvent(Event event) throws BusinessException {
        if (!event.getReservations().isEmpty()) {
            throw new BusinessException("Impossible de supprimer un événement avec des réservations");
        }
        eventRepository.delete(event);
    }

    @Override
    public List<Event> searchEvents(String keyword, String location, LocalDateTime start, LocalDateTime end,
                                    Double minPrice, Double maxPrice, EventStatut statut) {
        return eventRepository.findAll().stream()
                .filter(e -> statut == null || e.getStatut() == statut)
                .filter(e -> keyword == null || e.getTitre().toLowerCase().contains(keyword.toLowerCase()))
                .filter(e -> location == null ||
                        e.getLieu().toLowerCase().contains(location.toLowerCase()) ||
                        e.getVille().toLowerCase().contains(location.toLowerCase()))
                .filter(e -> start == null || !e.getDateDebut().isBefore(start))
                .filter(e -> end == null || !e.getDateFin().isAfter(end))
                .filter(e -> minPrice == null || e.getPrixUnitaire() >= minPrice)
                .filter(e -> maxPrice == null || e.getPrixUnitaire() <= maxPrice)
                .collect(Collectors.toList());
    }

    @Override
    public void updateTerminatedEvents() {
        LocalDateTime now = LocalDateTime.now();
        eventRepository.findAll().stream()
                .filter(e -> e.getStatut() != EventStatut.TERMINE && e.getDateFin().isBefore(now))
                .forEach(e -> e.setStatut(EventStatut.TERMINE));
    }

    @Override
    public List<Event> getPopularEvents(int limit) {
        LocalDateTime now = LocalDateTime.now();

        return eventRepository.findAll().stream()
                // Filter only published events
                .filter(e -> e.getStatut() == EventStatut.PUBLIE)
                // Filter events that haven't passed yet
                .filter(e -> e.getDateDebut().isAfter(now))
                // Filter events that are still reservable
                .filter(e -> e.isReservable())
                // Filter events that still have available places
                .filter(e -> e.getPlacesDisponibles() > 0)
                // Sort by popularity (number of confirmed reservations)
                .sorted((e1, e2) -> Integer.compare(
                        e2.getReservations().stream()
                                .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                                .mapToInt(r -> r.getNombrePlaces())
                                .sum(),
                        e1.getReservations().stream()
                                .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                                .mapToInt(r -> r.getNombrePlaces())
                                .sum()
                ))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getOrganizerStats(User organizer) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Get ALL events for this organizer (not just published)
            List<Event> allEvents = eventRepository.findByOrganisateur(organizer);

            // Debug logging
            System.out.println("DEBUG: Found " + allEvents.size() + " events for organizer: " + organizer.getId());
            allEvents.forEach(e -> System.out.println("  - " + e.getTitre() + " | Status: " + e.getStatut()));

            int totalEvents = allEvents.size();

            // Count published events
            int publishedEvents = (int) allEvents.stream()
                    .filter(e -> e.getStatut() == EventStatut.PUBLIE)
                    .count();

            // Count draft events
            int draftEvents = (int) allEvents.stream()
                    .filter(e -> e.getStatut() == EventStatut.BROUILLON)
                    .count();

            // Get ALL reservations from ALL events (not just published)
            int totalReservations = 0;
            double totalRevenue = 0.0;

            for (Event event : allEvents) {
                if (event.getReservations() != null) {
                    List<Reservation> confirmedReservations = event.getReservations().stream()
                            .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                            .collect(Collectors.toList());

                    totalReservations += confirmedReservations.size();
                    totalRevenue += confirmedReservations.stream()
                            .mapToDouble(Reservation::getMontantTotal)
                            .sum();
                }
            }

            // Put all stats
            stats.put("totalEvents", totalEvents);
            stats.put("publishedEvents", publishedEvents);
            stats.put("draftEvents", draftEvents);
            stats.put("totalReservations", totalReservations);
            stats.put("totalRevenue", totalRevenue);

            // Debug output
            System.out.println("DEBUG Stats:");
            System.out.println("  totalEvents: " + totalEvents);
            System.out.println("  publishedEvents: " + publishedEvents);
            System.out.println("  draftEvents: " + draftEvents);
            System.out.println("  totalReservations: " + totalReservations);
            System.out.println("  totalRevenue: " + totalRevenue);

        } catch (Exception e) {
            System.err.println("ERROR in getOrganizerStats: " + e.getMessage());
            e.printStackTrace();

            // Fallback
            stats.put("totalEvents", 0);
            stats.put("publishedEvents", 0);
            stats.put("draftEvents", 0);
            stats.put("totalReservations", 0);
            stats.put("totalRevenue", 0.0);
        }

        return stats;
    }

    @Override
    public Event getEventById(Long id) throws ResourceNotFoundException {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé"));
    }
    @Override
    public boolean isReservable(Event event) {
        return event != null &&
                event.getStatut() == EventStatut.PUBLIE &&
                event.getDateDebut().isAfter(LocalDateTime.now());
    }

    @Override
    public int getPlacesDisponibles(Event event) {
        if (event == null) return 0;

        Integer placesReservees = reservationRepository.countTotalPlacesReserved(event);
        if (placesReservees == null) placesReservees = 0;

        return event.getCapaciteMax() - placesReservees;
    }
    @Override
    public Optional<EventDTO> getEventDTOById(Long id) {
        return eventRepository.findById(id)
                .map(eventMapper::toDTO);
    }

    @Override
    public Optional<EventDTO> getEventForReservation(Long id) {
        return eventRepository.findById(id)
                .map(event -> {
                    Integer placesDisponibles = getPlacesDisponibles(event);
                    return eventMapper.toReservationFormDTO(event, placesDisponibles);
                });
    }
    @Override
    @Transactional(readOnly = true)
    public List<EventLightDTO> getAllClientEvents(
            String search,
            String ville,
            LocalDateTime dateDebut,
            LocalDateTime dateFin,
            Double prixMin,
            Double prixMax
    ) {
        LocalDateTime now = LocalDateTime.now(); // Date actuelle
        List<Event> events = eventRepository.searchEventsClient(
                search, ville, dateDebut, dateFin, prixMin, prixMax, now
        );

        return events.stream()
                .map(EventLightDTO::fromEntity)
                .collect(Collectors.toList());
    }
    public List<Event> getOrganizerRecentEvents(Long organizerId, int limit) {
        List<Event> allEvents = eventRepository.findByOrganisateurId(organizerId);

        // Trier par date de création décroissante et limiter
        return allEvents.stream()
                .sorted((e1, e2) -> e2.getDateCreation().compareTo(e1.getDateCreation()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    @Override
    public List<Event> getOrganizerEvents(User organizer) {
        return eventRepository.findByOrganisateur(organizer);
    }
    @Override
    public Event saveEvent(Event event) {
        try {
            // Set modification date
            event.setDateModification(LocalDateTime.now());

            // If it's a new event (no ID), set creation date
            if (event.getId() == null) {
                event.setDateCreation(LocalDateTime.now());
            }

            return eventRepository.save(event);

        } catch (Exception e) {
            System.err.println("Error saving event: " + e.getMessage());
            throw new RuntimeException("Erreur lors de la sauvegarde de l'événement", e);
        }
    }
    // Dans EventServiceImpl.java, ajoute :
    @Override
    public List<Event> getPastEventsNotReviewed(Long userId) {
        return eventRepository.findPastEventsWithReservationAndNoReview(userId);
    }
    public Event updateEvent(Event event) {
        event.setDateModification(LocalDateTime.now());
        return eventRepository.save(event);
    }
    @Override
    public List<Event> getEventsByOrganizer(User organizer) {
        try {
            return eventRepository.findByOrganisateur(organizer);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des événements: " + e.getMessage(), e);
        }
    }
}
