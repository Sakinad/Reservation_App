package org.example.reservation_event.ui.Clients;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.StreamResource;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.dtos.EventDTO;
import org.example.reservation_event.email.EmailService;
import org.example.reservation_event.services.EventService;
import org.example.reservation_event.services.ReservationService;
import org.example.reservation_event.services.UserService;
import org.example.reservation_event.ui.MainLayout;
import org.example.reservation_event.utils.ImageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Route(value = "eventClient/:eventId/reserve", layout = MainLayout.class)
@PageTitle("Réserver un événement")
public class ReservationFormView extends VerticalLayout implements BeforeEnterObserver {

    private final EventService eventService;
    private final ReservationService reservationService;
    private final UserService userService;
    private final EmailService emailService;


    private Long eventId;
    private EventDTO eventDTO;
    private User currentUser;

    private IntegerField nombrePlacesField;
    private TextArea commentaireField;
    private Span prixUnitaireSpan;
    private H2 montantTotalH2;
    private Span placesDisponiblesSpan;
    private Button confirmButton;

    @Autowired
    public ReservationFormView(EventService eventService,
                               ReservationService reservationService,
                               UserService userService,
                               EmailService emailService) {
        this.eventService = eventService;
        this.reservationService = reservationService;
        this.userService = userService;
        this.emailService = emailService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        currentUser = getCurrentUser();
        if (currentUser == null) {
            Notification n = Notification.show("Veuillez vous connecter pour réserver un événement", 3000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            getUI().ifPresent(ui -> ui.navigate("login"));
            return;
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<String> param = event.getRouteParameters().get("eventId");
        if (param.isEmpty()) {
            Notification.show("ID d'événement invalide", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("eventsClient"));
            return;
        }

        try {
            this.eventId = Long.valueOf(param.get());
        } catch (NumberFormatException e) {
            Notification.show("ID d'événement invalide", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("eventsClient"));
            return;
        }

        try {
            Optional<EventDTO> dtoOpt = eventService.getEventForReservation(eventId);
            if (dtoOpt.isEmpty()) {
                Notification.show("Événement introuvable", 3000, Notification.Position.MIDDLE);
                getUI().ifPresent(ui -> ui.navigate("eventsClient"));
                return;
            }

            this.eventDTO = dtoOpt.get();

            if (eventDTO.getPlacesDisponibles() == null || eventDTO.getPlacesDisponibles() <= 0 || !Boolean.TRUE.equals(eventDTO.getIsReservable())) {
                Notification.show("Cet événement n'est pas disponible pour la réservation", 3000, Notification.Position.MIDDLE);
                getUI().ifPresent(ui -> ui.navigate("eventClient/" + eventId));
                return;
            }

            displayReservationForm();

        } catch (Exception e) {
            System.out.println("Erreur lors du chargement de l'event DTO: " + e.getMessage());
            e.printStackTrace();
            Notification.show("Erreur lors du chargement de l'événement", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("eventsClient"));
        }
    }

    private void displayReservationForm() {
        removeAll();

        // Back button
        Button backButton = new Button("Retour à l'événement", new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("eventClient/" + eventId)));
        add(backButton);

        HorizontalLayout main = new HorizontalLayout();
        main.setWidthFull();
        main.setSpacing(true);

        Component summary = createEventSummary();
        Component form = createReservationForm();

        main.add(summary, form);
        main.setFlexGrow(1, summary);
        main.setFlexGrow(1, form);

        add(main);
    }

    private Component createEventSummary() {
        VerticalLayout summary = new VerticalLayout();
        summary.setPadding(true);
        summary.setSpacing(true);
        summary.setWidth("48%");
        summary.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.06)");

        H3 title = new H3("Récapitulatif de l'événement");
        title.getStyle().set("color", "var(--lumo-primary-text-color)");
        summary.add(title);

        // Image section
        // Image section - Updated to match EventsClientView logic
        Div imageContainer = new Div();
        imageContainer.setWidthFull();
        imageContainer.setHeight("200px");
        imageContainer.getStyle()
                .set("border-radius", "8px")
                .set("overflow", "hidden")
                .set("position", "relative")
                .set("background-color", "var(--lumo-contrast-10pct)");

// Try to load the image with ImageUtils
        boolean hasImage = false;
        if (eventDTO.getImageUrl() != null && !eventDTO.getImageUrl().isEmpty()) {
            try {
                Image eventImage = ImageUtils.createEventImage(eventDTO.getImageUrl());
                eventImage.setWidthFull();
                eventImage.setHeight("100%");
                eventImage.getStyle()
                        .set("object-fit", "cover")
                        .set("position", "absolute")
                        .set("top", "0")
                        .set("left", "0");
                imageContainer.add(eventImage);
                hasImage = true;
            } catch (Exception e) {
                System.err.println("Error loading image for event " + eventDTO.getId() + ": " + e.getMessage());
            }
        }

// If no image, show gradient background with icon
        if (!hasImage) {
            imageContainer.getStyle()
                    .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("justify-content", "center");

            Icon defaultIcon = new Icon(VaadinIcon.CALENDAR);
            defaultIcon.setSize("48px");
            defaultIcon.setColor("white");
            imageContainer.add(defaultIcon);
        }

        summary.add(imageContainer);

        summary.add(detailRow("Date", formatDateTime(eventDTO.getDateDebut())));
        summary.add(detailRow("Lieu", (eventDTO.getLieu() == null ? "" : eventDTO.getLieu()) + (eventDTO.getVille() == null ? "" : ", " + eventDTO.getVille())));
        summary.add(detailRow("Catégorie", eventDTO.getCategorie() != null ? eventDTO.getCategorie().getLabel() : "—"));
        summary.add(detailRow("Prix unitaire", String.format("%.2f DH", eventDTO.getPrixUnitaire() == null ? 0.0 : eventDTO.getPrixUnitaire())));

        HorizontalLayout placesRow = detailRow("Places disponibles", "");
        int places = eventDTO.getPlacesDisponibles() == null ? 0 : eventDTO.getPlacesDisponibles();
        placesDisponiblesSpan = new Span(String.valueOf(places));
        placesDisponiblesSpan.getStyle().set("font-weight", "700");
        if (places > 10) {
            placesDisponiblesSpan.getStyle().set("color", "#059669");
        } else {
            placesDisponiblesSpan.getStyle().set("color", "#dc2626");
        }
        placesRow.add(placesDisponiblesSpan);
        summary.add(placesRow);

        return summary;
    }

    private HorizontalLayout detailRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "600");

        row.add(labelSpan, valueSpan);
        return row;
    }

    private Component createReservationForm() {
        VerticalLayout form = new VerticalLayout();
        form.setPadding(true);
        form.setSpacing(true);
        form.setWidth("48%");
        form.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.06)");

        H3 title = new H3("Formulaire de réservation");
        title.getStyle().set("color", "var(--lumo-primary-text-color)");
        form.add(title);

        nombrePlacesField = new IntegerField("Nombre de places");
        nombrePlacesField.setMin(1);
        nombrePlacesField.setMax(10);
        nombrePlacesField.setValue(1);
        nombrePlacesField.setStepButtonsVisible(true);
        nombrePlacesField.addValueChangeListener(e -> {
            updateMontantTotal();
            validateAvailability();
        });
        form.add(nombrePlacesField);

        prixUnitaireSpan = new Span(String.format("%.2f DH", eventDTO.getPrixUnitaire() == null ? 0.0 : eventDTO.getPrixUnitaire()));
        prixUnitaireSpan.getStyle().set("color", "var(--lumo-primary-text-color)");

        montantTotalH2 = new H2(String.format("%.2f DH", (eventDTO.getPrixUnitaire() == null ? 0.0 : eventDTO.getPrixUnitaire())));
        montantTotalH2.getStyle().set("color", "var(--lumo-primary-color)");

        Div priceDiv = new Div();
        Span priceLabel = new Span("Prix unitaire: ");
        priceLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
        priceDiv.add(priceLabel, prixUnitaireSpan);
        form.add(priceDiv);

        Div totalDiv = new Div();
        Span totalLabel = new Span("Montant total: ");
        totalLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
        totalDiv.add(totalLabel, montantTotalH2);
        form.add(totalDiv);

        commentaireField = new TextArea("Commentaire (optionnel)");
        commentaireField.setWidthFull();
        commentaireField.setPlaceholder("Ajoutez une note pour l'organisateur...");
        form.add(commentaireField);

        Div info = new Div();
        Span infoText = new Span("Les réservations peuvent être annulées jusqu'à 48h avant l'événement.");
        infoText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        info.add(infoText);
        info.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "8px")
                .set("border-radius", "6px");
        form.add(info);

        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button cancel = new Button("Annuler", new Icon(VaadinIcon.CLOSE));
        cancel.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("eventClient/" + eventId)));

        confirmButton = new Button("Confirmer la réservation", new Icon(VaadinIcon.CHECK));
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmButton.addClickListener(e -> onConfirmClicked());

        actions.add(cancel, confirmButton);
        form.add(actions);

        updateMontantTotal();
        validateAvailability();

        return form;
    }

    private void updateMontantTotal() {
        Integer nb = nombrePlacesField.getValue();
        if (nb == null || nb < 1) nb = 1;
        double pu = eventDTO.getPrixUnitaire() == null ? 0.0 : eventDTO.getPrixUnitaire();
        double total = nb * pu;
        montantTotalH2.setText(String.format("%.2f DH", total));
    }

    private void validateAvailability() {
        Integer nb = nombrePlacesField.getValue();
        int available = eventDTO.getPlacesDisponibles() == null ? 0 : eventDTO.getPlacesDisponibles();
        if (nb == null) nb = 1;
        if (nb > available) {
            nombrePlacesField.setInvalid(true);
            nombrePlacesField.setErrorMessage("Seulement " + available + " places disponibles");
            confirmButton.setEnabled(false);
        } else {
            nombrePlacesField.setInvalid(false);
            confirmButton.setEnabled(true);
        }
    }

    private void onConfirmClicked() {
        Integer nb = nombrePlacesField.getValue();
        if (nb == null || nb < 1) {
            Notification.show("Veuillez sélectionner au moins 1 place", 3000, Notification.Position.MIDDLE);
            return;
        }
        if (nb > 10) {
            Notification.show("Maximum 10 places par réservation", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            Event eventEntity = eventService.getEventById(eventId);
            int placesAvailableNow = eventService.getPlacesDisponibles(eventEntity);

            if (nb > placesAvailableNow) {
                Notification n = Notification.show("Désolé, il ne reste que " + placesAvailableNow + " places.", 4000, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                Optional<EventDTO> refreshed = eventService.getEventForReservation(eventId);
                refreshed.ifPresent(dto -> {
                    this.eventDTO = dto;
                    placesDisponiblesSpan.setText(String.valueOf(dto.getPlacesDisponibles()));
                    validateAvailability();
                });
                return;
            }

            Reservation reservation = reservationService.createReservation(
                    eventEntity,
                    currentUser,
                    nb,
                    commentaireField.getValue()
            );

            // AJOUT: Envoi de l'email de confirmation
            try {
                emailService.sendReservationConfirmation(reservation);
                System.out.println("✅ Confirmation email sent for reservation: " + reservation.getCodeReservation());
            } catch (Exception emailEx) {
                System.err.println("⚠️ Could not send confirmation email: " + emailEx.getMessage());
                // Ne pas bloquer la réservation si l'email échoue
            }

            showSuccessDialog(reservation);

        } catch (Exception ex) {
            System.out.println("Erreur réservation: " + ex.getMessage());
            ex.printStackTrace();
            Notification n = Notification.show("Erreur lors de la réservation: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    private void showSuccessDialog(Reservation reservation) {
        Dialog dialog = new Dialog();
        dialog.setWidth("520px");

        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon ok = new Icon(VaadinIcon.CHECK_CIRCLE);
        ok.setColor("#059669");
        ok.setSize("56px");

        H2 title = new H2("Réservation confirmée !");
        title.getStyle().set("color", "var(--lumo-primary-text-color)");

        Paragraph info = new Paragraph("Votre réservation a été effectuée avec succès. Conservez le code ci-dessous.");
        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        H1 code = new H1(reservation.getCodeReservation());
        code.getStyle()
                .set("font-family", "monospace")
                .set("letter-spacing", "2px")
                .set("color", "var(--lumo-primary-color)");

        layout.add(ok, title, info, code);

        HorizontalLayout actions = new HorizontalLayout();
        Button toReservations = new Button("Voir mes réservations", e -> {
            dialog.close();
            getUI().ifPresent(ui -> ui.navigate("my-reservations"));
        });
        toReservations.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button back = new Button("Retour aux événements", e -> {
            dialog.close();
            getUI().ifPresent(ui -> ui.navigate("eventsClient"));
        });

        actions.add(back, toReservations);
        layout.add(actions);

        dialog.add(layout);
        dialog.open();
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "Date non spécifiée";
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private User getCurrentUser() {
        try {
            User sessionUser = VaadinSession.getCurrent().getAttribute(User.class);
            if (sessionUser != null) return sessionUser;

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
}