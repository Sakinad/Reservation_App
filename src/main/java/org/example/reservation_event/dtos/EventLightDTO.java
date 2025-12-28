package org.example.reservation_event.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.reservation_event.Enums.EventCategorie;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.classes.Event;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventLightDTO {
    private Long id;
    private String titre;
    private EventCategorie categorie;
    private LocalDateTime dateDebut;
    private String ville;
    private Double prixUnitaire;
    private String imageUrl;
    private Integer placesDisponibles;
    private Boolean reservable;
    private EventStatut statut;

    public static EventLightDTO fromEntity(Event event) {
        int placesDispo = event.getPlacesDisponibles();

        return EventLightDTO.builder()
                .id(event.getId())
                .titre(event.getTitre())
                .categorie(event.getCategorie())
                .dateDebut(event.getDateDebut())
                .ville(event.getVille())
                .prixUnitaire(event.getPrixUnitaire())
                .imageUrl(event.getImageUrl())
                .placesDisponibles(placesDispo)
                .statut(event.getStatut())
                .reservable(
                        event.getStatut() == EventStatut.PUBLIE
                                && LocalDateTime.now().isBefore(event.getDateDebut())
                                && placesDispo > 0
                )
                .build();
    }

    @Deprecated
    public static EventLightDTO fromEntity(Event event, int placesDisponibles) {
        return fromEntity(event);
    }
}