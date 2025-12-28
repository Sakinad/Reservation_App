package org.example.reservation_event.Enums;

/**
 * Enumération des rôles utilisateur
 */
public enum UserRole {
    ADMIN("Administrateur", "#FF5722", "Admin"),
    ORGANIZER("Organisateur", "#2196F3", "Org"),
    CLIENT("Client", "#4CAF50", "Client");

    private final String label;
    private final String color;
    private final String shortName;

    UserRole(String label, String color, String shortName) {
        this.label = label;
        this.color = color;
        this.shortName = shortName;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }

    public String getShortName() {
        return shortName;
    }

    /**
     * Vérifie si le rôle a des privilèges d'administration
     */
    public boolean hasAdminPrivileges() {
        return this == ADMIN;
    }

    /**
     * Vérifie si le rôle peut créer des événements
     */
    public boolean canCreateEvents() {
        return this == ADMIN || this == ORGANIZER;
    }

    /**
     * Vérifie si le rôle peut effectuer des réservations
     */
    public boolean canMakeReservations() {
        return true; // Tous les rôles peuvent réserver
    }

    /**
     * Retourne le rôle à partir d'une chaîne
     */
    public static UserRole fromString(String role) {
        for (UserRole r : UserRole.values()) {
            if (r.name().equalsIgnoreCase(role)) {
                return r;
            }
        }
        return CLIENT; // Rôle par défaut
    }
}