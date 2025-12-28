package org.example.reservation_event.ui.Publiques;

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
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.example.reservation_event.Exceptions.ResourceNotFoundException;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.Enums.EventCategorie;
import org.example.reservation_event.services.EventService;
import org.example.reservation_event.ui.MainLayout;
import org.example.reservation_event.utils.ImageUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Route(value = "event", layout = MainLayout.class)
@AnonymousAllowed
@PageTitle("Détails de l'Événement - EventBooking")
public class EventDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final EventService eventService;
    private Event event;
    private Long eventId;

    private VerticalLayout contentLayout;

    @Autowired
    public EventDetailView(EventService eventService) {
        this.eventService = eventService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setPadding(true);
        contentLayout.setSpacing(true);
        add(contentLayout);
    }

    @Override
    public void setParameter(BeforeEvent event, Long eventId) {
        this.eventId = eventId;
        loadEventDetails();
    }

    private void loadEventDetails() {
        try {
            event = eventService.getEventById(eventId);
            displayEventDetails();
        } catch (ResourceNotFoundException e) {
            displayError("Événement introuvable.");
        } catch (Exception e) {
            displayError("Erreur lors du chargement de l'événement: " + e.getMessage());
        }
    }

    private void displayEventDetails() {
        contentLayout.removeAll();

        // Container principal
        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setMaxWidth("1200px");
        mainContainer.setWidthFull();
        mainContainer.setPadding(true);
        mainContainer.setSpacing(true);
        mainContainer.getStyle()
                .set("margin", "0 auto")
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "var(--lumo-box-shadow-m)")
                .set("border", "1px solid var(--lumo-contrast-10pct)");

        // Bouton retour
        Button backButton = new Button("Retour à la liste", VaadinIcon.ARROW_LEFT.create());
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("events")));
        mainContainer.add(backButton);

        createImageSection(mainContainer);
        createMainInfoSection(mainContainer);
        createMapSection(mainContainer);
        createDescriptionSection(mainContainer);
        createDetailsSection(mainContainer);
        createOrganizerSection(mainContainer);
        createReservationSection(mainContainer);

        contentLayout.add(mainContainer);
    }

    private void createImageSection(VerticalLayout container) {
        Div imageContainer = new Div();
        imageContainer.setWidthFull();
        imageContainer.setHeight("400px");
        imageContainer.getStyle()
                .set("border-radius", "12px")
                .set("overflow", "hidden")
                .set("position", "relative");

        // Utiliser ImageUtils pour créer l'image
        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            Image eventImage = ImageUtils.createEventImage(event.getImageUrl(), "400px");
            eventImage.setWidth("100%");
            eventImage.setHeight("100%");
            eventImage.getStyle()
                    .set("object-fit", "cover")
                    .set("border-radius", "12px");
            imageContainer.add(eventImage);
        } else {
            imageContainer.getStyle()
                    .set("background", "linear-gradient(135deg, var(--lumo-primary-color) 0%, var(--lumo-primary-color-50pct) 100%)")
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("justify-content", "center");

            Icon defaultIcon = getIconForCategory(event.getCategorie());
            defaultIcon.setSize("128px");
            defaultIcon.getStyle().set("color", "white");
            imageContainer.add(defaultIcon);
        }

        // Badge catégorie
        Span categoryBadge = new Span(event.getCategorie().getLabel());
        categoryBadge.getStyle()
                .set("position", "absolute")
                .set("top", "20px")
                .set("right", "20px")
                .set("background", event.getCategorie().getColor())
                .set("color", "white")
                .set("padding", "10px 20px")
                .set("border-radius", "25px")
                .set("font-size", "1em")
                .set("font-weight", "bold")
                .set("box-shadow", "0 4px 8px rgba(0, 0, 0, 0.2)");
        imageContainer.add(categoryBadge);

        // Badge statut
        Span statusBadge = new Span(event.getStatut().getLabel());
        statusBadge.getStyle()
                .set("position", "absolute")
                .set("top", "20px")
                .set("left", "20px")
                .set("background", event.getStatut().getColor())
                .set("color", "white")
                .set("padding", "10px 20px")
                .set("border-radius", "25px")
                .set("font-size", "1em")
                .set("font-weight", "bold")
                .set("box-shadow", "0 4px 8px rgba(0, 0, 0, 0.2)");
        imageContainer.add(statusBadge);

        container.add(imageContainer);
    }

    private void createMainInfoSection(VerticalLayout container) {
        VerticalLayout infoSection = new VerticalLayout();
        infoSection.setPadding(true);
        infoSection.setSpacing(true);

        // Titre
        H1 titre = new H1(event.getTitre());
        titre.getStyle()
                .set("margin", "0 0 20px 0")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "2.5em");

        // Informations en ligne
        HorizontalLayout infoRow = new HorizontalLayout();
        infoRow.setWidthFull();
        infoRow.setSpacing(true);

        // Colonne gauche - Date et lieu
        VerticalLayout leftCol = new VerticalLayout();
        leftCol.setPadding(false);
        leftCol.setSpacing(true);
        leftCol.setWidth("50%");

        leftCol.add(
                createInfoRow(VaadinIcon.CALENDAR.create(), "Date de début", formatDateTime(event.getDateDebut())),
                createInfoRow(VaadinIcon.CALENDAR_CLOCK.create(), "Date de fin", formatDateTime(event.getDateFin())),
                createInfoRow(VaadinIcon.MAP_MARKER.create(), "Lieu", event.getLieu()),
                createInfoRow(VaadinIcon.BUILDING.create(), "Ville", event.getVille())
        );

        // Colonne droite - Prix et places
        VerticalLayout rightCol = new VerticalLayout();
        rightCol.setPadding(false);
        rightCol.setSpacing(true);
        rightCol.setWidth("50%");

        // Prix
        VerticalLayout prixLayout = new VerticalLayout();
        prixLayout.setPadding(false);
        prixLayout.setSpacing(false);

        Span prixLabel = new Span("Prix par place");
        prixLabel.getStyle()
                .set("color", "#666")
                .set("font-size", "0.9em")
                .set("font-weight", "500");

        H2 prix = new H2(String.format("%.2f DH", event.getPrixUnitaire()));
        prix.getStyle()
                .set("margin", "5px 0")
                .set("color", "var(--lumo-primary-color)")
                .set("font-size", "2em")
                .set("font-weight", "bold");

        prixLayout.add(prixLabel, prix);

        // Capacité et places
        VerticalLayout placesLayout = new VerticalLayout();
        placesLayout.setPadding(true);
        placesLayout.setSpacing(true);
        placesLayout.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px")
                .set("margin-top", "15px");

        HorizontalLayout capaciteRow = createInfoItemRow("Capacité totale:", event.getCapaciteMax() + " places");
        HorizontalLayout reserveesRow = createInfoItemRow("Places réservées:", event.getPlacesReservees() + " places");
        reserveesRow.getComponentAt(1).getElement().getStyle().set("color", "#dc3545");

        HorizontalLayout disponiblesRow = createInfoItemRow("Places disponibles:", event.getPlacesDisponibles() + " places");
        disponiblesRow.getComponentAt(1).getElement().getStyle().set("color", "#28a745").set("font-weight", "bold");

        // Barre de progression
        ProgressBar progressBar = new ProgressBar();
        progressBar.setWidthFull();
        progressBar.setValue(event.getTauxRemplissage() / 100.0);

        Span progressLabel = new Span(String.format("Taux de remplissage: %.1f%%", event.getTauxRemplissage()));
        progressLabel.getStyle()
                .set("font-size", "0.9em")
                .set("color", "#666")
                .set("text-align", "center")
                .set("display", "block")
                .set("margin-top", "5px");

        placesLayout.add(capaciteRow, reserveesRow, disponiblesRow, progressBar, progressLabel);
        rightCol.add(prixLayout, placesLayout);

        infoRow.add(leftCol, rightCol);
        infoSection.add(titre, infoRow);
        container.add(infoSection);
    }
    private void createMapSection(VerticalLayout container) {
        VerticalLayout mapSection = new VerticalLayout();
        mapSection.setPadding(true);
        mapSection.setSpacing(true);
        mapSection.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("margin", "20px 0");

        H3 mapTitle = new H3("Localisation");
        mapTitle.getStyle()
                .set("margin", "0 0 15px 0")
                .set("color", "var(--lumo-primary-text-color)");

        // Adresse complète
        String address = event.getLieu() + ", " + event.getVille();
        String encodedAddress = address.replace(" ", "+");

        String mapUrl = "https://www.google.com/maps?q="
                + encodedAddress
                + "&output=embed";

        IFrame mapFrame = new IFrame(mapUrl);
        mapFrame.setWidth("100%");
        mapFrame.setHeight("350px");
        mapFrame.getStyle()
                .set("border", "0")
                .set("border-radius", "8px");

        // Bouton ouvrir dans Google Maps
        Button openMapsBtn = new Button("Ouvrir dans Google Maps", VaadinIcon.EXTERNAL_LINK.create());
        openMapsBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        openMapsBtn.addClickListener(e -> {
            String url = "https://www.google.com/maps/search/?api=1&query="
                    + encodedAddress;
            getUI().ifPresent(ui -> ui.getPage().open(url));
        });

        mapSection.add(mapTitle, mapFrame, openMapsBtn);
        container.add(mapSection);
    }
    private void createDescriptionSection(VerticalLayout container) {
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            VerticalLayout descSection = new VerticalLayout();
            descSection.setPadding(true);
            descSection.setSpacing(true);
            descSection.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "8px")
                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("margin", "20px 0");

            H3 descTitle = new H3("Description");
            descTitle.getStyle()
                    .set("margin", "0 0 15px 0")
                    .set("color", "var(--lumo-primary-text-color)");

            Paragraph description = new Paragraph(event.getDescription());
            description.getStyle()
                    .set("color", "var(--lumo-primary-text-color)")
                    .set("font-size", "1.1em")
                    .set("line-height", "1.6")
                    .set("margin", "0");

            descSection.add(descTitle, description);
            container.add(descSection);
        }
    }

    private void createDetailsSection(VerticalLayout container) {
        VerticalLayout detailsSection = new VerticalLayout();
        detailsSection.setPadding(true);
        detailsSection.setSpacing(true);

        H3 detailsTitle = new H3("Informations Complémentaires");
        detailsTitle.getStyle()
                .set("margin", "0 0 15px 0")
                .set("color", "var(--lumo-primary-text-color)");

        HorizontalLayout detailsGrid = new HorizontalLayout();
        detailsGrid.setWidthFull();
        detailsGrid.setSpacing(true);

        // Colonne 1
        VerticalLayout col1 = new VerticalLayout();
        col1.setPadding(false);
        col1.setSpacing(true);
        col1.setWidth("50%");

        col1.add(
                createDetailCard("Date de création", formatDateTime(event.getDateCreation())),
                createDetailCard("Dernière modification", formatDateTime(event.getDateModification()))
        );

        // Colonne 2
        VerticalLayout col2 = new VerticalLayout();
        col2.setPadding(false);
        col2.setSpacing(true);
        col2.setWidth("50%");

        col2.add(
                createDetailCard("Revenu total", String.format("%.2f DH", event.getRevenuTotal())),
                createDetailCard("Durée de l'événement", calculateDuration())
        );

        detailsGrid.add(col1, col2);
        detailsSection.add(detailsTitle, detailsGrid);
        container.add(detailsSection);
    }

    private void createOrganizerSection(VerticalLayout container) {
        VerticalLayout organizerSection = new VerticalLayout();
        organizerSection.setPadding(true);
        organizerSection.setSpacing(true);
        organizerSection.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("margin", "20px 0");

        H3 organizerTitle = new H3("Informations sur l'Organisateur");
        organizerTitle.getStyle()
                .set("margin", "0 0 15px 0")
                .set("color", "var(--lumo-primary-text-color)");

        HorizontalLayout organizerInfo = new HorizontalLayout();
        organizerInfo.setSpacing(true);
        organizerInfo.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon userIcon = VaadinIcon.USER_CARD.create();
        userIcon.setSize("48px");
        userIcon.getStyle().set("color", "var(--lumo-primary-color)");

        VerticalLayout organizerDetails = new VerticalLayout();
        organizerDetails.setPadding(false);
        organizerDetails.setSpacing(false);

        H4 organizerName = new H4(event.getOrganisateur().getNomComplet());
        organizerName.getStyle()
                .set("margin", "0 0 5px 0")
                .set("color", "var(--lumo-primary-text-color)");

        Span organizerRole = new Span("Organisateur - " + event.getOrganisateur().getRole().getLabel());
        organizerRole.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.9em");

        Span organizerEmail = new Span(event.getOrganisateur().getEmail());
        organizerEmail.getStyle().set("color", "var(--lumo-primary-color)").set("font-size", "0.9em");

        organizerDetails.add(organizerName, organizerRole, organizerEmail);

        if (event.getOrganisateur().getTelephone() != null) {
            Span organizerPhone = new Span("Tél: " + event.getOrganisateur().getTelephone());
            organizerPhone.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "0.9em");
            organizerDetails.add(organizerPhone);
        }

        organizerInfo.add(userIcon, organizerDetails);
        organizerSection.add(organizerTitle, organizerInfo);
        container.add(organizerSection);
    }

    private void createReservationSection(VerticalLayout container) {
        VerticalLayout reservationSection = new VerticalLayout();
        reservationSection.setPadding(true);
        reservationSection.setSpacing(true);
        reservationSection.setAlignItems(FlexComponent.Alignment.CENTER);
        reservationSection.getStyle()
                .set("background", "linear-gradient(135deg, var(--lumo-primary-color) 0%, var(--lumo-primary-color-50pct) 100%)")
                .set("border-radius", "8px")
                .set("margin", "20px 0");

        H3 reservationTitle = new H3("Prêt à réserver ?");
        reservationTitle.getStyle()
                .set("margin", "0 0 15px 0")
                .set("color", "white")
                .set("text-align", "center");

        Paragraph reservationText = new Paragraph(
                "Connectez-vous ou inscrivez-vous pour réserver vos places pour cet événement."
        );
        reservationText.getStyle()
                .set("color", "rgba(255, 255, 255, 0.9)")
                .set("text-align", "center")
                .set("margin", "0 0 20px 0");

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        if (event.isReservable()) {
            Button reserveButton = new Button("Réserver maintenant", VaadinIcon.TICKET.create());
            reserveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
            reserveButton.getStyle()
                    .set("background", "white")
                    .set("color", "var(--lumo-primary-color)")
                    .set("font-weight", "bold");
            reserveButton.addClickListener(e -> {
                Notification notification = Notification.show(
                        "Veuillez vous connecter pour réserver",
                        3000,
                        Notification.Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                getUI().ifPresent(ui -> ui.navigate("login"));
            });

            buttonLayout.add(reserveButton);
        } else {
            Span unavailableText = new Span("Cet événement n'est plus disponible à la réservation");
            unavailableText.getStyle()
                    .set("color", "white")
                    .set("font-size", "1.1em")
                    .set("font-weight", "bold")
                    .set("background", "rgba(220, 53, 69, 0.8)")
                    .set("padding", "10px 20px")
                    .set("border-radius", "8px");
            reservationSection.add(unavailableText);
        }

        Button loginButton = new Button("Se connecter", VaadinIcon.SIGN_IN.create());
        loginButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE);
        loginButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("login")));

        Button registerButton = new Button("S'inscrire", VaadinIcon.USER_CHECK.create());
        registerButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_LARGE);
        registerButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("register")));

        buttonLayout.add(loginButton, registerButton);

        reservationSection.add(reservationTitle, reservationText, buttonLayout);
        container.add(reservationSection);
    }

    // Méthodes utilitaires

    private HorizontalLayout createInfoRow(Icon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.getStyle().set("margin-bottom", "10px");

        icon.setSize("20px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");

        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.85em")
                .set("font-weight", "500");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "1em")
                .set("font-weight", "600");

        textLayout.add(labelSpan, valueSpan);
        row.add(icon, textLayout);
        return row;
    }

    private HorizontalLayout createInfoItemRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("font-weight", "500");

        Span valueSpan = new Span(value);

        row.add(labelSpan, valueSpan);
        return row;
    }

    private Div createDetailCard(String label, String value) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "8px")
                .set("padding", "15px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-left", "4px solid var(--lumo-primary-color)");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.85em")
                .set("font-weight", "500")
                .set("display", "block")
                .set("margin-bottom", "5px");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "1.1em")
                .set("font-weight", "600")
                .set("display", "block");

        card.add(labelSpan, valueSpan);
        return card;
    }

    private String calculateDuration() {
        if (event.getDateDebut() == null || event.getDateFin() == null) {
            return "Non spécifiée";
        }

        long hours = java.time.Duration.between(event.getDateDebut(), event.getDateFin()).toHours();
        long days = hours / 24;
        hours = hours % 24;

        if (days > 0) {
            return String.format("%d jour(s) et %d heure(s)", days, hours);
        } else {
            return String.format("%d heure(s)", hours);
        }
    }

    private Icon getIconForCategory(EventCategorie categorie) {
        switch (categorie) {
            case CONCERT: return VaadinIcon.MUSIC.create();
            case THEATRE: return VaadinIcon.GROUP.create();
            case CONFERENCE: return VaadinIcon.PRESENTATION.create();
            case SPORT: return VaadinIcon.TROPHY.create();
            case AUTRE: return VaadinIcon.STAR.create();
            default: return VaadinIcon.CALENDAR.create();
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "Non spécifié";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "EEEE d MMMM yyyy 'à' HH'h'mm",
                new Locale("fr", "FR")
        );
        return dateTime.format(formatter);
    }

    private void displayError(String message) {
        contentLayout.removeAll();

        VerticalLayout errorLayout = new VerticalLayout();
        errorLayout.setSizeFull();
        errorLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        errorLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        Icon errorIcon = VaadinIcon.WARNING.create();
        errorIcon.setSize("64px");
        errorIcon.getStyle().set("color", "#dc3545");

        H2 errorTitle = new H2("Erreur");
        errorTitle.getStyle().set("color", "#dc3545");

        Paragraph errorMessage = new Paragraph(message);
        errorMessage.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("font-size", "1.1em");

        Button backButton = new Button("Retour à la liste", VaadinIcon.ARROW_LEFT.create());
        backButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("events")));

        errorLayout.add(errorIcon, errorTitle, errorMessage, backButton);
        contentLayout.add(errorLayout);
    }
}