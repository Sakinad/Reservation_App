package org.example.reservation_event.email;

import org.example.reservation_event.classes.Reservation;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendVerificationEmail(String toEmail, String verificationCode) {
        try {
            System.out.println("📧 Attempting to send verification email to: " + toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("reservation.event.app@gmail.com");
            message.setTo(toEmail);
            message.setSubject("Vérification de votre email - Reservation Event");
            message.setText(
                    "Bonjour,\n\n" +
                            "Merci de vous être inscrit sur Reservation Event.\n\n" +
                            "Votre code de vérification est : " + verificationCode + "\n\n" +
                            "Ce code expirera dans 10 minutes.\n\n" +
                            "Si vous n'avez pas créé de compte, veuillez ignorer cet email.\n\n" +
                            "Cordialement,\nL'équipe Reservation Event"
            );

            mailSender.send(message);
            System.out.println("✅ Email sent successfully to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendEmailChangeVerification(String toEmail, String verificationCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("reservation.event.app@gmail.com");
            message.setTo(toEmail);
            message.setSubject("Vérification de changement d'email - Reservation Event");
            message.setText(
                    "Bonjour,\n\n" +
                            "Vous avez demandé à changer votre adresse email sur Reservation Event.\n\n" +
                            "Votre code de vérification est : " + verificationCode + "\n\n" +
                            "Ce code expirera dans 10 minutes.\n\n" +
                            "Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet email.\n\n" +
                            "Cordialement,\nL'équipe Reservation Event"
            );

            mailSender.send(message);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public boolean sendPasswordResetEmail(String toEmail, String resetCode) {
        try {
            System.out.println("🔑 Attempting to send password reset email to: " + toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("reservation.event.app@gmail.com");
            message.setTo(toEmail);
            message.setSubject("Réinitialisation de votre mot de passe - Reservation Event");
            message.setText(
                    "Bonjour,\n\n" +
                            "Vous avez demandé à réinitialiser votre mot de passe sur Reservation Event.\n\n" +
                            "Votre code de vérification est : " + resetCode + "\n\n" +
                            "Ce code expirera dans 10 minutes.\n\n" +
                            "Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet email " +
                            "et votre mot de passe restera inchangé.\n\n" +
                            "Pour des raisons de sécurité, ne partagez jamais ce code avec qui que ce soit.\n\n" +
                            "Cordialement,\nL'équipe Reservation Event"
            );

            mailSender.send(message);
            System.out.println("✅ Password reset email sent successfully to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to send password reset email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    public boolean sendReservationConfirmation(Reservation reservation) {
        try {
            System.out.println("🎟️ Attempting to send reservation confirmation email to: "
                    + reservation.getUtilisateur().getEmail());

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("reservation.event.app@gmail.com");
            message.setTo(reservation.getUtilisateur().getEmail());
            message.setSubject("Confirmation de votre réservation - Reservation Event");

            String eventName = reservation.getEvenement().getTitre();
            String eventDate = reservation.getEvenement().getDateDebut().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String venue = reservation.getEvenement().getLieu() + ", " + reservation.getEvenement().getVille();

            message.setText(
                    "Bonjour " + reservation.getUtilisateur().getPrenom() + ",\n\n" +
                            "Nous vous confirmons votre réservation pour l'événement suivant :\n\n" +
                            "📌 Événement : " + eventName + "\n" +
                            "📅 Date : " + eventDate + "\n" +
                            "📍 Lieu : " + venue + "\n" +
                            "🎟️ Code de réservation : " + reservation.getCodeReservation() + "\n" +
                            "👥 Nombre de places : " + reservation.getNombrePlaces() + "\n" +
                            "💰 Montant total : " + String.format("%.2f DH", reservation.getMontantTotal()) + "\n\n" +

                            "📋 Récapitulatif de votre réservation :\n" +
                            "----------------------------------------\n" +
                            "Code : " + reservation.getCodeReservation() + "\n" +
                            "Événement : " + eventName + "\n" +
                            "Date : " + eventDate + "\n" +
                            "Lieu : " + venue + "\n" +
                            "Nombre de places : " + reservation.getNombrePlaces() + "\n" +
                            "Prix unitaire : " + String.format("%.2f DH", reservation.getEvenement().getPrixUnitaire()) + "\n" +
                            "Montant total : " + String.format("%.2f DH", reservation.getMontantTotal()) + "\n" +
                            "Statut : " + reservation.getStatut().name() + "\n\n" +

                            "ℹ️ Informations importantes :\n" +
                            "• Présentez ce code à l'entrée de l'événement.\n" +
                            "• Vous pouvez annuler votre réservation jusqu'à 48h avant l'événement.\n" +
                            "• Conservez cet email comme justificatif de paiement.\n\n" +

                            "Pour gérer vos réservations, connectez-vous à votre compte.\n\n" +
                            "Cordialement,\n" +
                            "L'équipe Reservation Event"
            );

            mailSender.send(message);
            System.out.println("✅ Reservation confirmation email sent successfully to: "
                    + reservation.getUtilisateur().getEmail());
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to send reservation confirmation email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendReservationCancellation(Reservation reservation) {
        return sendReservationCancellation(reservation, null);
    }

    /**
     * Envoie un email d'annulation de réservation avec raison
     */
    public boolean sendReservationCancellation(Reservation reservation, String reason) {
        try {
            System.out.println("📧 Tentative d'envoi d'email d'annulation pour la réservation: "
                    + reservation.getCodeReservation());

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("reservation.event.app@gmail.com");
            message.setTo(reservation.getUtilisateur().getEmail());
            message.setSubject("Confirmation d'annulation - Réservation #" + reservation.getCodeReservation());

            // Formatage des dates
            DateTimeFormatter frenchDateTimeFormatter = DateTimeFormatter.ofPattern(
                    "EEEE d MMMM yyyy 'à' HH'h'mm",
                    new Locale("fr", "FR")
            );
            DateTimeFormatter simpleDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            String eventName = reservation.getEvenement().getTitre();
            String eventDate = reservation.getEvenement().getDateDebut().format(frenchDateTimeFormatter);
            String venue = reservation.getEvenement().getLieu() + ", " + reservation.getEvenement().getVille();
            String organizerName = reservation.getEvenement().getOrganisateur().getPrenom() + " "
                    + reservation.getEvenement().getOrganisateur().getNom();
            String cancellationDate = LocalDateTime.now().format(simpleDateFormatter);

            // Texte de la raison
            String reasonText = "";
            if (reason != null && !reason.trim().isEmpty()) {
                reasonText = "\nRaison de l'annulation : " + reason.trim() + "\n";
            }

            // Construction du message
            StringBuilder emailContent = new StringBuilder();
            emailContent.append("Bonjour ").append(reservation.getUtilisateur().getPrenom()).append(",\n\n");
            emailContent.append("Nous vous confirmons l'annulation de votre réservation.\n\n");

            emailContent.append("📋 DÉTAILS DE LA RÉSERVATION ANNULÉE\n");
            emailContent.append("════════════════════════════════════════════\n");
            emailContent.append("🎟️  Code : ").append(reservation.getCodeReservation()).append("\n");
            emailContent.append("📌  Événement : ").append(eventName).append("\n");
            emailContent.append("📅  Date : ").append(eventDate).append("\n");
            emailContent.append("📍  Lieu : ").append(venue).append("\n");
            emailContent.append("👤  Organisateur : ").append(organizerName).append("\n");
            emailContent.append("👥  Places : ").append(reservation.getNombrePlaces()).append("\n");
            emailContent.append("💰  Montant total : ").append(String.format("%.2f DH", reservation.getMontantTotal())).append("\n");
            emailContent.append("🗓️  Date d'annulation : ").append(cancellationDate).append("\n");
            emailContent.append(reasonText);

            emailContent.append("\n💳  INFORMATIONS DE REMBOURSEMENT\n");
            emailContent.append("════════════════════════════════════════════\n");
            emailContent.append("• Montant à rembourser : ").append(String.format("%.2f DH", reservation.getMontantTotal())).append("\n");
            emailContent.append("• Mode de remboursement : Sur le compte bancaire d'origine\n");
            emailContent.append("• Délai de traitement : 5 à 7 jours ouvrables\n");
            emailContent.append("• Référence de transaction : CANCEL-").append(reservation.getCodeReservation()).append("\n");

            emailContent.append("\n❓  QUESTIONS FRÉQUENTES\n");
            emailContent.append("════════════════════════════════════════════\n");
            emailContent.append("• Quand vais-je recevoir mon remboursement ?\n");
            emailContent.append("  Le délai dépend de votre banque, généralement 5-7 jours.\n\n");
            emailContent.append("• Puis-je réserver un autre événement ?\n");
            emailContent.append("  Oui ! Consultez nos événements disponibles sur la plateforme.\n\n");

            emailContent.append("\n📞  ASSISTANCE\n");
            emailContent.append("════════════════════════════════════════════\n");
            emailContent.append("Pour toute question, contactez-nous à :\n");
            emailContent.append("• Email : reservation.event.app@gmail.com\n");
            emailContent.append("• Horaires : Lundi-Vendredi 9h-18h\n");

            emailContent.append("\nNous sommes désolés de vous voir partir et espérons vous revoir bientôt !\n\n");
            emailContent.append("Cordialement,\n");
            emailContent.append("L'équipe Reservation Event\n");
            emailContent.append("🎭 Votre plateforme de réservation d'événements culturels");

            message.setText(emailContent.toString());

            mailSender.send(message);

            System.out.println("✅ Email d'annulation envoyé avec succès pour: " + reservation.getCodeReservation());
            System.out.println("   Destinataire: " + reservation.getUtilisateur().getEmail());
            System.out.println("   Montant remboursé: " + String.format("%.2f DH", reservation.getMontantTotal()));

            return true;

        } catch (Exception e) {
            System.err.println("❌ Échec d'envoi de l'email d'annulation: " + e.getMessage());
            System.err.println("   Réservation: " + reservation.getCodeReservation());
            System.err.println("   Utilisateur: " + reservation.getUtilisateur().getEmail());
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Envoie un rappel 24h avant l'événement
     */
    public boolean sendEventReminder(Reservation reservation) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("reservation.event.app@gmail.com");
            message.setTo(reservation.getUtilisateur().getEmail());
            message.setSubject("Rappel : Votre événement est demain ! - Reservation Event");

            String eventName = reservation.getEvenement().getTitre();
            String eventDate = reservation.getEvenement().getDateDebut().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String venue = reservation.getEvenement().getLieu() + ", " + reservation.getEvenement().getVille();
            String organizer = reservation.getEvenement().getOrganisateur().getNom() + " "
                    + reservation.getEvenement().getOrganisateur().getPrenom();

            message.setText(
                    "Bonjour " + reservation.getUtilisateur().getPrenom() + ",\n\n" +
                            "Rappel : Votre événement est prévu pour DEMAIN !\n\n" +
                            "📌 Événement : " + eventName + "\n" +
                            "📅 Date et heure : " + eventDate + "\n" +
                            "📍 Lieu : " + venue + "\n" +
                            "🎟️ Code de réservation : " + reservation.getCodeReservation() + "\n" +
                            "👥 Nombre de places : " + reservation.getNombrePlaces() + "\n" +
                            "👤 Organisateur : " + organizer + "\n\n" +

                            "📋 Informations pratiques :\n" +
                            "• Présentez-vous 30 minutes avant le début de l'événement.\n" +
                            "• Ayez votre code de réservation (ci-dessus) prêt à présenter.\n" +
                            "• En cas de retard, votre place pourra être attribuée à d'autres personnes.\n\n" +

                            "Bon événement !\n\n" +
                            "Cordialement,\n" +
                            "L'équipe Reservation Event"
            );

            mailSender.send(message);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to send reminder email: " + e.getMessage());
            return false;
        }
    }
}