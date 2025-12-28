package org.example.reservation_event.repositories;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.Enums.EventCategorie;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.classes.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    // Trouver les événements par catégorie
    List<Event> findByCategorie(EventCategorie categorie);
    List<Event> findByOrganisateur(User organisateur);
    // Trouver les événements publiés entre deux dates
    List<Event> findByStatutAndDateDebutBetween(EventStatut statut, LocalDateTime start, LocalDateTime end);
    // Trouver les événements d'un organisateur avec un statut donné
    List<Event> findByOrganisateurAndStatut(User organisateur, EventStatut statut);
    // Trouver les événements disponibles (publiés et non terminés)
    List<Event> findByOrganisateurId(Long organizerId);
    List<Event> findByStatut(EventStatut statut);
    List<Event> findByStatutAndDateFinBefore(EventStatut statut, LocalDateTime date);

    @Query("SELECT e FROM Event e WHERE e.statut = 'PUBLIE' AND e.dateFin > :now")
    List<Event> findAvailableEvents(@Param("now") LocalDateTime now);
    // Compter le nombre d'événements par catégorie
    @Query("SELECT COUNT(e) FROM Event e WHERE e.categorie = :categorie")
    long countByCategorie(@Param("categorie") EventCategorie categorie);
    // Trouver les événements par lieu ou ville
    @Query("SELECT e FROM Event e WHERE LOWER(e.lieu) LIKE LOWER(CONCAT('%', :location, '%')) " +
            "OR LOWER(e.ville) LIKE LOWER(CONCAT('%', :location, '%'))")
    List<Event> findByLieuOrVille(@Param("location") String location);
    // Rechercher les événements par titre (contenant un mot-clé)
    List<Event> findByTitreContainingIgnoreCase(String keyword);
    // Trouver les événements par plage de prix
    List<Event> findByPrixUnitaireBetween(Double minPrice, Double maxPrice);
    @Query("SELECT e FROM Event e " +
            "WHERE e.statut = 'PUBLIE' " +
            "AND e.dateDebut > :now " +
            "AND (:search IS NULL OR LOWER(e.titre) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:ville IS NULL OR LOWER(e.ville) LIKE LOWER(CONCAT('%', :ville, '%'))) " +
            "AND (:dateDebut IS NULL OR e.dateDebut >= :dateDebut) " +
            "AND (:dateFin IS NULL OR e.dateDebut <= :dateFin) " +
            "AND (:prixMin IS NULL OR e.prixUnitaire >= :prixMin) " +
            "AND (:prixMax IS NULL OR e.prixUnitaire <= :prixMax) " +
            "AND e.capaciteMax > (SELECT COALESCE(SUM(r.nombrePlaces), 0) " +
            "                     FROM Reservation r " +
            "                     WHERE r.evenement = e " +
            "                     AND r.statut IN ('CONFIRMEE', 'EN_ATTENTE')) " +
            "ORDER BY e.dateDebut ASC")
    List<Event> searchEventsClient(
            @Param("search") String search,
            @Param("ville") String ville,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("prixMin") Double prixMin,
            @Param("prixMax") Double prixMax,
            @Param("now") LocalDateTime now
    );
    // Dans EventRepository.java, ajoute :
    @Query("""
    SELECT DISTINCT e FROM Event e
    JOIN Reservation r ON r.evenement = e
    WHERE r.utilisateur.id = :userId
    AND e.statut = 'TERMINE'
    AND e.dateDebut < CURRENT_TIMESTAMP
    AND NOT EXISTS (
        SELECT rev FROM Review rev 
        WHERE rev.event.id = e.id AND rev.user.id = :userId
    )
    ORDER BY e.dateDebut DESC
""")
    List<Event> findPastEventsWithReservationAndNoReview(@Param("userId") Long userId);
    @Query("SELECT e FROM Event e " +
            "WHERE e.statut = :statut " +
            "AND e.dateDebut < :now " +
            "AND e.dateFin > :now")
    List<Event> findEventsInProgress(
            @Param("statut") EventStatut statut,
            @Param("now") LocalDateTime now);

    /**
     * Trouve les événements PUBLIE qui sont à venir
     * (dateDebut > maintenant)
     */
    List<Event> findByStatutAndDateDebutAfter(EventStatut statut, LocalDateTime date);

    /**
     * Trouve les événements qui doivent être mis à jour en TERMINE
     * (PUBLIE et dateFin < maintenant)
     */
    @Query("SELECT e FROM Event e " +
            "WHERE e.statut IN (:status) " +
            "AND e.dateFin < :now " +
            "AND e.statut != org.example.reservation_event.Enums.EventStatut.ANNULE")
    List<Event> findEventsToUpdateToTermine(
            @Param("status") List<EventStatut> status,
            @Param("now") LocalDateTime now);
    @Query("SELECT e FROM Event e WHERE e.statut = 'PUBLIE' AND e.dateFin < :cutoffTime")
    List<Event> findEventsToTerminate(@Param("cutoffTime") LocalDateTime cutoffTime);
    @Modifying
    @Query("UPDATE Event e SET e.statut = 'TERMINE' WHERE e.id = :eventId AND e.statut = 'PUBLIE'")
    int forceUpdateToTermine(@Param("eventId") Long eventId);

    /**
     * Bulk update for past events
     */
    @Modifying
    @Query("UPDATE Event e SET e.statut = 'TERMINE' " +
            "WHERE e.statut = 'PUBLIE' AND e.dateFin < :cutoffTime")
    int bulkUpdatePastEvents(@Param("cutoffTime") LocalDateTime cutoffTime);
}
