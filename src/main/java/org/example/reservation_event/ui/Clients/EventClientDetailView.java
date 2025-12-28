package org.example.reservation_event.ui.Clients;

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
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.StreamResource;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.services.EventService;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Route(value = "eventClient", layout = MainLayout.class)
@PageTitle("Détails de l'événement")
public class EventClientDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final EventService eventService;
    private final UserService userService;
    private User currentUser;
    private Event currentEvent;
    private Long eventId;
    private VerticalLayout contentContainer;

    @Autowired
    public EventClientDetailView(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        currentUser = getCurrentUser();
        initializeLayout();
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        if (parameter == null) {
            Notification.show("ID d'événement invalide", 3000, Notification.Position.MIDDLE);
            event.forwardTo("eventsClient");
            return;
        }
        this.eventId = parameter;
        loadEventDetails();
    }

    private void initializeLayout() {
        // Back button
        Button backButton = new Button("Retour aux événements", new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("eventsClient")));
        add(backButton);

        contentContainer = new VerticalLayout();
        contentContainer.setWidthFull();
        contentContainer.setPadding(false);
        contentContainer.setSpacing(true);
        add(contentContainer);
    }

    private void loadEventDetails() {
        try {
            Event event = eventService.getEventById(eventId);

            if (event == null) {
                Notification.show("Événement non trouvé", 3000, Notification.Position.MIDDLE);
                getUI().ifPresent(ui -> ui.navigate("eventsClient"));
                return;
            }

            currentEvent = event;
            displayEventDetails();

        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Erreur lors du chargement de l'événement", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("eventsClient"));
        }
    }

    private void displayEventDetails() {
        contentContainer.removeAll();

        // Main centered container
        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setMaxWidth("1200px");
        mainContainer.setWidthFull();
        mainContainer.setPadding(true);
        mainContainer.setSpacing(true);
        mainContainer.getStyle()
                .set("margin", "0 auto")
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 12px rgba(0, 0, 0, 0.1)");

        mainContainer.add(createHeaderSection());
        mainContainer.add(createMainInfoSection());

        if (currentEvent.getDescription() != null && !currentEvent.getDescription().isEmpty()) {
            mainContainer.add(createDescriptionSection());
        }

        mainContainer.add(createOrganizerSection());

        // Add the map section
        mainContainer.add(createMapSection());

        mainContainer.add(createReservationSection());

        contentContainer.add(mainContainer);
    }

    private Component createHeaderSection() {
        VerticalLayout headerSection = new VerticalLayout();
        headerSection.setWidthFull();
        headerSection.setPadding(false);
        headerSection.setSpacing(true);

        Div imageContainer = new Div();
        imageContainer.setWidthFull();
        imageContainer.setHeight("360px");
        imageContainer.getStyle()
                .set("border-radius", "12px 12px 0 0")
                .set("overflow", "hidden")
                .set("position", "relative")
                .set("background-color", "var(--lumo-contrast-10pct)");

        boolean hasImage = false;

        if (currentEvent.getImageUrl() != null && !currentEvent.getImageUrl().isEmpty()) {
            try {
                // Utiliser ImageUtils pour créer l'image
                Image image = ImageUtils.createEventImage(currentEvent.getImageUrl(), "360px");
                image.setWidthFull();
                image.setHeight("100%");
                image.getStyle()
                        .set("object-fit", "cover")
                        .set("position", "absolute")
                        .set("top", "0")
                        .set("left", "0")
                        .set("border-radius", "12px 12px 0 0");
                imageContainer.add(image);
                hasImage = true;
            } catch (Exception e) {
                System.err.println("Error loading image with ImageUtils: " + e.getMessage());
            }
        }

        if (!hasImage) {
            imageContainer.getStyle()
                    .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("justify-content", "center");

            Icon defaultIcon = new Icon(VaadinIcon.CALENDAR);
            defaultIcon.setSize("120px");
            defaultIcon.setColor("white");
            imageContainer.add(defaultIcon);
        }

        try {
            if (currentEvent.getCategorie() != null) {
                Span categoryBadge = new Span(currentEvent.getCategorie().getLabel());
                categoryBadge.getStyle()
                        .set("position", "absolute")
                        .set("top", "20px")
                        .set("right", "20px")
                        .set("background", currentEvent.getCategorie().getColor())
                        .set("color", "white")
                        .set("padding", "8px 20px")
                        .set("border-radius", "25px")
                        .set("font-weight", "700");
                imageContainer.add(categoryBadge);
            }
        } catch (Exception ignored) {}

        H1 title = new H1(currentEvent.getTitre());
        title.getStyle()
                .set("margin", "16px 0")
                .set("color", "var(--lumo-primary-text-color)");

        headerSection.add(imageContainer, title);
        return headerSection;
    }
    private Component createMapSection() {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("overflow", "hidden")
                .set("margin-top", "20px");

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

        H3 title = new H3("Localisation de l'événement");
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

        Span addressText = new Span(currentEvent.getLieu() + ", " + currentEvent.getVille());
        addressText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "14px");

        addressRow.add(locationIcon, addressText);
        addressSection.add(addressRow);

        // Carte Google Maps
        String address = currentEvent.getLieu() + ", " + currentEvent.getVille();
        String encodedAddress = address.replace(" ", "+");
        String mapUrl = "https://www.google.com/maps?q=" + encodedAddress + "&output=embed&zoom=15";

        Div mapContainer = new Div();
        mapContainer.setHeight("400px");
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
        String address = currentEvent.getLieu() + ", " + currentEvent.getVille();
        String encodedAddress = address.replace(" ", "+");
        String url = "https://www.google.com/maps/search/?api=1&query=" + encodedAddress;
        getUI().ifPresent(ui -> ui.getPage().open(url, "_blank"));
    }
    private Component createMainInfoSection() {
        VerticalLayout infoSection = new VerticalLayout();
        infoSection.setPadding(true);
        infoSection.setSpacing(true);

        HorizontalLayout infoRow = new HorizontalLayout();
        infoRow.setWidthFull();
        infoRow.setSpacing(true);
        infoRow.setAlignItems(FlexComponent.Alignment.START);

        // Left column
        VerticalLayout leftCol = new VerticalLayout();
        leftCol.setPadding(false);
        leftCol.setSpacing(true);
        leftCol.setWidth("50%");

        leftCol.add(createInfoRow(VaadinIcon.CALENDAR, "Date de début", formatDate(currentEvent.getDateDebut())));
        leftCol.add(createInfoRow(VaadinIcon.CALENDAR_CLOCK, "Date de fin", formatDate(currentEvent.getDateFin())));
        leftCol.add(createInfoRow(VaadinIcon.MAP_MARKER, "Lieu", currentEvent.getLieu()));
        leftCol.add(createInfoRow(VaadinIcon.BUILDING, "Ville", currentEvent.getVille()));

        // Right column
        VerticalLayout rightCol = new VerticalLayout();
        rightCol.setPadding(false);
        rightCol.setSpacing(true);
        rightCol.setWidth("50%");

        Span prixLabel = new Span("Prix par place");
        prixLabel.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.9em")
                .set("font-weight", "500");

        H2 prix = new H2(String.format("%.2f DH", currentEvent.getPrixUnitaire()));
        prix.getStyle()
                .set("margin", "5px 0")
                .set("color", "var(--lumo-primary-color)")
                .set("font-size", "2em")
                .set("font-weight", "bold");

        VerticalLayout placesLayout = new VerticalLayout();
        placesLayout.setPadding(false);
        placesLayout.setSpacing(true);
        placesLayout.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px")
                .set("padding", "15px");

        HorizontalLayout capaciteRow = new HorizontalLayout();
        capaciteRow.setWidthFull();
        capaciteRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        Span capaciteLabel = new Span("Capacité totale:");
        capaciteLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
        Span capaciteValue = new Span(currentEvent.getCapaciteMax() + " places");
        capaciteValue.getStyle().set("color", "var(--lumo-primary-text-color)");
        capaciteRow.add(capaciteLabel, capaciteValue);

        HorizontalLayout reserveesRow = new HorizontalLayout();
        reserveesRow.setWidthFull();
        reserveesRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        Span reserveesLabel = new Span("Places réservées:");
        reserveesLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
        Span reserveesValue = new Span(currentEvent.getPlacesReservees() + " places");
        reserveesValue.getStyle().set("color", "#dc3545");
        reserveesRow.add(reserveesLabel, reserveesValue);

        HorizontalLayout disponiblesRow = new HorizontalLayout();
        disponiblesRow.setWidthFull();
        disponiblesRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        Span disponiblesLabel = new Span("Places disponibles:");
        disponiblesLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
        Span disponiblesValue = new Span(currentEvent.getPlacesDisponibles() + " places");
        disponiblesValue.getStyle().set("color", "#28a745").set("font-weight", "bold");
        disponiblesRow.add(disponiblesLabel, disponiblesValue);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setWidthFull();
        double taux = currentEvent.getTauxRemplissage() / 100.0;
        progressBar.setValue(taux);

        Span tauxSpan = new Span(String.format("Taux: %.1f%%", currentEvent.getTauxRemplissage()));
        tauxSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        placesLayout.add(capaciteRow, reserveesRow, disponiblesRow, progressBar, tauxSpan);

        rightCol.add(prixLabel, prix, placesLayout);

        infoRow.add(leftCol, rightCol);
        infoSection.add(infoRow);
        return infoSection;
    }

    private Component createDescriptionSection() {
        VerticalLayout desc = new VerticalLayout();
        desc.setWidthFull();
        desc.setPadding(true);
        desc.setSpacing(true);
        desc.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px")
                .set("margin", "10px 0");

        H3 descTitle = new H3("Description");
        descTitle.getStyle().set("color", "var(--lumo-primary-text-color)");

        Paragraph descText = new Paragraph(currentEvent.getDescription());
        descText.getStyle().set("color", "var(--lumo-secondary-text-color)");

        desc.add(descTitle, descText);
        return desc;
    }

    private Component createOrganizerSection() {
        VerticalLayout org = new VerticalLayout();
        org.setWidthFull();
        org.setPadding(true);
        org.setSpacing(true);

        H3 title = new H3("Organisateur");
        title.getStyle().set("color", "var(--lumo-primary-text-color)");

        User organizer = currentEvent.getOrganisateur();
        if (organizer != null) {
            Paragraph p = new Paragraph(organizer.getNom() + " " + organizer.getPrenom());
            p.getStyle().set("color", "var(--lumo-primary-text-color)");

            Span email = new Span(organizer.getEmail());
            email.getStyle().set("color", "var(--lumo-primary-color)");

            org.add(title, p, email);
        } else {
            Span noOrg = new Span("Non spécifié");
            noOrg.getStyle().set("color", "var(--lumo-secondary-text-color)");
            org.add(title, noOrg);
        }

        return org;
    }

    private Component createReservationSection() {
        HorizontalLayout reservation = new HorizontalLayout();
        reservation.setWidthFull();
        reservation.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        reservation.setPadding(true);
        reservation.setSpacing(true);
        reservation.getStyle()
                .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
                .set("border-radius", "8px")
                .set("margin", "20px 0")
                .set("align-items", "center");

        Button back = new Button("Retour", new Icon(VaadinIcon.ARROW_LEFT));
        back.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE);
        back.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("eventsClient")));

        Button reserve = new Button("Réserver", new Icon(VaadinIcon.TICKET));
        reserve.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        reserve.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("eventClient/" + eventId + "/reserve")));

        reservation.add(back, reserve);
        return reservation;
    }

    private HorizontalLayout createInfoRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon ic = new Icon(icon);
        ic.setSize("20px");
        ic.getStyle().set("color", "var(--lumo-primary-color)");

        VerticalLayout text = new VerticalLayout();
        text.setPadding(false);
        text.setSpacing(false);

        Span lab = new Span(label);
        lab.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.85em")
                .set("font-weight", "500");

        Span val = new Span(value == null ? "Non spécifié" : value);
        val.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "1em")
                .set("font-weight", "600");

        text.add(lab, val);
        row.add(ic, text);
        return row;
    }

    private String formatDate(LocalDateTime dt) {
        if (dt == null) return "Non spécifié";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH'h'mm");
        return dt.format(fmt);
    }

    private User getCurrentUser() {
        try {
            User sessionUser = VaadinSession.getCurrent().getAttribute(User.class);
            if (sessionUser != null) return sessionUser;

            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                return userService.findByEmail(((UserDetails) principal).getUsername());
            }
        } catch (Exception ignored) {}
        return null;
    }
}