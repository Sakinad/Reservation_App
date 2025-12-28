package org.example.reservation_event.ui.Organizateurs;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import org.example.reservation_event.Enums.EventCategorie;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.services.EventService;
import org.example.reservation_event.services.ReservationService;
import org.example.reservation_event.services.UserService;
import org.example.reservation_event.ui.MainLayout;
import org.example.reservation_event.utils.ImageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Route(value = "organizer/eventDetail/:eventId", layout = MainLayout.class)
@PageTitle("Détails de l'Événement - Organisateur")
public class OrganizerEventDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final EventService eventService;
    private final ReservationService reservationService;
    private final UserService userService;

    private User currentUser;
    private Event event;
    private Long eventId;

    @Autowired
    public OrganizerEventDetailView(EventService eventService, ReservationService reservationService,
                                    UserService userService) {
        this.eventService = eventService;
        this.reservationService = reservationService;
        this.userService = userService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        currentUser = getCurrentUser();
        if (currentUser == null || !isOrganizer(currentUser)) {
            showAccessDenied();
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        try {
            String eventIdParam = beforeEnterEvent.getRouteParameters().get("eventId").orElse(null);
            if (eventIdParam != null) {
                this.eventId = Long.parseLong(eventIdParam);
                this.event = eventService.getEventById(eventId);

                if (event == null) {
                    showEventNotFound();
                    return;
                }

                if (!event.getOrganisateur().getId().equals(currentUser.getId())) {
                    showAccessDenied();
                    return;
                }

                loadEventDetails();
            } else {
                showEventNotFound();
            }
        } catch (NumberFormatException e) {
            showEventNotFound();
        } catch (Exception e) {
            Notification.show("Erreur lors du chargement de l'événement: " + e.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void loadEventDetails() {
        removeAll();

        VerticalLayout contentArea = new VerticalLayout();
        contentArea.setSizeFull();
        contentArea.setPadding(true);
        contentArea.setSpacing(true);
        contentArea.getStyle()
                .set("max-width", "1400px")
                .set("margin", "0 auto");

        contentArea.add(
                createHeader(),
                createEventDetailSection()
        );

        add(contentArea);
    }

    private Component createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-bottom", "var(--lumo-space-m)");

        Button backButton = new Button("Retour", new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("organizer/events")));

        H1 title = new H1("Détails de l'Événement");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        HorizontalLayout actionButtons = new HorizontalLayout();
        actionButtons.setSpacing(true);

        Button reservationsButton = new Button("Réservations", new Icon(VaadinIcon.LIST));
        reservationsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        reservationsButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/event-reservations/" + event.getId()))
        );

        Button editButton = new Button("Modifier", new Icon(VaadinIcon.EDIT));
        editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/event/edit/" + event.getId()))
        );

        actionButtons.add(reservationsButton, editButton);

        header.add(backButton, title, actionButtons);
        header.setFlexGrow(0, backButton);
        header.setFlexGrow(1, title);
        header.setFlexGrow(0, actionButtons);

        return header;
    }


    private Component createEventDetailSection() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setWidthFull();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Première ligne : Event card et les 3 info cards côte à côte
        HorizontalLayout topRow = new HorizontalLayout();
        topRow.setWidthFull();
        topRow.setSpacing(true);
        topRow.setAlignItems(FlexComponent.Alignment.STRETCH);

        // Colonne gauche : Carte de l'événement (60%)
        VerticalLayout eventColumn = new VerticalLayout();
        eventColumn.setWidth("60%");
        eventColumn.setPadding(false);
        eventColumn.setSpacing(false);
        eventColumn.add(createEventCard());

        // Colonne droite : Les 3 info cards empilées (40%)
        VerticalLayout infoCardsColumn = new VerticalLayout();
        infoCardsColumn.setWidth("40%");
        infoCardsColumn.setPadding(false);
        infoCardsColumn.setSpacing(true);
        infoCardsColumn.add(
                createOrganizerCard(),
                createCapacityCard(),
                createActionsCard()
        );

        topRow.add(eventColumn, infoCardsColumn);

        // Deuxième ligne : Carte Google Maps (100% de la largeur)
        VerticalLayout mapRow = new VerticalLayout();
        mapRow.setWidthFull();
        mapRow.setPadding(false);
        mapRow.setSpacing(false);
        mapRow.add(createMapCard());

        mainLayout.add(topRow, mapRow);
        return mainLayout;
    }
    private Div createEventCard() {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("overflow", "hidden");

        Div imageSection = new Div();
        imageSection.setWidthFull();
        imageSection.setHeight("300px");
        imageSection.getStyle()
                .set("position", "relative")
                .set("overflow", "hidden");

        boolean hasImage = false;
        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            try {
                // Utiliser ImageUtils pour créer l'image
                Image image = ImageUtils.createEventImage(event.getImageUrl());
                image.setWidthFull();
                image.setHeight("100%");
                image.getStyle()
                        .set("object-fit", "cover")
                        .set("position", "absolute")
                        .set("top", "0")
                        .set("left", "0");
                imageSection.add(image);
                hasImage = true;
            } catch (Exception e) {
                System.err.println("Error loading image for event " + event.getId() + ": " + e.getMessage());
            }
        }

        if (!hasImage) {
            imageSection.getStyle()
                    .set("background", "linear-gradient(135deg, var(--lumo-primary-color) 0%, var(--lumo-primary-color-50pct) 100%)")
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("justify-content", "center");

            Icon categoryIcon = getCategoryIcon(event.getCategorie());
            categoryIcon.setSize("64px");
            categoryIcon.setColor("rgba(255, 255, 255, 0.9)");
            categoryIcon.getStyle()
                    .set("filter", "drop-shadow(0 2px 4px rgba(0,0,0,0.3))");
            imageSection.add(categoryIcon);
        }

        Div statusOverlay = new Div();
        statusOverlay.getStyle()
                .set("position", "absolute")
                .set("top", "20px")
                .set("right", "20px")
                .set("z-index", "10");

        Span statusBadge = new Span(event.getStatut().getLabel());
        statusBadge.getStyle()
                .set("background", getStatusColor(event.getStatut()))
                .set("color", "white")
                .set("padding", "8px 16px")
                .set("border-radius", "20px")
                .set("font-size", "14px")
                .set("font-weight", "700")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.3)");

        statusOverlay.add(statusBadge);
        imageSection.add(statusOverlay);

        VerticalLayout contentSection = new VerticalLayout();
        contentSection.setWidthFull();
        contentSection.setPadding(true);
        contentSection.setSpacing(true);

        H2 title = new H2(event.getTitre());
        title.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "700")
                .set("font-size", "24px")
                .set("margin", "0");

        Span categoryBadge = new Span(event.getCategorie().getLabel());
        categoryBadge.getStyle()
                .set("background", "var(--lumo-primary-color-10pct)")
                .set("color", "var(--lumo-primary-color)")
                .set("padding", "6px 12px")
                .set("border-radius", "16px")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("display", "inline-block")
                .set("margin-bottom", "10px");

        Div descriptionSection = new Div();
        descriptionSection.getStyle()
                .set("margin-top", "20px")
                .set("margin-bottom", "20px");

        H4 descriptionTitle = new H4("Description");
        descriptionTitle.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("margin", "0 0 10px 0")
                .set("font-size", "18px");

        Span descriptionText = new Span(event.getDescription() != null ? event.getDescription() : "Aucune description");
        descriptionText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("line-height", "1.6");

        descriptionSection.add(descriptionTitle, descriptionText);

        Div detailsGrid = new Div();
        detailsGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(2, 1fr)")
                .set("gap", "20px")
                .set("margin-top", "20px");

        detailsGrid.add(
                createDetailCard("Date de début", formatDateTime(event.getDateDebut())),
                createDetailCard("Date de fin", event.getDateFin() != null ? formatDateTime(event.getDateFin()) : "Non spécifiée"),
                createDetailCard("Prix par place", String.format("%.2f DH", event.getPrixUnitaire())),
                createDetailCard("Lieu", event.getLieu() + ", " + event.getVille())
        );

        contentSection.add(title, categoryBadge, descriptionSection, detailsGrid);
        card.add(imageSection, contentSection);

        return card;
    }
    private Div createMapCard() {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("overflow", "hidden");

        // Header avec titre et bouton
        Div header = new Div();
        header.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("padding", "20px 20px 15px 20px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        Div titleSection = new Div();
        titleSection.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "10px");

        Icon mapIcon = new Icon(VaadinIcon.MAP_MARKER);
        mapIcon.setColor("var(--lumo-primary-color)");
        mapIcon.setSize("20px");

        H4 title = new H4("Localisation de l'événement");
        title.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("margin", "0")
                .set("font-size", "18px");

        titleSection.add(mapIcon, title);

        // Bouton ouvrir dans Google Maps
        Button openMapsBtn = new Button("Ouvrir dans Google Maps", new Icon(VaadinIcon.EXTERNAL_LINK));
        openMapsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        openMapsBtn.addClickListener(e -> openInGoogleMaps());

        header.add(titleSection, openMapsBtn);

        // Adresse
        Div addressSection = new Div();
        addressSection.getStyle()
                .set("padding", "15px 20px")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        Div addressRow = new Div();
        addressRow.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "8px");

        Icon locationIcon = new Icon(VaadinIcon.LOCATION_ARROW);
        locationIcon.setColor("var(--lumo-secondary-text-color)");
        locationIcon.setSize("16px");

        Span addressText = new Span(event.getLieu() + ", " + event.getVille());
        addressText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "14px");

        addressRow.add(locationIcon, addressText);
        addressSection.add(addressRow);

        // Carte Google Maps
        String address = event.getLieu() + ", " + event.getVille();
        String encodedAddress = address.replace(" ", "+");
        String mapUrl = "https://www.google.com/maps?q=" + encodedAddress + "&output=embed&zoom=15";

        Div mapContainer = new Div();
        mapContainer.setHeight("500px"); // Hauteur augmentée pour une carte pleine largeur
        mapContainer.getStyle()
                .set("position", "relative")
                .set("overflow", "hidden");

        IFrame mapFrame = new IFrame(mapUrl);
        mapFrame.setWidth("100%");
        mapFrame.setHeight("100%");
        mapFrame.getStyle()
                .set("border", "0")
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0");

        // Overlay pour améliorer l'interaction
        Div mapOverlay = new Div();
        mapOverlay.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0")
                .set("width", "100%")
                .set("height", "100%")
                .set("z-index", "1")
                .set("cursor", "pointer")
                .set("background", "transparent");

        mapOverlay.addClickListener(e -> openInGoogleMaps());

        mapContainer.add(mapFrame, mapOverlay);

        // Instructions
        Div instructionSection = new Div();
        instructionSection.getStyle()
                .set("padding", "12px 20px")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-top", "1px solid var(--lumo-contrast-10pct)")
                .set("font-size", "12px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center");

        Span instruction = new Span("Cliquez sur la carte pour l'ouvrir dans Google Maps");
        instructionSection.add(instruction);

        card.add(header, addressSection, mapContainer, instructionSection);
        return card;
    }

    private void openInGoogleMaps() {
        String address = event.getLieu() + ", " + event.getVille();
        String encodedAddress = address.replace(" ", "+");
        String url = "https://www.google.com/maps/search/?api=1&query=" + encodedAddress;
        getUI().ifPresent(ui -> ui.getPage().open(url, "_blank"));
    }
    private VerticalLayout createDetailCard(String title, String value) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "1px solid var(--lumo-contrast-10pct)");

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "16px")
                .set("font-weight", "600")
                .set("margin-top", "5px");

        card.add(titleSpan, valueSpan);
        return card;
    }

    private Div createOrganizerCard() {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("padding", "20px");

        H4 title = new H4("Organisateur");
        title.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("margin", "0 0 20px 0")
                .set("font-size", "18px")
                .set("border-bottom", "2px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "10px");

        VerticalLayout organizerInfo = new VerticalLayout();
        organizerInfo.setPadding(false);
        organizerInfo.setSpacing(true);

        Div nameRow = new Div();
        nameRow.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("margin-bottom", "10px");

        Icon userIcon = new Icon(VaadinIcon.USER);
        userIcon.setColor("var(--lumo-primary-color)");
        userIcon.getStyle().set("margin-right", "10px");

        Span nameText = new Span(event.getOrganisateur().getNom() + " " + event.getOrganisateur().getPrenom());
        nameText.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "600")
                .set("font-size", "16px");

        nameRow.add(userIcon, nameText);

        Div emailRow = new Div();
        emailRow.getStyle()
                .set("display", "flex")
                .set("align-items", "center");

        Icon emailIcon = new Icon(VaadinIcon.ENVELOPE);
        emailIcon.setColor("var(--lumo-secondary-text-color)");
        emailIcon.getStyle().set("margin-right", "10px");

        Span emailText = new Span(event.getOrganisateur().getEmail());
        emailText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "14px");

        emailRow.add(emailIcon, emailText);

        organizerInfo.add(nameRow, emailRow);
        card.add(title, organizerInfo);

        return card;
    }

    private Div createCapacityCard() {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("padding", "20px");

        H4 title = new H4("Capacité");
        title.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("margin", "0 0 20px 0")
                .set("font-size", "18px")
                .set("border-bottom", "2px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "10px");

        int reservedPlaces = reservationService.getTotalReservedPlaces(event.getId());
        int totalPlaces = event.getCapaciteMax();
        int availablePlaces = totalPlaces - reservedPlaces;
        double fillRate = totalPlaces > 0 ? (double) reservedPlaces / totalPlaces * 100 : 0;

        VerticalLayout statsLayout = new VerticalLayout();
        statsLayout.setPadding(false);
        statsLayout.setSpacing(true);

        statsLayout.add(
                createStatRow("Capacité totale:", totalPlaces + " places", "var(--lumo-success-color)"),
                createStatRow("Places réservées:", reservedPlaces + " places", "var(--lumo-primary-color)"),
                createStatRow("Places disponibles:", availablePlaces + " places", "var(--lumo-contrast-60pct)")
        );

        Div rateRow = new Div();
        rateRow.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("margin-top", "15px");

        Span rateLabel = new Span("Taux:");
        rateLabel.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "600");

        Span rateValue = new Span(String.format("%.1f%%", fillRate));
        rateValue.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "700")
                .set("font-size", "18px");

        rateRow.add(rateLabel, rateValue);
        statsLayout.add(rateRow);

        card.add(title, statsLayout);
        return card;
    }

    private Div createStatRow(String label, String value, String color) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "600");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("color", color)
                .set("font-weight", "700");

        row.add(labelSpan, valueSpan);
        return row;
    }

    private Div createActionsCard() {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("padding", "20px");

        H4 title = new H4("Actions");
        title.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("margin", "0 0 20px 0")
                .set("font-size", "18px")
                .set("border-bottom", "2px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "10px");

        VerticalLayout actionsLayout = new VerticalLayout();
        actionsLayout.setPadding(false);
        actionsLayout.setSpacing(true);

        if (event.getStatut() == EventStatut.BROUILLON) {
            Button publishButton = new Button("Publier l'événement", new Icon(VaadinIcon.GLOBE));
            publishButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            publishButton.setWidthFull();
            publishButton.addClickListener(e -> confirmPublishEvent());
            actionsLayout.add(publishButton);
        }

        Button reservationsButton = new Button("Voir les réservations", new Icon(VaadinIcon.LIST));
        reservationsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        reservationsButton.setWidthFull();
        reservationsButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/event-reservations/" + event.getId()))
        );
        actionsLayout.add(reservationsButton);

        Button editButton = new Button("Modifier l'événement", new Icon(VaadinIcon.EDIT));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editButton.setWidthFull();
        editButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/event/edit/" + event.getId()))
        );
        actionsLayout.add(editButton);

        if (event.getStatut() == EventStatut.BROUILLON || event.getStatut() == EventStatut.ANNULE ||
                event.getStatut() == EventStatut.TERMINE) {
            Button deleteButton = new Button("Supprimer l'événement", new Icon(VaadinIcon.TRASH));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            deleteButton.setWidthFull();
            deleteButton.addClickListener(e -> confirmDeleteEvent());
            actionsLayout.add(deleteButton);
        } else if (event.getStatut() == EventStatut.PUBLIE) {
            Button cancelButton = new Button("Annuler l'événement", new Icon(VaadinIcon.BAN));
            cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            cancelButton.setWidthFull();
            cancelButton.addClickListener(e -> confirmCancelEvent());
            actionsLayout.add(cancelButton);
        }

        card.add(title, actionsLayout);
        return card;
    }

    private Icon getCategoryIcon(EventCategorie categorie) {
        VaadinIcon iconType = switch (categorie) {
            case CONCERT -> VaadinIcon.MUSIC;
            case CONFERENCE -> VaadinIcon.COMMENTS;
            case THEATRE -> VaadinIcon.GROUP;
            case SPORT -> VaadinIcon.TROPHY;
            case AUTRE -> VaadinIcon.CALENDAR;
        };
        return new Icon(iconType);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH'h'mm",
                new Locale("fr", "FR"));
        return dateTime.format(formatter);
    }

    private String getStatusColor(EventStatut statut) {
        return switch (statut) {
            case PUBLIE -> "var(--lumo-success-color)";
            case BROUILLON -> "var(--lumo-warning-color)";
            case ANNULE -> "var(--lumo-error-color)";
            case TERMINE -> "var(--lumo-contrast-60pct)";
        };
    }

    private void confirmPublishEvent() {
        Notification.show("Fonctionnalité de publication à implémenter",
                3000, Notification.Position.TOP_CENTER);
    }

    private void confirmDeleteEvent() {
        Notification.show("Fonctionnalité de suppression à implémenter",
                3000, Notification.Position.TOP_CENTER);
    }

    private void confirmCancelEvent() {
        Notification.show("Fonctionnalité d'annulation à implémenter",
                3000, Notification.Position.TOP_CENTER);
    }

    private void showEventNotFound() {
        removeAll();

        VerticalLayout errorLayout = new VerticalLayout();
        errorLayout.setSizeFull();
        errorLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        errorLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        errorLayout.getStyle().set("background", "var(--lumo-contrast-5pct)");

        Icon icon = new Icon(VaadinIcon.EXCLAMATION_CIRCLE);
        icon.setSize("64px");
        icon.setColor("var(--lumo-error-color)");

        H2 title = new H2("Événement non trouvé");
        title.getStyle().set("color", "var(--lumo-error-text-color)");

        Span message = new Span("L'événement que vous cherchez n'existe pas ou a été supprimé.");
        message.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button backBtn = new Button("Retour à mes événements", e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/events")));
        backBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        errorLayout.add(icon, title, message, backBtn);
        add(errorLayout);
    }

    private void showAccessDenied() {
        removeAll();

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.getStyle().set("background", "var(--lumo-contrast-5pct)");

        Icon icon = new Icon(VaadinIcon.BAN);
        icon.setSize("64px");
        icon.setColor("var(--lumo-error-color)");

        H2 title = new H2("Accès refusé");
        title.getStyle().set("color", "var(--lumo-error-text-color)");

        Span message = new Span("Vous devez être organisateur pour accéder à cette page");
        message.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button homeBtn = new Button("Retour à l'accueil", e ->
                getUI().ifPresent(ui -> ui.navigate("dashboard")));
        homeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(icon, title, message, homeBtn);
        add(layout);
    }

    private User getCurrentUser() {
        try {
            User sessionUser = VaadinSession.getCurrent().getAttribute(User.class);
            if (sessionUser != null) {
                return sessionUser;
            }

            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                return userService.findByEmail(username);
            }
        } catch (Exception e) {
            System.out.println("Error getting current user: " + e.getMessage());
        }
        return null;
    }

    private boolean isOrganizer(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String roleName = user.getRole().name();
        return roleName.equals("ORGANIZER") || roleName.equals("ADMIN");
    }
}