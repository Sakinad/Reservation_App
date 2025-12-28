package org.example.reservation_event.ui.Admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.example.reservation_event.Enums.ReservationStatut;
import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.repositories.ReservationRepository;
import org.example.reservation_event.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AllReservationsView - Vue complète de toutes les réservations
 * Version améliorée avec support Dark Mode et affichage horizontal
 *
 * ✅ CORRECTION : Utilisation de JOIN FETCH pour éviter LazyInitializationException
 */
@Route(value = "admin/reservations", layout = MainLayout.class)
@PageTitle("Toutes les Réservations | Event Manager")
@RolesAllowed("ADMIN")
public class AllReservationsView extends VerticalLayout {

    private final ReservationRepository reservationRepository;

    // Composants UI
    private Grid<Reservation> grid;
    private TextField searchField;
    private ComboBox<ReservationStatut> statusFilter;
    private DatePicker dateDebutFilter;
    private DatePicker dateFinFilter;
    private Div statsContainer;

    // Filtres actuels
    private String currentSearch = "";
    private ReservationStatut currentStatusFilter = null;
    private LocalDateTime currentDateDebutFilter = null;
    private LocalDateTime currentDateFinFilter = null;

    @Autowired
    public AllReservationsView(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createStats();
        createFilters();
        createGrid();

        loadReservations();
    }

    private void createHeader() {
        H1 title = new H1("🎫 Toutes les Réservations");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "700");

        Span subtitle = new Span("Vue complète de toutes les réservations de la plateforme");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "14px");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setSpacing(false);
        header.setPadding(false);

        add(header);
    }

    private void createStats() {
        // ✅ Disposition HORIZONTALE avec flex
        statsContainer = new Div();
        statsContainer.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "20px")
                .set("margin-top", "20px")
                .set("justify-content", "space-between");

        add(statsContainer);
    }

    private void createFilters() {
        searchField = new TextField();
        searchField.setPlaceholder("Rechercher par code, utilisateur ou événement...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidth("350px");
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            loadReservations();
        });

        statusFilter = new ComboBox<>("Statut");
        statusFilter.setItems(ReservationStatut.values());
        statusFilter.setItemLabelGenerator(ReservationStatut::getLabel);
        statusFilter.setPlaceholder("Tous les statuts");
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("200px");
        statusFilter.addValueChangeListener(e -> {
            currentStatusFilter = e.getValue();
            loadReservations();
        });

        dateDebutFilter = new DatePicker("Date début");
        dateDebutFilter.setPlaceholder("À partir de...");
        dateDebutFilter.setClearButtonVisible(true);
        dateDebutFilter.setWidth("180px");
        dateDebutFilter.addValueChangeListener(e -> {
            LocalDate date = e.getValue();
            currentDateDebutFilter = date != null ? date.atStartOfDay() : null;
            loadReservations();
        });

        dateFinFilter = new DatePicker("Date fin");
        dateFinFilter.setPlaceholder("Jusqu'à...");
        dateFinFilter.setClearButtonVisible(true);
        dateFinFilter.setWidth("180px");
        dateFinFilter.addValueChangeListener(e -> {
            LocalDate date = e.getValue();
            currentDateFinFilter = date != null ? date.atTime(LocalTime.MAX) : null;
            loadReservations();
        });

        Button exportButton = new Button("Exporter CSV", VaadinIcon.DOWNLOAD.create());
        exportButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        exportButton.addClickListener(e -> exportToCSV());

        Button resetButton = new Button("Réinitialiser", VaadinIcon.REFRESH.create());
        resetButton.addClickListener(e -> resetFilters());

        HorizontalLayout filters = new HorizontalLayout(
                searchField, statusFilter, dateDebutFilter, dateFinFilter,
                exportButton, resetButton
        );
        filters.setAlignItems(Alignment.END);
        filters.getStyle().set("margin-top", "20px");

        add(filters);
    }

    private void createGrid() {
        grid = new Grid<>(Reservation.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("550px");

        // Colonne Code
        grid.addColumn(Reservation::getCodeReservation)
                .setHeader("Code")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Utilisateur
        grid.addColumn(reservation -> {
                    try {
                        return reservation.getUtilisateur().getNomComplet();
                    } catch (Exception e) {
                        return "N/A";
                    }
                })
                .setHeader("Utilisateur")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Email
        grid.addColumn(reservation -> {
                    try {
                        return reservation.getUtilisateur().getEmail();
                    } catch (Exception e) {
                        return "N/A";
                    }
                })
                .setHeader("Email")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Événement
        grid.addColumn(reservation -> {
                    try {
                        return reservation.getEvenement().getTitre();
                    } catch (Exception e) {
                        return "N/A";
                    }
                })
                .setHeader("Événement")
                .setSortable(true)
                .setAutoWidth(true)
                .setFlexGrow(1);

        // Colonne Date réservation
        grid.addColumn(reservation -> reservation.getDateReservation()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setHeader("Date Réservation")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Places
        grid.addColumn(Reservation::getNombrePlaces)
                .setHeader("Places")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Montant
        grid.addColumn(reservation -> String.format("%.2f DH", reservation.getMontantTotal()))
                .setHeader("Montant")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Statut avec badge
        grid.addColumn(new ComponentRenderer<>(reservation -> {
                    Span badge = new Span(reservation.getStatut().getLabel());
                    badge.getElement().getThemeList().add("badge");

                    String theme = switch (reservation.getStatut()) {
                        case EN_ATTENTE -> "contrast";
                        case CONFIRMEE -> "success";
                        case ANNULEE -> "error";
                    };

                    badge.getElement().getThemeList().add(theme);
                    return badge;
                }))
                .setHeader("Statut")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Actions
        grid.addColumn(new ComponentRenderer<>(this::createActionsLayout))
                .setHeader("Actions")
                .setAutoWidth(true)
                .setFlexGrow(0);

        add(grid);
    }

    private Component createActionsLayout(Reservation reservation) {
        Button viewButton = new Button(VaadinIcon.EYE.create());
        viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        viewButton.getElement().setAttribute("title", "Voir les détails");
        viewButton.addClickListener(e -> showReservationDetails(reservation));

        return new HorizontalLayout(viewButton);
    }

    /**
     * ✅ CORRECTION PRINCIPALE : Utilisation de la méthode avec JOIN FETCH
     */
    private void loadReservations() {
        try {
            // 🔥 UTILISER LA NOUVELLE MÉTHODE REPOSITORY AVEC JOIN FETCH
            List<Reservation> allReservations = loadAllReservationsWithFetch();

            // Appliquer les filtres
            List<Reservation> filtered = allReservations.stream()
                    .filter(r -> currentStatusFilter == null || r.getStatut() == currentStatusFilter)
                    .filter(r -> currentDateDebutFilter == null ||
                            !r.getDateReservation().isBefore(currentDateDebutFilter))
                    .filter(r -> currentDateFinFilter == null ||
                            !r.getDateReservation().isAfter(currentDateFinFilter))
                    .filter(r -> {
                        if (currentSearch == null || currentSearch.isEmpty()) return true;
                        String search = currentSearch.toLowerCase();
                        try {
                            return r.getCodeReservation().toLowerCase().contains(search) ||
                                    r.getUtilisateur().getNomComplet().toLowerCase().contains(search) ||
                                    r.getUtilisateur().getEmail().toLowerCase().contains(search) ||
                                    r.getEvenement().getTitre().toLowerCase().contains(search);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            grid.setItems(filtered);
            updateStats(filtered);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
            showErrorNotification("Erreur lors du chargement: " + e.getMessage());
        }
    }

    private List<Reservation> loadAllReservationsWithFetch() {
        try {
            // ✅ UTILISER LA NOUVELLE MÉTHODE AVEC JOIN FETCH
            List<Reservation> reservations = reservationRepository.findAllWithUserAndEvent();
            return reservations;
        } catch (Exception e) {
            System.err.println("❌ Erreur dans loadAllReservationsWithFetch: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Impossible de charger les réservations", e);
        }
    }

    private void updateStats(List<Reservation> reservations) {
        statsContainer.removeAll();

        long total = reservations.size();
        long confirmees = reservations.stream()
                .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                .count();

        int totalPlaces = reservations.stream()
                .filter(r -> r.getStatut() != ReservationStatut.ANNULEE)
                .mapToInt(Reservation::getNombrePlaces)
                .sum();

        double totalRevenue = reservations.stream()
                .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                .mapToDouble(Reservation::getMontantTotal)
                .sum();

        // ✅ Cartes avec icônes et style amélioré
        statsContainer.add(createStatCardWithIcon("Total", String.valueOf(total), "#2196F3", "🎫"));
        statsContainer.add(createStatCardWithIcon("Confirmées", String.valueOf(confirmees), "#4CAF50", "✅"));
        statsContainer.add(createStatCardWithIcon("Places", String.valueOf(totalPlaces), "#FF9800", "👥"));
        statsContainer.add(createStatCardWithIcon("Revenus",
                String.format("%.2f DH", totalRevenue), "#9C27B0", "💰"));
    }

    private Div createStatCardWithIcon(String title, String value, String color, String emoji) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("padding", "20px 24px")
                .set("box-shadow", "0 4px 12px var(--lumo-shade-10pct)")
                .set("border-left", "4px solid " + color)
                .set("flex", "1")
                .set("min-width", "180px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "16px")
                .set("transition", "transform 0.2s, box-shadow 0.2s");

        // Hover effect
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle()
                    .set("transform", "translateY(-4px)")
                    .set("box-shadow", "0 6px 20px var(--lumo-shade-20pct)");
        });

        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle()
                    .set("transform", "translateY(0)")
                    .set("box-shadow", "0 4px 12px var(--lumo-shade-10pct)");
        });

        // Icône emoji
        Span iconSpan = new Span(emoji);
        iconSpan.getStyle()
                .set("font-size", "40px")
                .set("line-height", "1");

        // Conteneur texte
        Div textContainer = new Div();
        textContainer.getStyle().set("flex", "1");

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-size", "13px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("display", "block")
                .set("font-weight", "500")
                .set("margin-bottom", "4px");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "700")
                .set("color", color)
                .set("display", "block")
                .set("letter-spacing", "-0.5px");

        textContainer.add(titleSpan, valueSpan);
        card.add(iconSpan, textContainer);

        return card;
    }

    private void showReservationDetails(Reservation reservation) {
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
        dialog.setHeaderTitle("Détails de la réservation");
        dialog.setWidth("600px");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);

        try {
            // Informations réservation
            content.add(createDetailSection("Informations de réservation",
                    "Code: " + reservation.getCodeReservation(),
                    "Date: " + reservation.getDateReservation()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    "Places: " + reservation.getNombrePlaces(),
                    "Montant: " + String.format("%.2f DH", reservation.getMontantTotal()),
                    "Statut: " + reservation.getStatut().getLabel()
            ));

            // Informations utilisateur
            if (reservation.getUtilisateur() != null) {
                content.add(createDetailSection("Utilisateur",
                        "Nom: " + reservation.getUtilisateur().getNomComplet(),
                        "Email: " + reservation.getUtilisateur().getEmail(),
                        "Téléphone: " + (reservation.getUtilisateur().getTelephone() != null ?
                                reservation.getUtilisateur().getTelephone() : "Non renseigné")
                ));
            }

            // Informations événement
            if (reservation.getEvenement() != null) {
                content.add(createDetailSection("Événement",
                        "Titre: " + reservation.getEvenement().getTitre(),
                        "Date: " + reservation.getEvenement().getDateDebut()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        "Lieu: " + reservation.getEvenement().getLieu() + ", " +
                                reservation.getEvenement().getVille()
                ));
            }

            if (reservation.getCommentaire() != null && !reservation.getCommentaire().isEmpty()) {
                content.add(createDetailSection("Commentaire",
                        reservation.getCommentaire()
                ));
            }
        } catch (Exception e) {
            content.add(new Span("Erreur lors du chargement des détails: " + e.getMessage()));
        }

        dialog.add(content);

        Button closeButton = new Button("Fermer", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeButton);

        dialog.open();
    }

    private Div createDetailSection(String title, String... lines) {
        Div section = new Div();
        section.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "15px")
                .set("border-radius", "8px")
                .set("margin-bottom", "10px");

        H3 sectionTitle = new H3(title);
        sectionTitle.getStyle()
                .set("margin", "0 0 10px 0")
                .set("font-size", "16px")
                .set("color", "var(--lumo-primary-color)");

        section.add(sectionTitle);

        for (String line : lines) {
            Span lineSpan = new Span(line);
            lineSpan.getStyle()
                    .set("display", "block")
                    .set("margin-bottom", "5px")
                    .set("color", "var(--lumo-primary-text-color)");
            section.add(lineSpan);
        }

        return section;
    }

    private void resetFilters() {
        searchField.clear();
        statusFilter.clear();
        dateDebutFilter.clear();
        dateFinFilter.clear();

        currentSearch = "";
        currentStatusFilter = null;
        currentDateDebutFilter = null;
        currentDateFinFilter = null;

        loadReservations();
    }

    /**
     * Export des réservations en CSV
     */
    private void exportToCSV() {
        try {
            List<Reservation> reservations = grid.getGenericDataView()
                    .getItems()
                    .collect(Collectors.toList());

            if (reservations.isEmpty()) {
                showErrorNotification("Aucune réservation à exporter");
                return;
            }

            // Générer le CSV
            StringBuilder csv = new StringBuilder();
            csv.append("Code,Utilisateur,Email,Événement,Date Réservation,Places,Montant,Statut\n");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Reservation r : reservations) {
                try {
                    csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%.2f,\"%s\"\n",
                            r.getCodeReservation(),
                            r.getUtilisateur().getNomComplet(),
                            r.getUtilisateur().getEmail(),
                            r.getEvenement().getTitre(),
                            r.getDateReservation().format(dateFormatter),
                            r.getNombrePlaces(),
                            r.getMontantTotal(),
                            r.getStatut().getLabel()
                    ));
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'export d'une ligne: " + e.getMessage());
                }
            }

            // Créer un nom de fichier avec timestamp
            String filename = "reservations_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

            // Télécharger le fichier côté client
            String csvContent = csv.toString();
            String jsCode = String.format(
                    "var blob = new Blob([atob('%s')], {type: 'text/csv;charset=utf-8;'});" +
                            "var link = document.createElement('a');" +
                            "link.href = URL.createObjectURL(blob);" +
                            "link.download = '%s';" +
                            "link.click();",
                    java.util.Base64.getEncoder().encodeToString(csvContent.getBytes(StandardCharsets.UTF_8)),
                    filename
            );

            getUI().ifPresent(ui -> ui.getPage().executeJs(jsCode));

            showSuccessNotification(reservations.size() + " réservations exportées avec succès");

        } catch (Exception e) {
            showErrorNotification("Erreur lors de l'export: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showSuccessNotification(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}