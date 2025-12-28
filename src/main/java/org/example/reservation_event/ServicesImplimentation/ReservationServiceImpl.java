package org.example.reservation_event.ServicesImplimentation;

import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.Enums.ReservationStatut;
import org.example.reservation_event.Exceptions.*;
import org.example.reservation_event.repositories.ReservationRepository;
import org.example.reservation_event.repositories.EventRepository;
import org.example.reservation_event.services.ReservationService;
import org.example.reservation_event.dtos.ReservationSummaryDTO;
import org.example.reservation_event.mappers.ReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;

    @Autowired
    private ReservationMapper reservationMapper;

    public ReservationServiceImpl(ReservationRepository reservationRepository, EventRepository eventRepository) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    @Override
    public Reservation createReservation(Event event, User user, int nombrePlaces, String commentaire)
            throws BadRequestException, ConflictException, BusinessException {

        if (event == null) throw new BadRequestException("Événement invalide");
        if (!event.isReservable()) throw new BusinessException("L'événement n'est pas réservable");
        if (nombrePlaces < 1 || nombrePlaces > 10) throw new BadRequestException("Nombre de places invalide");
        if (!event.hasPlacesDisponibles(nombrePlaces))
            throw new ConflictException("Pas assez de places disponibles");

        // Debug logging
        System.out.println("=== CREATING RESERVATION ===");
        System.out.println("Event available places before: " + event.getPlacesDisponibles());

        // Create reservation
        Reservation reservation = Reservation.builder()
                .evenement(event)
                .utilisateur(user)
                .nombrePlaces(nombrePlaces)
                .commentaire(commentaire)
                .build();

        // Check status after building
        System.out.println("Reservation status after build: " + reservation.getStatut());

        reservation.calculerMontantTotal();

        // Check status before save
        System.out.println("Reservation status before save: " + reservation.getStatut());

        Reservation savedReservation = reservationRepository.save(reservation);

        // Check status after save
        System.out.println("Reservation status after save: " + savedReservation.getStatut());

        // Refresh event to see available places
        event = eventRepository.findById(event.getId()).orElseThrow();
        System.out.println("Event available places after: " + event.getPlacesDisponibles());
        System.out.println("=== RESERVATION CREATED ===");

        return savedReservation;
    }

    @Override
    public Reservation confirmReservation(Reservation reservation) throws BusinessException {
        if (reservation.getStatut() != ReservationStatut.EN_ATTENTE) {
            throw new BusinessException("Seule une réservation en attente peut être confirmée");
        }
        reservation.setStatut(ReservationStatut.CONFIRMEE);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));

        // Logique d'annulation existante...
        reservation.setStatut(ReservationStatut.ANNULEE);
        reservationRepository.save(reservation);
    }

    @Override
    public Reservation getReservationByCode(String code) throws ResourceNotFoundException {
        return reservationRepository.findByCodeReservation(code)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable"));
    }

    @Override
    public List<Reservation> getReservationsByUser(User user) {
        return reservationRepository.findByUtilisateur(user);
    }

    @Override
    public List<Reservation> getReservationsByEvent(Event event, ReservationStatut statut) {
        return reservationRepository.findByEvenementAndStatut(event, statut);
    }

    @Override
    public double calculateTotalRevenueByUser(User user) {
        return reservationRepository.findByUtilisateur(user).stream()
                .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                .mapToDouble(Reservation::getMontantTotal)
                .sum();
    }

    @Override
    public Map<String, Object> getUserStats(User user) {
        List<Reservation> reservations = getReservationsByUser(user);
        long totalReservations = reservations.size();
        long upcomingReservations = reservations.stream().filter(Reservation::isAVenir).count();
        double totalSpent = reservations.stream()
                .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                .mapToDouble(Reservation::getMontantTotal)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReservations", totalReservations);
        stats.put("upcomingReservations", upcomingReservations);
        stats.put("totalSpent", totalSpent);
        return stats;
    }

    @Override
    public List<Reservation> getReservationsBetweenDates(LocalDateTime start, LocalDateTime end) {
        return reservationRepository.findByDateReservationBetween(start, end);
    }

    // NOUVELLES MÉTHODES IMPLÉMENTÉES

    @Override
    public List<ReservationSummaryDTO> getReservationsByUserAsDTO(User user) {
        try {
            // Essayer d'abord avec la méthode repository qui retourne directement les DTOs
            if (reservationRepositoryHasDTOMethods()) {
                List<ReservationSummaryDTO> dtos = reservationRepository.findReservationsByUserAsDTO(user);

                // ✅ AJOUT : Enrichir les DTOs avec le statut de l'événement
                dtos.forEach(dto -> {
                    eventRepository.findById(dto.getEvenementId())
                            .ifPresent(event -> dto.setEvenementStatut(event.getStatut()));
                });

                return dtos;
            }
        } catch (Exception e) {
            System.out.println("Méthode DTO du repository non disponible, utilisation du fallback: " + e.getMessage());
        }

        // Fallback: récupérer les entités et les mapper
        List<Reservation> reservations = reservationRepository.findByUtilisateur(user);
        return reservations.stream()
                .map(reservation -> {
                    ReservationSummaryDTO dto = new ReservationSummaryDTO();
                    dto.setId(reservation.getId());
                    dto.setCodeReservation(reservation.getCodeReservation());
                    dto.setNombrePlaces(reservation.getNombrePlaces());
                    dto.setMontantTotal(reservation.getMontantTotal());
                    dto.setDateReservation(reservation.getDateReservation());
                    dto.setStatut(reservation.getStatut());

                    // Charger l'événement si nécessaire (éviter LazyInitialization)
                    if (reservation.getEvenement() != null) {
                        dto.setEvenementId(reservation.getEvenement().getId());
                        dto.setEvenementTitre(reservation.getEvenement().getTitre());
                        dto.setEvenementDateDebut(reservation.getEvenement().getDateDebut());
                        dto.setEvenementDateFin(reservation.getEvenement().getDateFin());
                        dto.setEvenementLieu(reservation.getEvenement().getLieu());
                        dto.setEvenementVille(reservation.getEvenement().getVille());
                        dto.setEvenementPrixUnitaire(reservation.getEvenement().getPrixUnitaire());
                        dto.setEvenementStatut(reservation.getEvenement().getStatut()); // ✅ AJOUT ICI
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationSummaryDTO> getUpcomingReservationsByUser(User user) {
        try {
            // Essayer d'abord avec la méthode repository DTO
            if (reservationRepositoryHasDTOMethods()) {
                return reservationRepository.findUpcomingReservationsByUser(
                        user,
                        ReservationStatut.CONFIRMEE,
                        LocalDateTime.now()
                );
            }
        } catch (Exception e) {
            System.out.println("Méthode upcoming DTO non disponible, utilisation du fallback: " + e.getMessage());
        }

        // Fallback: méthode alternative avec filtre manuel
        List<Reservation> reservations = reservationRepository.findByUtilisateurAndStatut(
                user,
                ReservationStatut.CONFIRMEE
        );

        return reservations.stream()
                .filter(reservation -> {
                    try {
                        return reservation.getEvenement().getDateDebut().isAfter(LocalDateTime.now());
                    } catch (Exception e) {
                        System.out.println("Erreur lors de l'accès à la date de l'événement: " + e.getMessage());
                        return false;
                    }
                })
                .map(reservation -> {
                    ReservationSummaryDTO dto = new ReservationSummaryDTO();
                    dto.setId(reservation.getId());
                    dto.setCodeReservation(reservation.getCodeReservation());
                    dto.setNombrePlaces(reservation.getNombrePlaces());
                    dto.setMontantTotal(reservation.getMontantTotal());
                    dto.setDateReservation(reservation.getDateReservation());
                    dto.setStatut(reservation.getStatut());

                    if (reservation.getEvenement() != null) {
                        dto.setEvenementTitre(reservation.getEvenement().getTitre());
                        dto.setEvenementDateDebut(reservation.getEvenement().getDateDebut());
                        dto.setEvenementLieu(reservation.getEvenement().getLieu());
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Méthode utilitaire pour vérifier si les méthodes DTO existent dans le repository
    private boolean reservationRepositoryHasDTOMethods() {
        try {
            // Tester si la méthode existe en essayant de l'appeler avec des paramètres nuls
            // Cela va échouer mais nous indiquera si la méthode existe
            reservationRepository.getClass().getMethod("findReservationsByUserAsDTO", User.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public Reservation getReservationById(Long id) {
        return reservationRepository.findByIdWithEvent(id);
    }

    public int getTotalReservedPlaces(Long eventId) {
        List<Reservation> reservations = reservationRepository.findByEvenementId(eventId);

        return reservations.stream()
                .filter(r -> r.getStatut() != ReservationStatut.ANNULEE)
                .mapToInt(Reservation::getNombrePlaces)
                .sum();
    }

// Add these methods to your ReservationService class

    /**
     * Save or update a reservation
     *
     * @param reservation The reservation to save
     * @return The saved reservation
     */
    public Reservation saveReservation(Reservation reservation) {
        try {
            return reservationRepository.save(reservation);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la sauvegarde de la réservation: " + e.getMessage(), e);

        }
    }

    /**
     * Get all reservations for a specific event
     * @param event The event
     * @return List of reservations for this event
     */
    @Override
    public List<Reservation> getReservationsByEvent(Event event) {
        try {
            // This will keep the session open
            return reservationRepository.findByEvenement(event);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des réservations: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true) // ✅ Garde la session ouverte pendant la lecture
    public Reservation getReservationByIdWithRelations(Long id) {
        Reservation reservation = reservationRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));

        // Force le chargement de toutes les relations pour éviter lazy loading
        reservation.getUtilisateur().getNom(); // Trigger
        reservation.getEvenement().getTitre(); // Trigger

        if (reservation.getEvenement().getOrganisateur() != null) {
            reservation.getEvenement().getOrganisateur().getNom(); // Trigger
        }

        return reservation;
    }
    @Override
    public boolean hasUserReservedEvent(Event event, User user) {
        // Vérifie s'il existe au moins une réservation confirmée
        return reservationRepository.existsByEvenementAndUtilisateurAndStatut(
                event, user, ReservationStatut.CONFIRMEE);
    }
}