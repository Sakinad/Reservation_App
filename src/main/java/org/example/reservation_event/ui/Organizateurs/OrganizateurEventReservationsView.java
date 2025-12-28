package org.example.reservation_event.ui.Organizateurs;

import com.itextpdf.kernel.pdf.PdfWriter;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.RolesAllowed;
import org.example.reservation_event.Enums.ReservationStatut;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.services.EventService;
import org.example.reservation_event.services.ReservationService;
import org.example.reservation_event.services.UserService;
import org.example.reservation_event.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.example.reservation_event.ServicesImplimentation.PdfReservationExportService;

import javax.swing.text.Document;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "organizer/event-reservations", layout = MainLayout.class)
@PageTitle("Réservations - Organisateur")
public class OrganizateurEventReservationsView extends VerticalLayout implements HasUrlParameter<Long> {

    private final EventService eventService;
    private final ReservationService reservationService;
    private final UserService userService;

    private User currentUser;
    private Event currentEvent;
    private List<Reservation> allReservations;

    private Grid<Reservation> reservationsGrid;
    private ComboBox<ReservationStatut> statusFilter;
    private TextField searchField;
    private final PdfReservationExportService pdfExportService;

    private Span totalReservationsValue;
    private Span totalPlacesValue;
    private Span totalRevenueValue;
    private Span confirmedReservationsValue;

    @Autowired
    public OrganizateurEventReservationsView(EventService eventService,
                                             ReservationService reservationService,
                                             UserService userService,
                                             PdfReservationExportService pdfExportService) {  // ✅ AJOUTÉ
        this.eventService = eventService;
        this.reservationService = reservationService;
        this.userService = userService;
        this.pdfExportService = pdfExportService;  // ✅ AJOUTÉ

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("overflow-y", "auto");
    }

    @Override
    public void setParameter(BeforeEvent event, Long eventId) {
        currentUser = getCurrentUser();

        if (currentUser == null || !isOrganizer(currentUser)) {
            showAccessDenied();
            return;
        }

        if (eventId == null) {
            showEventNotFound();
            return;
        }

        try {
            List<Event> organizerEvents = eventService.getEventsByOrganizer(currentUser);
            this.currentEvent = organizerEvents.stream()
                    .filter(e -> e.getId().equals(eventId))
                    .findFirst()
                    .orElse(null);

            if (this.currentEvent == null) {
                showEventNotFound();
                return;
            }

            initializeLayout();
            loadReservations();

        } catch (Exception e) {
            e.printStackTrace();
            showEventNotFound();
        }
    }

    private void initializeLayout() {
        removeAll();
        add(
                createHeader(),
                createEventInfoCard(),
                createStatisticsSection(),
                createFiltersSection(),
                createReservationsGrid()
        );
    }

    private Component createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.05)")
                .set("border-radius", "12px");

        VerticalLayout titleSection = new VerticalLayout();
        titleSection.setSpacing(false);
        titleSection.setPadding(false);

        H1 title = new H1("Réservations");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Span subtitle = new Span(currentEvent != null ? currentEvent.getTitre() : "");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        titleSection.add(title, subtitle);

        Button backButton = new Button("Retour aux événements", new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("organizer/events")));

        header.add(titleSection, backButton);
        header.setFlexGrow(1, titleSection);
        header.setFlexGrow(0, backButton);

        return header;
    }

    private Component createEventInfoCard() {
        HorizontalLayout card = new HorizontalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
                .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
                .set("border-radius", "12px")
                .set("color", "white")
                .set("box-shadow", "0 4px 12px rgba(0,0,0,0.15)");

        VerticalLayout info = new VerticalLayout();
        info.setSpacing(false);
        info.setPadding(false);

        H3 eventTitle = new H3(currentEvent.getTitre());
        eventTitle.getStyle().set("margin", "0 0 8px 0").set("color", "white");

        Span eventDetails = new Span(
                currentEvent.getCategorie().getLabel() + " • " +
                        formatDate(currentEvent.getDateDebut()) + " • " +
                        currentEvent.getLieu()
        );
        eventDetails.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("opacity", "0.9");

        info.add(eventTitle, eventDetails);

        Span capacityBadge = new Span(
                reservationService.getTotalReservedPlaces(currentEvent.getId()) +
                        "/" + currentEvent.getCapaciteMax() + " places"
        );
        capacityBadge.getStyle()
                .set("background", "rgba(255,255,255,0.2)")
                .set("padding", "6px 12px")
                .set("border-radius", "20px")
                .set("font-weight", "600")
                .set("white-space", "nowrap");

        card.add(info, capacityBadge);
        card.setFlexGrow(1, info);
        card.setFlexGrow(0, capacityBadge);
        card.setAlignItems(FlexComponent.Alignment.CENTER);

        return card;
    }

    private Component createStatisticsSection() {
        HorizontalLayout statsContainer = new HorizontalLayout();
        statsContainer.setWidthFull();
        statsContainer.setSpacing(true);

        statsContainer.add(
                createStatCard("Total Réservations", "0", VaadinIcon.TICKET, "#3b82f6",
                        stat -> totalReservationsValue = stat),
                createStatCard("Places Réservées", "0", VaadinIcon.USERS, "#10b981",
                        stat -> totalPlacesValue = stat),
                createStatCard("Revenus", "0 DH", VaadinIcon.DOLLAR, "#f59e0b",
                        stat -> totalRevenueValue = stat),
                createStatCard("Confirmées", "0", VaadinIcon.CHECK_CIRCLE, "#8b5cf6",
                        stat -> confirmedReservationsValue = stat)
        );

        return statsContainer;
    }

    private Component createStatCard(String label, String initialValue, VaadinIcon icon,
                                     String color, StatValueSetter setter) {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "500");

        Icon cardIcon = new Icon(icon);
        cardIcon.setSize("20px");
        cardIcon.setColor(color);
        cardIcon.getStyle()
                .set("padding", "8px")
                .set("background", color + "20")
                .set("border-radius", "8px");

        header.add(labelSpan, cardIcon);

        Span value = new Span(initialValue);
        value.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "700")
                .set("color", "var(--lumo-primary-text-color)");

        setter.setValue(value);

        card.add(header, value);
        return card;
    }

    @FunctionalInterface
    interface StatValueSetter {
        void setValue(Span span);
    }

    private Component createFiltersSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        HorizontalLayout filtersRow = new HorizontalLayout();
        filtersRow.setWidthFull();
        filtersRow.setSpacing(true);
        filtersRow.setAlignItems(FlexComponent.Alignment.END);

        searchField = new TextField();
        searchField.setPlaceholder("Rechercher...");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setClearButtonVisible(true);
        searchField.setWidth("300px");
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> applyFilters());

        statusFilter = new ComboBox<>("Statut");
        statusFilter.setItems(ReservationStatut.values());
        statusFilter.setItemLabelGenerator(ReservationStatut::name);
        statusFilter.setPlaceholder("Tous");
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("200px");
        statusFilter.addValueChangeListener(e -> applyFilters());

        Button exportCsvButton = new Button("Export CSV", new Icon(VaadinIcon.DOWNLOAD));
        exportCsvButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        exportCsvButton.addClickListener(e -> exportToCSV());

        Button exportPdfButton = new Button("Export PDF", new Icon(VaadinIcon.FILE_TEXT));
        exportPdfButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        exportPdfButton.addClickListener(e -> exportToPDF());

        Button resetButton = new Button("Réinitialiser", new Icon(VaadinIcon.REFRESH));
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetButton.addClickListener(e -> {
            searchField.clear();
            statusFilter.clear();
            applyFilters();
        });

        filtersRow.add(searchField, statusFilter, exportCsvButton, exportPdfButton, resetButton);
        section.add(filtersRow);


        return section;
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

        reservationsGrid = new Grid<>(Reservation.class, false);
        reservationsGrid.setWidthFull();
        reservationsGrid.setHeight("500px");
        reservationsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);

        reservationsGrid.addColumn(new ComponentRenderer<>(reservation -> {
                    Span code = new Span(reservation.getCodeReservation());
                    code.getStyle()
                            .set("font-family", "monospace")
                            .set("font-weight", "600")
                            .set("color", "#4f46e5")
                            .set("background", "#ede9fe")
                            .set("padding", "4px 8px")
                            .set("border-radius", "4px");
                    return code;
                }))
                .setHeader("Code")
                .setWidth("140px")
                .setFlexGrow(0);

        reservationsGrid.addColumn(reservation -> {
                    User user = reservation.getUtilisateur();
                    return user != null ? user.getPrenom() + " " + user.getNom() : "N/A";
                })
                .setHeader("Client")
                .setAutoWidth(true)
                .setFlexGrow(1);

        reservationsGrid.addColumn(reservation -> {
                    User user = reservation.getUtilisateur();
                    return user != null ? user.getEmail() : "N/A";
                })
                .setHeader("Email")
                .setAutoWidth(true)
                .setFlexGrow(1);

        reservationsGrid.addColumn(new ComponentRenderer<>(reservation -> {
                    Span places = new Span(String.valueOf(reservation.getNombrePlaces()));
                    places.getStyle()
                            .set("font-weight", "600")
                            .set("color", "var(--lumo-primary-text-color)");
                    return places;
                }))
                .setHeader("Places")
                .setWidth("80px")
                .setFlexGrow(0);

        reservationsGrid.addColumn(new ComponentRenderer<>(reservation -> {
                    Span amount = new Span(String.format("%.2f DH", reservation.getMontantTotal()));
                    amount.getStyle()
                            .set("font-weight", "600")
                            .set("color", "#059669");
                    return amount;
                }))
                .setHeader("Montant")
                .setWidth("120px")
                .setFlexGrow(0);

        reservationsGrid.addColumn(reservation -> formatDateTime(reservation.getDateReservation()))
                .setHeader("Date")
                .setWidth("180px")
                .setFlexGrow(0);

        reservationsGrid.addColumn(new ComponentRenderer<>(reservation -> {
                    Span badge = new Span(reservation.getStatut().name());
                    badge.getStyle()
                            .set("background", getReservationStatusColor(reservation.getStatut()))
                            .set("color", "white")
                            .set("padding", "4px 12px")
                            .set("border-radius", "12px")
                            .set("font-size", "var(--lumo-font-size-s)")
                            .set("font-weight", "600");
                    return badge;
                }))
                .setHeader("Statut")
                .setWidth("120px")
                .setFlexGrow(0);

        reservationsGrid.addColumn(new ComponentRenderer<>(reservation -> {
                    HorizontalLayout actions = new HorizontalLayout();
                    actions.setSpacing(true);

                    Button viewButton = new Button(new Icon(VaadinIcon.EYE));
                    viewButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                    viewButton.addClickListener(e -> showReservationDetails(reservation));

                    actions.add(viewButton);

                    if (reservation.getStatut() == ReservationStatut.EN_ATTENTE) {
                        Button confirmButton = new Button(new Icon(VaadinIcon.CHECK));
                        confirmButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                        confirmButton.addClickListener(e -> confirmReservation(reservation));
                        actions.add(confirmButton);
                    }

                    if (reservation.getStatut() == ReservationStatut.CONFIRMEE ||
                            reservation.getStatut() == ReservationStatut.EN_ATTENTE) {
                        Button cancelButton = new Button(new Icon(VaadinIcon.CLOSE));
                        cancelButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
                        cancelButton.addClickListener(e -> confirmCancelReservation(reservation));
                        actions.add(cancelButton);
                    }

                    return actions;
                }))
                .setHeader("Actions")
                .setWidth("150px")
                .setFlexGrow(0);

        gridSection.add(reservationsGrid);
        return gridSection;
    }

    private void loadReservations() {
        try {
            allReservations = reservationService.getReservationsByEvent(currentEvent);
            applyFilters();
            updateStatistics();
        } catch (Exception e) {
            Notification.show("Erreur: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void applyFilters() {
        if (allReservations == null) return;

        List<Reservation> filtered = allReservations;

        ReservationStatut selectedStatus = statusFilter.getValue();
        if (selectedStatus != null) {
            filtered = filtered.stream()
                    .filter(r -> r.getStatut() == selectedStatus)
                    .collect(Collectors.toList());
        }

        String searchText = searchField.getValue();
        if (searchText != null && !searchText.trim().isEmpty()) {
            String search = searchText.toLowerCase().trim();
            filtered = filtered.stream()
                    .filter(r -> {
                        User user = r.getUtilisateur();
                        return r.getCodeReservation().toLowerCase().contains(search) ||
                                (user != null && (
                                        user.getNom().toLowerCase().contains(search) ||
                                                user.getPrenom().toLowerCase().contains(search) ||
                                                user.getEmail().toLowerCase().contains(search)
                                ));
                    })
                    .collect(Collectors.toList());
        }

        reservationsGrid.setItems(filtered);
    }

    private void updateStatistics() {
        if (allReservations == null) return;

        int totalReservations = allReservations.size();

        int totalPlaces = allReservations.stream()
                .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE ||
                        r.getStatut() == ReservationStatut.EN_ATTENTE)
                .mapToInt(Reservation::getNombrePlaces)
                .sum();

        double totalRevenue = allReservations.stream()
                .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                .mapToDouble(Reservation::getMontantTotal)
                .sum();

        long confirmedCount = allReservations.stream()
                .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                .count();

        totalReservationsValue.setText(String.valueOf(totalReservations));
        totalPlacesValue.setText(String.valueOf(totalPlaces));
        totalRevenueValue.setText(String.format("%.2f DH", totalRevenue));
        confirmedReservationsValue.setText(String.valueOf(confirmedCount));
    }

    private void exportToCSV() {
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("\uFEFF"); // UTF-8 BOM
            csv.append("Code,Client,Email,Places,Montant,Date,Statut\n");

            List<Reservation> reservations = getFilteredReservations();

            for (Reservation r : reservations) {
                User user = r.getUtilisateur();
                String clientName = user != null ? user.getPrenom() + " " + user.getNom() : "N/A";
                String email = user != null ? user.getEmail() : "N/A";

                csv.append(String.format("\"%s\",\"%s\",\"%s\",%d,%.2f,\"%s\",\"%s\"\n",
                        r.getCodeReservation(),
                        clientName,
                        email,
                        r.getNombrePlaces(),
                        r.getMontantTotal(),
                        formatDateTime(r.getDateReservation()),
                        r.getStatut().name()
                ));
            }

            String fileName = "reservations_" + currentEvent.getTitre().replaceAll("[^a-zA-Z0-9]", "_") +
                    "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

            StreamResource resource = new StreamResource(fileName, () ->
                    new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8))
            );
            resource.setContentType("text/csv");

            Anchor downloadLink = new Anchor(resource, "");
            downloadLink.getElement().setAttribute("download", true);
            downloadLink.setId("csv-download-link");
            downloadLink.getStyle().set("display", "none");

            getUI().ifPresent(ui -> {
                ui.add(downloadLink);
                ui.getPage().executeJs(
                        "setTimeout(() => document.getElementById('csv-download-link').click(), 100);" +
                                "setTimeout(() => { const link = document.getElementById('csv-download-link'); if(link) link.remove(); }, 1000);"
                );
            });

            Notification.show("Export CSV réussi : " + reservations.size() + " réservations",
                            3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception e) {
            Notification.show("Erreur lors de l'export CSV: " + e.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void exportToPDF() {
        try {
            // Récupérer les réservations filtrées
            List<Reservation> reservations = getFilteredReservations();

            // Calculer les statistiques
            int totalPlaces = reservations.stream()
                    .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE ||
                            r.getStatut() == ReservationStatut.EN_ATTENTE)
                    .mapToInt(Reservation::getNombrePlaces)
                    .sum();

            double totalRevenue = reservations.stream()
                    .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                    .mapToDouble(Reservation::getMontantTotal)
                    .sum();

            // ✅ GÉNÉRER LE PDF AVEC LE SERVICE
            byte[] pdfBytes = pdfExportService.generateReservationsPdf(
                    currentEvent,
                    reservations,
                    totalPlaces,
                    totalRevenue
            );

            // ✅ GÉNÉRER LE NOM DE FICHIER
            String fileName = pdfExportService.generateFileName(currentEvent);

            // Créer la ressource de téléchargement
            StreamResource resource = new StreamResource(fileName, () ->
                    new ByteArrayInputStream(pdfBytes)
            );
            resource.setContentType("application/pdf");

            // Créer le lien de téléchargement
            Anchor downloadLink = new Anchor(resource, "");
            downloadLink.getElement().setAttribute("download", true);
            downloadLink.setId("pdf-download-link");
            downloadLink.getStyle().set("display", "none");

            // Déclencher le téléchargement
            getUI().ifPresent(ui -> {
                ui.add(downloadLink);
                ui.getPage().executeJs(
                        "setTimeout(() => document.getElementById('pdf-download-link').click(), 100);" +
                                "setTimeout(() => { const link = document.getElementById('pdf-download-link'); if(link) link.remove(); }, 1000);"
                );
            });

            // Notification de succès
            Notification.show("✅ Export PDF réussi : " + reservations.size() + " réservations",
                            3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception e) {
            Notification.show("❌ Erreur lors de l'export PDF: " + e.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            e.printStackTrace();
        }
    }

    private List<Reservation> getFilteredReservations() {
        List<Reservation> filtered = allReservations;

        ReservationStatut selectedStatus = statusFilter.getValue();
        if (selectedStatus != null) {
            filtered = filtered.stream()
                    .filter(r -> r.getStatut() == selectedStatus)
                    .collect(Collectors.toList());
        }

        String searchText = searchField.getValue();
        // Suite de getFilteredReservations()
        if (searchText != null && !searchText.trim().isEmpty()) {
            String search = searchText.toLowerCase().trim();
            filtered = filtered.stream()
                    .filter(r -> {
                        User user = r.getUtilisateur();
                        return r.getCodeReservation().toLowerCase().contains(search) ||
                                (user != null && (
                                        user.getNom().toLowerCase().contains(search) ||
                                                user.getPrenom().toLowerCase().contains(search) ||
                                                user.getEmail().toLowerCase().contains(search)
                                ));
                    })
                    .collect(Collectors.toList());
        }

        return filtered;
    }

    private void showReservationDetails(Reservation reservation) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Détails de la réservation");
        dialog.setWidth("500px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        addDetailRow(content, "Code", reservation.getCodeReservation());

        User user = reservation.getUtilisateur();
        if (user != null) {
            addDetailRow(content, "Client", user.getPrenom() + " " + user.getNom());
            addDetailRow(content, "Email", user.getEmail());
            addDetailRow(content, "Téléphone", user.getTelephone() != null ? user.getTelephone() : "N/A");
        }

        addDetailRow(content, "Places", String.valueOf(reservation.getNombrePlaces()));
        addDetailRow(content, "Montant", String.format("%.2f DH", reservation.getMontantTotal()));
        addDetailRow(content, "Date", formatDateTime(reservation.getDateReservation()));

        HorizontalLayout statusRow = new HorizontalLayout();
        statusRow.setSpacing(true);
        statusRow.setAlignItems(FlexComponent.Alignment.CENTER);

        Span statusLabel = new Span("Statut:");
        statusLabel.getStyle().set("font-weight", "600");

        Span statusBadge = new Span(reservation.getStatut().name());
        statusBadge.getStyle()
                .set("background", getReservationStatusColor(reservation.getStatut()))
                .set("color", "white")
                .set("padding", "4px 12px")
                .set("border-radius", "12px")
                .set("font-weight", "600");

        statusRow.add(statusLabel, statusBadge);
        content.add(statusRow);

        Button closeButton = new Button("Fermer", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(content);
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    private void confirmReservation(Reservation reservation) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmer la réservation");
        dialog.setText("Voulez-vous confirmer cette réservation ?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Confirmer");
        dialog.setConfirmButtonTheme("success primary");

        dialog.addConfirmListener(e -> {
            try {
                reservation.setStatut(ReservationStatut.CONFIRMEE);
                reservationService.saveReservation(reservation);
                Notification.show("Réservation confirmée", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                loadReservations();
            } catch (Exception ex) {
                Notification.show("Erreur: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.open();
    }

    private void confirmCancelReservation(Reservation reservation) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Annuler la réservation");
        dialog.setText("Voulez-vous annuler cette réservation ?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Annuler");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                reservation.setStatut(ReservationStatut.ANNULEE);
                reservationService.saveReservation(reservation);
                Notification.show("Réservation annulée", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                loadReservations();
            } catch (Exception ex) {
                Notification.show("Erreur: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

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

        Span valueSpan = new Span(value != null ? value : "N/A");
        valueSpan.getStyle().set("color", "var(--lumo-primary-text-color)");

        row.add(labelSpan, valueSpan);
        container.add(row);
    }

    private String getReservationStatusColor(ReservationStatut statut) {
        return switch (statut) {
            case CONFIRMEE -> "#10b981";
            case EN_ATTENTE -> "#f59e0b";
            case ANNULEE -> "#ef4444";
            default -> "#6b7280";
        };
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateTime.format(formatter);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm");
        return dateTime.format(formatter);
    }

    private void showAccessDenied() {
        removeAll();
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon icon = new Icon(VaadinIcon.BAN);
        icon.setSize("64px");
        icon.setColor("#ef4444");

        H1 title = new H1("Accès refusé");
        title.getStyle().set("color", "#dc2626");

        Button backButton = new Button("Retour", e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/events")));
        backButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(icon, title, backButton);
        add(layout);
    }

    private void showEventNotFound() {
        removeAll();
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon icon = new Icon(VaadinIcon.WARNING);
        icon.setSize("64px");
        icon.setColor("#f59e0b");

        H1 title = new H1("Événement introuvable");
        title.getStyle().set("color", "#d97706");

        Button backButton = new Button("Retour", e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/events")));
        backButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(icon, title, backButton);
        add(layout);
    }

    private User getCurrentUser() {
        return VaadinSession.getCurrent().getAttribute(User.class);
    }

    private boolean isOrganizer(User user) {
        if (user == null || user.getRole() == null) return false;
        String roleName = user.getRole().name();
        return "ORGANIZER".equals(roleName) || "ADMIN".equals(roleName);
    }
}