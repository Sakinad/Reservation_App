package org.example.reservation_event.Enums;

/**
 * Enumération des statuts de réservation
 */
public enum ReservationStatut {
    EN_ATTENTE("En attente", "#FF9800", "⏳", false),
    CONFIRMEE("Confirmée", "#4CAF50", "✓", true),
    ANNULEE("Annulée", "#F44336", "✗", false);

    private final String label;
    private final String color;
    private final String icon;
    private final boolean countAsReserved;

    ReservationStatut(String label, String color, String icon, boolean countAsReserved) {
        this.label = label;
        this.color = color;
        this.icon = icon;
        this.countAsReserved = countAsReserved;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isCountAsReserved() {
        return countAsReserved;
    }

    /**
     * Retourne le label avec l'icône
     */
    public String getLabelWithIcon() {
        return icon + " " + label;
    }

    /**
     * Vérifie si la réservation est active
     */
    public boolean isActive() {
        return this == EN_ATTENTE || this == CONFIRMEE;
    }

    /**
     * Vérifie si la réservation peut être confirmée
     */
    public boolean canBeConfirmed() {
        return this == EN_ATTENTE;
    }

    /**
     * Vérifie si la réservation peut être annulée
     */
    public boolean canBeCancelled() {
        return this == EN_ATTENTE || this == CONFIRMEE;
    }

    /**
     * Vérifie si la réservation est finalisée (confirmée ou annulée)
     */
    public boolean isFinalized() {
        return this == CONFIRMEE || this == ANNULEE;
    }

    /**
     * Vérifie si la réservation compte dans les places réservées
     */
    public boolean countsInCapacity() {
        return this == EN_ATTENTE || this == CONFIRMEE;
    }

    /**
     * Retourne le statut à partir d'une chaîne
     */
    public static ReservationStatut fromString(String statut) {
        for (ReservationStatut s : ReservationStatut.values()) {
            if (s.name().equalsIgnoreCase(statut)) {
                return s;
            }
        }
        return EN_ATTENTE; // Statut par défaut
    }

    /**
     * Retourne le prochain statut possible
     */
    public ReservationStatut getNextStatut() {
        return switch (this) {
            case EN_ATTENTE -> CONFIRMEE;
            case CONFIRMEE -> ANNULEE;
            case ANNULEE -> null; // Statut final
        };
    }
}