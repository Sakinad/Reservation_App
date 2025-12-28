package org.example.reservation_event.repositories;

import org.example.reservation_event.classes.User;
import org.example.reservation_event.Enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // Trouver utilisateurs actifs par rôle
    List<User> findByRoleAndActifTrue(UserRole role);

    // Rechercher par nom ou prénom (insensible à la casse)
    List<User> findByNomIgnoreCaseContainingOrPrenomIgnoreCaseContaining(String nom, String prenom);

    // Compter par rôle
    long countByRole(UserRole role);

    // Exemple avec @Query si besoin
    @Query("select u from User u where lower(u.email) like lower(concat('%', :term, '%'))")
    List<User> searchByEmailLike(String term);
}
