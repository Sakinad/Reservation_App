package org.example.reservation_event.ServicesImplimentation;
import org.example.reservation_event.Enums.ReservationStatut;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.Enums.UserRole;
import org.example.reservation_event.repositories.UserRepository;
import org.example.reservation_event.repositories.EventRepository;
import org.example.reservation_event.repositories.ReservationRepository;

import org.example.reservation_event.services.UserService;
import org.example.reservation_event.Exceptions.ResourceNotFoundException;
import org.example.reservation_event.Exceptions.ConflictException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    public UserServiceImpl(UserRepository userRepository,
                           EventRepository eventRepository,
                           ReservationRepository reservationRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ------------------ INSCRIPTION ----------------------
    @Override
    @Transactional
    public User register(User user, String rawPassword) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ConflictException("Email déjà utilisé");
        }

        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setActif(true);
        user.setDateInscription(java.time.LocalDateTime.now());

        return userRepository.save(user);
    }

    // ------------------ AUTHENTIFICATION ------------------
    @Override
    public String authenticateAndGetUserType(String email, String rawPassword) {
        // Rechercher l'utilisateur par email
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Vérifier le mot de passe
            if (passwordEncoder.matches(rawPassword, user.getPassword())) {
                // Retourner le rôle de l'utilisateur
                return user.getRole().name(); // Convertir l'enum en String
            }
        }

        return null; // Authentification échouée
    }

    // Votre méthode authenticate existante peut rester ou utiliser la nouvelle
    @Override
    public boolean authenticate(String email, String rawPassword) {
        return authenticateAndGetUserType(email, rawPassword) != null;
    }

    @Override
    public User getUser(String email, String rawPassword) {

        // 1️⃣ Try to get user from database
        Optional<User> optionalUser = userRepository.findByEmail(email);

        // User not found
        if (optionalUser.isEmpty()) {
            return null;
        }

        User user = optionalUser.get();  // extract User

        // 2️⃣ Compare raw password with hashed password
        boolean match = passwordEncoder.matches(rawPassword, user.getPassword());

        // 3️⃣ Return user if match, otherwise null
        return match ? user : null;
    }


    // ------------------ MISE À JOUR PROFIL ----------------
    @Override
    @Transactional
    public User updateProfile(Long userId, User updated) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        u.setNom(updated.getNom());
        u.setPrenom(updated.getPrenom());
        u.setTelephone(updated.getTelephone());

        // ADD THIS LINE TO UPDATE EMAIL
        if (updated.getEmail() != null && !updated.getEmail().equals(u.getEmail())) {
            // Optional: Check if email is already used by another user
            boolean emailExists = userRepository.existsByEmail(updated.getEmail());
            if (emailExists) {
                throw new ConflictException("Cet email est déjà utilisé par un autre compte");
            }
            u.setEmail(updated.getEmail());
        }

        return userRepository.save(u);
    }

    // ------------------ CHANGER PASSWORD ------------------
    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(oldPassword, u.getPassword())) {
            throw new ConflictException("Ancien mot de passe incorrect");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
    }

    // ------------------ ACTIVER / DÉSACTIVER ----------------------
    @Override
    @Transactional
    public void setActive(Long userId, boolean actif) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        u.setActif(actif);
        userRepository.save(u);
    }

    // ------------------ LISTE AVEC FILTRES ------------------------
    @Override
    public List<User> listUsers(UserRole role, Boolean actif, String search) {
        List<User> list = userRepository.findAll();

        // Filtre rôle
        if (role != null) {
            list = list.stream()
                    .filter(u -> u.getRole() == role)
                    .toList();
        }

        // Filtre actif
        if (actif != null) {
            list = list.stream()
                    .filter(u -> u.getActif() == actif)
                    .toList();
        }

        // Filtre recherche (nom, prénom, email)
        if (search != null && !search.isEmpty()) {
            String s = search.toLowerCase();
            list = list.stream()
                    .filter(u -> u.getNom().toLowerCase().contains(s)
                            || u.getPrenom().toLowerCase().contains(s)
                            || u.getEmail().toLowerCase().contains(s))
                    .toList();
        }

        return list;
    }
    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec email: " + email));
    }


    // ------------------ STATISTIQUES PAR UTILISATEUR ----------------
    @Override
    public Map<String, Object> getUserStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Map<String, Object> stats = new HashMap<>();

        // Nombre d'événements créés par l'utilisateur
        long eventsCreated = eventRepository.findAll().stream()
                .filter(e -> e.getOrganisateur().getId().equals(userId))
                .count();

        // Nombre de réservations de l'utilisateur
        long reservations = reservationRepository.findByUtilisateur(user).size();

        // Montant total dépensé
        double totalSpent = reservationRepository.findByUtilisateur(user).stream()
                .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                .mapToDouble(r -> r.getMontantTotal())
                .sum();

        stats.put("eventsCreated", eventsCreated);
        stats.put("reservations", reservations);
        stats.put("totalSpent", totalSpent);

        return stats;
    }
    @Override
    public User findByEmailOrNull(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    @Override
    @Transactional
    public void changeUserRole(Long userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        user.setRole(newRole);
        userRepository.save(user);
    }

}