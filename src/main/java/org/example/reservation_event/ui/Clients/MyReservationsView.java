package org.example.reservation_event.ui.Clients;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.Enums.ReservationStatut;
import org.example.reservation_event.Exceptions.ResourceNotFoundException;
import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.dtos.ReservationSummaryDTO;
import org.example.reservation_event.ServicesImplimentation.PdfTicketService;
import org.example.reservation_event.email.EmailService;
import org.example.reservation_event.services.ReservationService;
import org.example.reservation_event.services.ReviewService;
import org.example.reservation_event.services.UserService;
import org.example.reservation_event.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "my-reservations", layout = MainLayout.class)
@PageTitle("Mes Réservations")
public class MyReservationsView extends VerticalLayout {

    private final ReservationService reservationService;
    private final UserService userService;
    private final PdfTicketService pdfTicketService;
    private User currentUser;

    // Components
    private Grid<ReservationSummaryDTO> reservationsGrid;
    private TextField searchField;
    private ComboBox<ReservationStatut> statusFilter;
    private List<ReservationSummaryDTO> allReservations;
    private Span totalReservationsSpan;
    private Span upcomingEventsSpan;
    private Span totalSpentSpan;
    private final EmailService emailService;
    private final ReviewService reviewService;

    @Autowired
    public MyReservationsView(ReservationService reservationService,
                              UserService userService,
                              PdfTicketService pdfTicketService,
                              EmailService emailService,
                              ReviewService reviewService) {
        this.reservationService = reservationService;
        this.userService = userService;
        this.pdfTicketService = pdfTicketService;
        this.emailService = emailService;
        this.reviewService=reviewService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        // Get current user
        currentUser = getCurrentUser();
        if (currentUser == null) {
            showLoginRequired();
            return;
        }

        // Initialize layout
        initializeLayout();
    }

    private void initializeLayout() {
        add(
                createHeader(),
                createStatsSection(),
                createFiltersSection(),
                createReservationsGrid()
        );

        // Load reservations
        loadReservations();
    }

    private Component createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        H1 title = new H1("Mes Réservations");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Button newReservationButton = new Button("Nouvelle réservation", new Icon(VaadinIcon.PLUS));
        newReservationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newReservationButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("eventsClient")));

        header.add(title, newReservationButton);
        return header;
    }

    private Component createStatsSection() {
        HorizontalLayout statsSection = new HorizontalLayout();
        statsSection.setWidthFull();
        statsSection.setSpacing(true);

        totalReservationsSpan = new Span("0");
        upcomingEventsSpan = new Span("0");
        totalSpentSpan = new Span("0.00 DH");

        statsSection.add(
                createStatCard("Total réservations", totalReservationsSpan, VaadinIcon.CALENDAR, "#4f46e5"),
                createStatCard("À venir", upcomingEventsSpan, VaadinIcon.CLOCK, "#059669"),
                createStatCard("Total dépensé", totalSpentSpan, VaadinIcon.EURO, "#dc2626")
        );

        return statsSection;
    }

    private Component createStatCard(String label, Span valueSpan, VaadinIcon icon, String color) {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)")
                .set("border-left", "4px solid " + color);

        Icon cardIcon = new Icon(icon);
        cardIcon.setSize("32px");
        cardIcon.setColor(color);

        Span cardLabel = new Span(label);
        cardLabel.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "700")
                .set("color", "var(--lumo-primary-text-color)");

        card.add(cardIcon, cardLabel, valueSpan);
        return card;
    }

    private Component createFiltersSection() {
        VerticalLayout filtersSection = new VerticalLayout();
        filtersSection.setWidthFull();
        filtersSection.setPadding(true);
        filtersSection.setSpacing(true);
        filtersSection.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        H3 filtersTitle = new H3("Filtres");
        filtersTitle.getStyle()
                .set("margin", "0 0 15px 0")
                .set("color", "var(--lumo-primary-text-color)");

        HorizontalLayout filtersRow = new HorizontalLayout();
        filtersRow.setWidthFull();
        filtersRow.setSpacing(true);

        // Search by code
        searchField = new TextField("Rechercher par code");
        searchField.setPlaceholder("Ex: EVT-12345");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setClearButtonVisible(true);
        searchField.setWidth("300px");
        searchField.addValueChangeListener(e -> applyFilters());

        // Status filter
        statusFilter = new ComboBox<>("Statut");
        statusFilter.setItems(ReservationStatut.values());
        statusFilter.setItemLabelGenerator(ReservationStatut::getLabel);
        statusFilter.setPlaceholder("Tous les statuts");
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("200px");
        statusFilter.addValueChangeListener(e -> applyFilters());

        // Reset button
        Button resetButton = new Button("Réinitialiser", new Icon(VaadinIcon.REFRESH));
        resetButton.addClickListener(e -> resetFilters());

        filtersRow.add(searchField, statusFilter, resetButton);

        filtersSection.add(filtersTitle, filtersRow);
        return filtersSection;
    }

    private Component createReservationsGrid() {
        VerticalLayout gridSection = new VerticalLayout();
        gridSection.setWidthFull();
        gridSection.setPadding(true);
        gridSection.setSpacing(true);
        gridSection.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        H3 gridTitle = new H3("Liste des réservations");
        gridTitle.getStyle()
                .set("margin", "0 0 15px 0")
                .set("color", "var(--lumo-primary-text-color)");

        reservationsGrid = new Grid<>(ReservationSummaryDTO.class, false);
        reservationsGrid.setHeight("600px");
        reservationsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        // Code column
        reservationsGrid.addColumn(new ComponentRenderer<>(reservation -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setAlignItems(FlexComponent.Alignment.CENTER);

            if (isUpcoming(reservation)) {
                Icon upcomingIcon = new Icon(VaadinIcon.CIRCLE);
                upcomingIcon.setSize("8px");
                upcomingIcon.setColor("#059669");
                upcomingIcon.getElement().setAttribute("title", "À venir");
                layout.add(upcomingIcon);
            }

            Span code = new Span(reservation.getCodeReservation());
            code.getStyle()
                    .set("font-family", "monospace")
                    .set("font-weight", "600")
                    .set("color", "var(--lumo-primary-color)");

            layout.add(code);
            return layout;
        })).setHeader("Code").setWidth("150px").setFlexGrow(0);

        // Event column
        reservationsGrid.addColumn(ReservationSummaryDTO::getEvenementTitre)
                .setHeader("Événement")
                .setAutoWidth(true)
                .setFlexGrow(1);

        // Date column
        reservationsGrid.addColumn(reservation -> formatDate(reservation.getEvenementDateDebut()))
                .setHeader("Date")
                .setWidth("180px")
                .setFlexGrow(0);

        // Places column
        reservationsGrid.addColumn(new ComponentRenderer<>(reservation -> {
            Span places = new Span(reservation.getNombrePlaces() + " place(s)");
            places.getStyle().set("font-weight", "500");
            return places;
        })).setHeader("Places").setWidth("100px").setFlexGrow(0);

        // Amount column
        reservationsGrid.addColumn(new ComponentRenderer<>(reservation -> {
            Span amount = new Span(String.format("%.2f DH", reservation.getMontantTotal()));
            amount.getStyle().set("font-weight", "600");
            amount.getStyle().set("color", "#059669");
            return amount;
        })).setHeader("Montant").setWidth("120px").setFlexGrow(0);

        // Status column
        reservationsGrid.addColumn(new ComponentRenderer<>(reservation -> {
            Span statusBadge = new Span(reservation.getStatut().getLabel());
            statusBadge.getElement().getThemeList().add("badge");
            statusBadge.getStyle()
                    .set("background", getStatusColor(reservation.getStatut()))
                    .set("color", "white")
                    .set("padding", "4px 12px")
                    .set("border-radius", "12px")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("font-weight", "600");
            return statusBadge;
        })).setHeader("Statut").setWidth("120px").setFlexGrow(0);

        // Actions column
        // Remplacez la colonne Actions dans votre createReservationsGrid() par ceci :

// Actions column - TOUJOURS AFFICHER TOUS LES BOUTONS
        reservationsGrid.addColumn(new ComponentRenderer<>(reservation -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            actions.setAlignItems(FlexComponent.Alignment.CENTER);
            actions.setWidth("220px"); // ✅ Largeur fixe
            actions.getStyle().set("gap", "6px");

            // 1️⃣ Details button (TOUJOURS VISIBLE ET ACTIF)
            Button detailsButton = new Button(new Icon(VaadinIcon.EYE));
            detailsButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            detailsButton.getElement().setAttribute("title", "Voir détails");
            detailsButton.addClickListener(e -> showReservationDetails(reservation));
            detailsButton.getStyle()
                    .set("min-width", "40px")
                    .set("flex-shrink", "0");

            // 2️⃣ PDF button (TOUJOURS VISIBLE, activé si confirmée)
            Button pdfButton = new Button(new Icon(VaadinIcon.DOWNLOAD));
            pdfButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
            pdfButton.getElement().setAttribute("title", "Télécharger le billet PDF");
            boolean isPdfAvailable = reservation.getStatut() == ReservationStatut.CONFIRMEE;
            pdfButton.setEnabled(isPdfAvailable);
            if (!isPdfAvailable) {
                pdfButton.getStyle().set("opacity", "0.3"); // ✅ Griser si désactivé
            }
            pdfButton.addClickListener(e -> {
                if (isPdfAvailable) {
                    downloadPdf(reservation);
                }
            });
            pdfButton.getStyle()
                    .set("min-width", "40px")
                    .set("flex-shrink", "0");

            // 3️⃣ Review button (TOUJOURS VISIBLE, activé selon conditions)
            Button reviewButton = new Button(new Icon(VaadinIcon.STAR));
            reviewButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
            reviewButton.getElement().setAttribute("title", "Donner mon avis");
            boolean canReview = canReviewReservation(reservation);
            reviewButton.setEnabled(canReview);
            if (!canReview) {
                reviewButton.getStyle().set("opacity", "0.3"); // ✅ Griser si désactivé
                reviewButton.getElement().setAttribute("title", "Avis non disponible");
            }
            reviewButton.addClickListener(e -> {
                if (canReview) {
                    showReviewDialog(reservation);
                }
            });
            reviewButton.getStyle()
                    .set("min-width", "40px")
                    .set("flex-shrink", "0");

            // 4️⃣ Cancel button (TOUJOURS VISIBLE, activé si annulable)
            Button cancelButton = new Button(new Icon(VaadinIcon.CLOSE));
            cancelButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            cancelButton.getElement().setAttribute("title", "Annuler");
            boolean canCancel = canBeCancelled(reservation);
            cancelButton.setEnabled(canCancel);
            if (!canCancel) {
                cancelButton.getStyle().set("opacity", "0.3"); // ✅ Griser si désactivé
                cancelButton.getElement().setAttribute("title", "Annulation non disponible");
            }
            cancelButton.addClickListener(e -> {
                if (canCancel) {
                    confirmCancelReservation(reservation);
                }
            });
            cancelButton.getStyle()
                    .set("min-width", "40px")
                    .set("flex-shrink", "0");

            // ✅ Ajouter TOUS les boutons (toujours visibles)
            actions.add(detailsButton, pdfButton, reviewButton, cancelButton);

            return actions;
        })).setHeader("Actions").setWidth("220px").setFlexGrow(0).setAutoWidth(false);
        gridSection.add(gridTitle, reservationsGrid);
        return gridSection;
    }
    private boolean canReviewReservation(ReservationSummaryDTO reservation) {
        System.out.println("🔍 Vérification pour réservation " + reservation.getCodeReservation() + ":");
        System.out.println("   Statut réservation: " + reservation.getStatut());
        System.out.println("   Statut événement: " + reservation.getEvenementStatut()); // ⭐ NOUVEAU
        System.out.println("   Date événement: " + reservation.getEvenementDateDebut());

        // 1. Réservation CONFIRMÉE
        if (reservation.getStatut() != ReservationStatut.CONFIRMEE) {
            System.out.println("   ❌ REJET: Réservation non confirmée");
            return false;
        }

        // 2. Événement TERMINÉ
        if (reservation.getEvenementStatut() != EventStatut.TERMINE) {
            System.out.println("   ❌ REJET: Événement non terminé (statut: " +
                    reservation.getEvenementStatut() + ")");
            return false;
        }

        // 3. Date PASSÉE (sécurité supplémentaire)
        if (reservation.getEvenementDateDebut().isAfter(LocalDateTime.now())) {
            System.out.println("   ❌ REJET: Événement dans le futur");
            return false;
        }

        // 4. Pas encore noté
        try {
            boolean alreadyReviewed = reviewService.hasUserReviewedForReservation(reservation.getId());
            if (alreadyReviewed) {
                System.out.println("   ❌ REJET: Déjà noté");
                return false;
            }
        } catch (Exception e) {
            System.out.println("   ⚠️ ERREUR vérification avis: " + e.getMessage());
            return false;
        }

        System.out.println("   ✅ ACCEPTÉ: Toutes les conditions remplies");
        return true;
    }
    private void showReviewDialog(ReservationSummaryDTO reservationDTO) {
        System.out.println("🎬 OUVERTURE DIALOGUE pour: " + reservationDTO.getCodeReservation());

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("⭐ Votre avis sur: " + reservationDTO.getEvenementTitre());
        dialog.setWidth("500px");

        // ✅ AJOUTE CE LOG POUR VOIR SI LE DIALOGUE S'AFFICHE
        dialog.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                System.out.println("✅ Dialogue OUVERT avec succès");
            }
        });

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);

        // Info réservation
        VerticalLayout reservationInfo = new VerticalLayout(
                new Span("Événement: " + reservationDTO.getEvenementTitre()),
                new Span("Date: " + formatDateTime(reservationDTO.getEvenementDateDebut()))
        );

        reservationInfo.setSpacing(false);
        reservationInfo.setPadding(false);
        reservationInfo.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "10px")
                .set("border-radius", "8px");

        // Instructions
        Paragraph instructions = new Paragraph(
                "Notez votre expérience de 1 à 5 étoiles"
        );
        instructions.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Étoiles
        HorizontalLayout starsLayout = new HorizontalLayout();
        starsLayout.setSpacing(true);
        starsLayout.setAlignItems(Alignment.CENTER);

        Button[] stars = new Button[5];
        int[] selectedRating = {0};

        for (int i = 0; i < 5; i++) {
            final int starValue = i + 1;

            Button star = new Button(VaadinIcon.STAR_O.create());
            star.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

            star.getStyle()
                    .set("cursor", "pointer")
                    .set("color", "#f59e0b")
                    .set("font-size", "32px")
                    .set("padding", "0")
                    .set("margin", "0 3px");

            star.addClickListener(e -> {
                selectedRating[0] = starValue;

                // Update all stars
                for (int j = 0; j < 5; j++) {
                    stars[j].setIcon(
                            j < starValue
                                    ? VaadinIcon.STAR.create()
                                    : VaadinIcon.STAR_O.create()
                    );
                }
            });

            stars[i] = star;
            starsLayout.add(star);
        }

        // Commentaire
        TextArea commentField = new TextArea("Commentaire (optionnel)");
        commentField.setWidthFull();
        commentField.setMaxHeight("150px");
        commentField.setPlaceholder("Partagez votre expérience...");

        // Boutons
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(true);

        Button cancelButton = new Button("Annuler");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> dialog.close());

        Button submitButton = new Button("Publier mon avis");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.addClickListener(e -> {
            if (selectedRating[0] == 0) {
                Notification.show("Veuillez donner une note");
                return;
            }

            try {
                // ✅ Récupérer la réservation complète
                Reservation reservation = reservationService.getReservationByIdWithRelations(
                        reservationDTO.getId()
                );

                // ✅ Appel SIMPLIFIÉ (juste reservation, rating, comment)
                reviewService.saveOrUpdateReview(
                        reservation,
                        selectedRating[0],
                        commentField.getValue()
                );

                Notification.show("✅ Merci pour votre avis !", 3000,
                                Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                dialog.close();
                loadReservations();

            } catch (Exception ex) {
                ex.printStackTrace();
                Notification.show("❌ " + ex.getMessage(), 5000,
                                Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        buttons.add(cancelButton, submitButton);

        dialogLayout.add(reservationInfo, instructions, starsLayout, commentField, buttons);
        dialog.add(dialogLayout);
        dialog.open();
    }
    private void downloadPdf(ReservationSummaryDTO reservationDTO) {
        try {
            // ✅ UTILISER getReservationByIdWithRelations au lieu de getReservationById
            Reservation reservation = reservationService.getReservationByIdWithRelations(reservationDTO.getId());

            // Generate PDF bytes
            byte[] pdfData = pdfTicketService.generateTicketPdf(reservation);

            // Create stream resource for download
            String fileName = "billet_" + reservation.getCodeReservation() + ".pdf";
            StreamResource streamResource = new StreamResource(
                    fileName,
                    () -> new ByteArrayInputStream(pdfData)
            );

            // Set content type
            streamResource.setContentType("application/pdf");

            // Open in new tab which will trigger download
            getUI().ifPresent(ui -> {
                ui.getPage().open(String.valueOf(streamResource));
            });

            Notification.show("✅ Billet PDF généré avec succès!",
                            3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (ResourceNotFoundException e) {
            Notification.show("❌ Réservation introuvable",
                            3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur PDF: " + e.getMessage());
            Notification.show("❌ Erreur lors de la génération du PDF: " + e.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    private void loadReservations() {
        try {
            allReservations = reservationService.getReservationsByUserAsDTO(currentUser);
            updateStats();
            applyFilters();
        } catch (Exception e) {
            Notification.show("Erreur lors du chargement des réservations: " + e.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void applyFilters() {
        if (allReservations == null) return;

        List<ReservationSummaryDTO> filtered = allReservations;

        // Filter by code
        String searchText = searchField.getValue();
        if (searchText != null && !searchText.trim().isEmpty()) {
            filtered = filtered.stream()
                    .filter(r -> r.getCodeReservation().toLowerCase()
                            .contains(searchText.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Filter by status
        ReservationStatut selectedStatus = statusFilter.getValue();
        if (selectedStatus != null) {
            filtered = filtered.stream()
                    .filter(r -> r.getStatut() == selectedStatus)
                    .collect(Collectors.toList());
        }

        reservationsGrid.setItems(filtered);
    }

    private void resetFilters() {
        searchField.clear();
        statusFilter.clear();
        applyFilters();
    }

    private void updateStats() {
        if (allReservations == null) return;

        int total = allReservations.size();
        long upcoming = allReservations.stream()
                .filter(this::isUpcoming)
                .count();
        double totalSpent = allReservations.stream()
                .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                .mapToDouble(ReservationSummaryDTO::getMontantTotal)
                .sum();

        totalReservationsSpan.setText(String.valueOf(total));
        upcomingEventsSpan.setText(String.valueOf(upcoming));
        totalSpentSpan.setText(String.format("%.2f DH", totalSpent));
    }

    private void showReservationDetails(ReservationSummaryDTO reservation) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Détails de la réservation");
        dialog.setWidth("500px");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        addDetailRow(dialogLayout, "Code de réservation", reservation.getCodeReservation());
        addDetailRow(dialogLayout, "Événement", reservation.getEvenementTitre());
        addDetailRow(dialogLayout, "Date de l'événement", formatDateTime(reservation.getEvenementDateDebut()));
        addDetailRow(dialogLayout, "Lieu", reservation.getEvenementLieu());
        addDetailRow(dialogLayout, "Ville", reservation.getEvenementVille());
        addDetailRow(dialogLayout, "Nombre de places", String.valueOf(reservation.getNombrePlaces()));
        addDetailRow(dialogLayout, "Prix unitaire", String.format("%.2f DH", reservation.getEvenementPrixUnitaire()));
        addDetailRow(dialogLayout, "Montant total", String.format("%.2f DH", reservation.getMontantTotal()));
        addDetailRow(dialogLayout, "Date de réservation", formatDateTime(reservation.getDateReservation()));

        // Status badge
        HorizontalLayout statusLayout = new HorizontalLayout();
        statusLayout.setSpacing(true);
        statusLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Span statusLabel = new Span("Statut:");
        statusLabel.getStyle().set("font-weight", "600");
        Span statusValue = new Span(reservation.getStatut().getLabel());
        statusValue.getStyle()
                .set("background", getStatusColor(reservation.getStatut()))
                .set("color", "white")
                .set("padding", "4px 12px")
                .set("border-radius", "12px")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "600");
        statusLayout.add(statusLabel, statusValue);
        dialogLayout.add(statusLayout);

        // Comment if exists
        if (reservation.getCommentaire() != null && !reservation.getCommentaire().isEmpty()) {
            addDetailRow(dialogLayout, "Commentaire", reservation.getCommentaire());
        }

        Button closeButton = new Button("Fermer", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(dialogLayout);
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    private void addDetailRow(VerticalLayout container, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle()
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("color", "var(--lumo-primary-text-color)");

        row.add(labelSpan, valueSpan);
        container.add(row);
    }

    private void confirmCancelReservation(ReservationSummaryDTO reservation) {
        // Créer un dialogue personnalisé au lieu de ConfirmDialog
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Annuler la réservation");
        dialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);

        // Message
        Paragraph message = new Paragraph(
                "Êtes-vous sûr de vouloir annuler votre réservation pour :\n" +
                        reservation.getEvenementTitre() + " ?"
        );

        // Champ de raison (optionnel)
        TextArea reasonField = new TextArea("Raison (optionnel)");
        reasonField.setPlaceholder("Pourquoi annulez-vous ?");
        reasonField.setWidthFull();
        reasonField.setHeight("80px");

        // Boutons
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(true);

        Button cancelBtn = new Button("Non, garder", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button confirmBtn = new Button("Oui, annuler");
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        confirmBtn.addClickListener(e -> {
            String reason = reasonField.getValue();
            // ✅ Appel avec 2 paramètres maintenant
            cancelReservation(reservation);
            dialog.close();
        });

        buttons.add(cancelBtn, confirmBtn);

        layout.add(message, reasonField, buttons);
        dialog.add(layout);
        dialog.open();
    }


    private void cancelReservation(ReservationSummaryDTO reservationDTO) {
        try {
            // ✅ UTILISER getReservationByIdWithRelations COMME POUR LE PDF
            Reservation reservation = reservationService.getReservationByIdWithRelations(reservationDTO.getId());

            if (!canBeCancelled(reservationDTO)) {
                Notification.show("Cette réservation ne peut plus être annulée (moins de 48h avant l'événement)",
                                3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // ✅ L'annulation nécessite juste l'ID
            reservationService.cancelReservation(reservationDTO.getId());

            // ✅ Maintenant envoyer l'email (les relations sont chargées)
            try {
                boolean emailSent = emailService.sendReservationCancellation(reservation);
                if (emailSent) {
                    System.out.println("✅ Email d'annulation envoyé pour: " + reservation.getCodeReservation());
                    Notification successNotif = Notification.show(
                            "✅ Réservation annulée avec succès ! Un email de confirmation vous a été envoyé.",
                            4000, Notification.Position.TOP_CENTER
                    );
                    successNotif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    System.err.println("⚠️ Échec d'envoi d'email d'annulation");
                    Notification warningNotif = Notification.show(
                            "⚠️ Réservation annulée, mais l'email de confirmation n'a pas pu être envoyé.",
                            4000, Notification.Position.TOP_CENTER
                    );
                    warningNotif.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                }
            } catch (Exception emailEx) {
                System.err.println("❌ Erreur lors de l'envoi de l'email: " + emailEx.getMessage());
                Notification.show(
                        "✅ Réservation annulée (problème technique pour l'email).",
                        3000, Notification.Position.MIDDLE
                ).addThemeVariants(NotificationVariant.LUMO_PRIMARY);
            }

            loadReservations();

        } catch (Exception e) {
            Notification.show("❌ Erreur lors de l'annulation: " + e.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            e.printStackTrace();
        }
    }

    private boolean isUpcoming(ReservationSummaryDTO reservation) {
        return reservation.getStatut() == ReservationStatut.CONFIRMEE &&
                reservation.getEvenementDateDebut().isAfter(LocalDateTime.now());
    }

    private boolean canBeCancelled(ReservationSummaryDTO reservation) {
        if (reservation.getStatut() == ReservationStatut.ANNULEE) {
            return false;
        }

        LocalDateTime eventDate = reservation.getEvenementDateDebut();
        LocalDateTime cancellationDeadline = LocalDateTime.now().plusHours(48);

        return eventDate.isAfter(cancellationDeadline);
    }

    private String getStatusColor(ReservationStatut statut) {
        switch (statut) {
            case CONFIRMEE:
                return "#059669";
            case EN_ATTENTE:
                return "#f59e0b";
            case ANNULEE:
                return "#dc2626";
            default:
                return "#64748b";
        }
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateTime.format(formatter);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH'h'mm",
                new java.util.Locale("fr", "FR"));
        return dateTime.format(formatter);
    }

    private void showLoginRequired() {
        removeAll();

        VerticalLayout loginLayout = new VerticalLayout();
        loginLayout.setSizeFull();
        loginLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        loginLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon warningIcon = new Icon(VaadinIcon.EXCLAMATION_CIRCLE_O);
        warningIcon.setSize("64px");
        warningIcon.setColor("#f59e0b");

        H2 warningTitle = new H2("Connexion requise");
        warningTitle.getStyle().set("color", "#d97706");

        Span warningMessage = new Span("Vous devez être connecté pour voir vos réservations");

        Button loginButton = new Button("Se connecter", e -> getUI().ifPresent(ui -> ui.navigate("login")));
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        loginLayout.add(warningIcon, warningTitle, warningMessage, loginButton);
        add(loginLayout);
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
}