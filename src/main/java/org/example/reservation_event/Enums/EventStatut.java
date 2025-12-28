package org.example.reservation_event.Enums;

/**
 * Enumération des statuts d'événement
 */
public enum EventStatut {
    BROUILLON("Brouillon", "#9E9E9E", false, "📝"),
    PUBLIE("Publié", "#4CAF50", true, "✓"),
    ANNULE("Annulé", "#F44336", false, "✗"),
    TERMINE("Terminé", "#607D8B", false, "🏁");

    private final String label;
    private final String color;
    private final boolean visible;
    private final String icon;

    EventStatut(String label, String color, boolean visible, String icon) {
        this.label = label;
        this.color = color;
        this.visible = visible;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getIcon() {
        return icon;
    }

    /**
     * Retourne le label avec l'icône
     */
    public String getLabelWithIcon() {
        return icon + " " + label;
    }

    /**
     * Vérifie si l'événement peut être modifié
     */
    public boolean isModifiable() {
        return this == BROUILLON || this == PUBLIE;
    }

    /**
     * Vérifie si l'événement peut être publié
     */
    public boolean canBePublished() {
        return this == BROUILLON;
    }

    /**
     * Vérifie si l'événement peut être annulé
     */
    public boolean canBeCancelled() {
        return this == BROUILLON || this == PUBLIE;
    }

    /**
     * Vérifie si l'événement peut être supprimé
     */
    public boolean canBeDeleted() {
        return this == BROUILLON;
    }

    /**
     * Vérifie si l'événement accepte des réservations
     */
    public boolean acceptsReservations() {
        return this == PUBLIE;
    }

    /**
     * Retourne le statut à partir d'une chaîne
     */
    public static EventStatut fromString(String statut) {
        for (EventStatut s : EventStatut.values()) {
            if (s.name().equalsIgnoreCase(statut)) {
                return s;
            }
        }
        return BROUILLON; // Statut par défaut
    }
}