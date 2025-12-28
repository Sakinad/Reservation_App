package org.example.reservation_event.Enums;

/**
 * Enumération des catégories d'événements
 */
public enum EventCategorie {
    CONCERT("Concert", "🎵", "#E91E63"),
    THEATRE("Théâtre", "🎭", "#9C27B0"),
    CONFERENCE("Conférence", "🎤", "#3F51B5"),
    SPORT("Sport", "⚽", "#FF9800"),
    AUTRE("Autre", "📅", "#607D8B");

    private final String label;
    private final String icon;
    private final String color;

    EventCategorie(String label, String icon, String color) {
        this.label = label;
        this.icon = icon;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }

    /**
     * Retourne le label avec l'icône
     */
    public String getLabelWithIcon() {
        return icon + " " + label;
    }

    /**
     * Retourne la catégorie à partir d'une chaîne
     */
    public static EventCategorie fromString(String categorie) {
        for (EventCategorie c : EventCategorie.values()) {
            if (c.name().equalsIgnoreCase(categorie) ||
                    c.label.equalsIgnoreCase(categorie)) {
                return c;
            }
        }
        return AUTRE; // Catégorie par défaut
    }

    /**
     * Vérifie si la catégorie est culturelle
     */
    public boolean isCulturel() {
        return this == CONCERT || this == THEATRE || this == CONFERENCE;
    }

    /**
     * Vérifie si la catégorie est sportive
     */
    public boolean isSportif() {
        return this == SPORT;
    }
}