package org.example.reservation_event.repositories;

import org.example.reservation_event.classes.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Trouve un token par email et code
     */
    Optional<PasswordResetToken> findByEmailAndToken(String email, String token);

    /**
     * Supprime tous les tokens pour un email donné
     */
    void deleteByEmail(String email);

    /**
     * Supprime tous les tokens expirés avant une date donnée
     */
    void deleteByExpiryDateBefore(LocalDateTime dateTime);

    /**
     * Trouve tous les tokens non utilisés pour un email
     */
    Optional<PasswordResetToken> findByEmailAndUsedFalse(String email);
}