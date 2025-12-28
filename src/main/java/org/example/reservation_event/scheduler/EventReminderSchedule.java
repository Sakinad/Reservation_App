package org.example.reservation_event.scheduler;

import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.email.EmailService;
import org.example.reservation_event.repositories.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EventReminderSchedule {

    private static final Logger logger = LoggerFactory.getLogger(EventReminderSchedule.class);

    private final ReservationRepository reservationRepository;
    private final EmailService emailService;

    // Add a flag to prevent duplicate sending
    private boolean alreadySentToday = false;
    private LocalDateTime lastRunDate = null;

    public EventReminderSchedule(ReservationRepository reservationRepository,
                                 EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.emailService = emailService;
    }

    /**
     * Envoie des rappels pour les événements qui ont lieu dans 24h
     * Exécuté tous les jours à 9h du matin
     */
    @Scheduled(cron = "0 34 14 * * ?") // Tous les jours à 9h (plus standard)
    @Transactional
    public void send24HourReminders() {
        LocalDateTime now = LocalDateTime.now();

        // ✅ PREVENT DUPLICATE RUNS ON THE SAME DAY
        if (lastRunDate != null && lastRunDate.toLocalDate().equals(now.toLocalDate())) {
            logger.info("⏭️ Rappels déjà envoyés aujourd'hui, skipping...");
            return;
        }

        logger.info("🚀 Démarrage de l'envoi des rappels 24h avant événement...");

        LocalDateTime tomorrowStart = now.plusDays(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrowEnd = now.plusDays(1).withHour(23).withMinute(59).withSecond(59);

        List<Reservation> reservations = reservationRepository
                .findByEvenement_DateDebutBetween(tomorrowStart, tomorrowEnd);

        logger.info("📋 {} réservation(s) trouvée(s) pour les événements de demain", reservations.size());

        if (reservations.isEmpty()) {
            logger.info("⚠️ Aucune réservation trouvée pour demain");
            lastRunDate = now;
            return;
        }

        int sentCount = 0;
        int errorCount = 0;

        for (Reservation reservation : reservations) {
            try {
                // ✅ CHECK IF USER ALREADY RECEIVED REMINDER FOR THIS EVENT
                // You might want to add a field in Reservation like 'reminderSent' or
                // track in a separate table

                boolean emailSent = emailService.sendEventReminder(reservation);

                if (emailSent) {
                    sentCount++;
                    logger.info("✅ Rappel envoyé pour la réservation: {} à {}",
                            reservation.getCodeReservation(),
                            reservation.getUtilisateur().getEmail());

                    // ✅ OPTIONAL: Mark as sent in database
                    // reservation.setReminderSent(true);
                    // reservationRepository.save(reservation);
                } else {
                    errorCount++;
                    logger.error("❌ Échec d'envoi pour la réservation: {}",
                            reservation.getCodeReservation());
                }

            } catch (Exception e) {
                errorCount++;
                logger.error("💥 Erreur pour la réservation {}: {}",
                        reservation.getCodeReservation(), e.getMessage());
            }
        }

        // ✅ UPDATE LAST RUN DATE
        lastRunDate = now;
        logger.info("📊 Résultat final: {} email(s) envoyé(s), {} erreur(s)", sentCount, errorCount);
    }

    /**
     * 🧪 MÉTHODE DE TEST SEULEMENT - À DÉSACTIVER EN PRODUCTION
     * Commenter l'annotation @Scheduled pour désactiver
     */
    // @Scheduled(cron = "0 0 * * * ?") // Toutes les heures - DÉSACTIVÉ
    @Transactional
    public void testSchedulerHourly() {
        logger.info("🧪 TEST HEUREMENT - Cette méthode est désactivée en production");
        // Ne pas appeler send24HourReminders() ici
    }

    /**
     * 🧪 MÉTHODE DE TEST SEULEMENT - À DÉSACTIVER EN PRODUCTION
     */
    // @Scheduled(fixedDelay = 60000, initialDelay = 10000) // DÉSACTIVÉ
    @Transactional
    public void startupTest() {
        logger.info("🔧 TEST DÉMARRAGE - Cette méthode est désactivée en production");
    }
}