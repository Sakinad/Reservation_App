package org.example.reservation_event.services;


import org.example.reservation_event.classes.User;
import org.example.reservation_event.Enums.UserRole;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Map;
@Service
public interface UserService {
    void changeUserRole(Long userId, UserRole newRole);
    // Inscription
    User register(User user, String rawPassword);
    User findByEmail(String email);
    User getUser(String email, String rawPassword);
    // Authentification
    boolean authenticate(String email, String rawPassword);
    String authenticateAndGetUserType(String email, String rawPassword);
    // Mise à jour profil
    User updateProfile(Long userId, User updated);
    // Changer mot de passe
    void changePassword(Long userId, String oldPassword, String newPassword);
    // Activer / désactiver
    void setActive(Long userId, boolean actif);
    // Liste avec filtres
    List<User> listUsers(UserRole role, Boolean actif, String search);
    // Statistiques utilisateur
    Map<String, Object> getUserStats(Long userId);
    User findByEmailOrNull(String email);
    User getUserById(Long id);

}