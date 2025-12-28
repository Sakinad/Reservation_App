package org.example.reservation_event.config;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Vérifie si l'utilisateur est authentifié
     * @return true si authentifié, false sinon
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        // Vérifier si authentifié et pas anonyme
        boolean isAuthenticated = authentication.isAuthenticated();
        Object principal = authentication.getPrincipal();

        // Exclure les utilisateurs anonymes
        boolean isAnonymous = principal instanceof String && "anonymousUser".equals(principal);

        return isAuthenticated && !isAnonymous;
    }

    /**
     * Récupère l'utilisateur authentifié
     * @return Optional contenant UserDetails si authentifié
     */
    public static Optional<UserDetails> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        // Vérifier si c'est un utilisateur anonyme
        if (principal == null || "anonymousUser".equals(principal)) {
            return Optional.empty();
        }

        // Retourner le UserDetails si c'est le bon type
        if (principal instanceof UserDetails) {
            return Optional.of((UserDetails) principal);
        }

        return Optional.empty();
    }

    /**
     * Vérifie si l'utilisateur a une autorité spécifique
     * @param authority L'autorité à vérifier (ex: "ADMIN", "ORGANIZER", "CLIENT")
     * @return true si l'utilisateur a cette autorité
     */
    public static boolean hasAuthority(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null &&
                auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(role));
    }

    /**
     * Vérifie si l'utilisateur a l'une des autorités spécifiées
     * @param authorities Les autorités à vérifier
     * @return true si l'utilisateur a au moins une de ces autorités
     */
    public static boolean hasAnyAuthority(String... authorities) {
        if (!isAuthenticated() || authorities == null || authorities.length == 0) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        for (String authority : authorities) {
            if (authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority::equals)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Récupère le rôle de l'utilisateur actuel
     * @return Le rôle (ADMIN, ORGANIZER, CLIENT) ou null
     */
    public static String getCurrentUserRole() {
        if (!isAuthenticated()) {
            return null;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);
    }

    /**
     * Déconnexion de l'utilisateur
     */
    public static void logout() {
        // Nettoyer le contexte Spring Security
        SecurityContextHolder.clearContext();

        // Fermer la session Vaadin
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.close();
        }
    }
}