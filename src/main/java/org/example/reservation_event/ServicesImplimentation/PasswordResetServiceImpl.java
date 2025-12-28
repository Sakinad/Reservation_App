package org.example.reservation_event.ServicesImplimentation;

import org.example.reservation_event.classes.PasswordResetToken;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.email.EmailService;
import org.example.reservation_event.services.PasswordResetService;
import org.example.reservation_event.repositories.PasswordResetTokenRepository;
import org.example.reservation_event.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PasswordResetServiceImpl implements PasswordResetService{

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PasswordResetServiceImpl(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Crée une demande de réinitialisation de mot de passe et envoie un email
     */
    @Transactional
    public boolean createPasswordResetRequest(String email) {
        try {
            System.out.println("🔑 Creating password reset request for: " + email);

            // Vérifier si l'utilisateur existe
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null || !user.getActif()) {
                // Pour des raisons de sécurité, on retourne true même si l'utilisateur n'existe pas
                System.out.println("⚠️ User not found or inactive, but returning true for security");
                return true;
            }

            // Supprimer les anciens tokens pour cet email
            passwordResetTokenRepository.deleteByEmail(email);

            // Générer un code à 6 chiffres
            String code = generateVerificationCode();
            System.out.println("🔢 Generated code: " + code);

            // Créer le token
            PasswordResetToken token = new PasswordResetToken();
            token.setEmail(email);
            token.setToken(code);
            token.setExpiryDate(LocalDateTime.now().plusMinutes(10)); // Expire dans 10 minutes
            token.setUsed(false);

            // Sauvegarder le token
            passwordResetTokenRepository.save(token);
            System.out.println("💾 Token saved to database");

            // Envoyer l'email
            boolean emailSent = emailService.sendPasswordResetEmail(email, code);

            if (emailSent) {
                System.out.println("✅ Password reset request created successfully");
            } else {
                System.err.println("⚠️ Failed to send password reset email");
            }

            return emailSent;

        } catch (Exception e) {
            System.err.println("❌ Error creating password reset request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Réinitialise le mot de passe avec le code de vérification
     */
    @Transactional
    public boolean resetPassword(String email, String code, String newPassword) {
        try {
            System.out.println("🔄 Attempting to reset password for: " + email);

            // Vérifier si l'utilisateur existe
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                System.err.println("❌ User not found: " + email);
                return false;
            }

            // Trouver le token
            PasswordResetToken token = passwordResetTokenRepository
                    .findByEmailAndToken(email, code)
                    .orElse(null);

            if (token == null) {
                System.err.println("❌ Token not found for email: " + email + " and code: " + code);
                return false;
            }

            // Vérifier si le token a déjà été utilisé
            if (token.isUsed()) {
                System.err.println("❌ Token already used: " + code);
                return false;
            }

            // Vérifier si le token a expiré
            if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
                System.err.println("❌ Token expired: " + code);
                // Supprimer le token expiré
                passwordResetTokenRepository.delete(token);
                return false;
            }

            // Mettre à jour le mot de passe
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            System.out.println("🔐 Password updated in database");

            // Marquer le token comme utilisé
            token.setUsed(true);
            passwordResetTokenRepository.save(token);
            System.out.println("✔️ Token marked as used");

            System.out.println("✅ Password reset successful for: " + email);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error resetting password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Vérifie si un code de réinitialisation est valide
     */
    public boolean isValidResetCode(String email, String code) {
        try {
            PasswordResetToken token = passwordResetTokenRepository
                    .findByEmailAndToken(email, code)
                    .orElse(null);

            if (token == null) {
                return false;
            }

            return !token.isUsed() && token.getExpiryDate().isAfter(LocalDateTime.now());

        } catch (Exception e) {
            System.err.println("❌ Error validating reset code: " + e.getMessage());
            return false;
        }
    }

    /**
     * Nettoie les tokens expirés (à exécuter périodiquement)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            passwordResetTokenRepository.deleteByExpiryDateBefore(now);
            System.out.println("✅ Expired password reset tokens cleaned up");
        } catch (Exception e) {
            System.err.println("❌ Error cleaning up expired tokens: " + e.getMessage());
        }
    }

    /**
     * Génère un code de vérification à 6 chiffres
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Génère un nombre entre 100000 et 999999
        return String.valueOf(code);
    }
}