package org.example.reservation_event.ui.Organizateurs;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
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

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "organizer/events", layout = MainLayout.class)
@PageTitle("Mes Événements - Organisateur")
public class OrganizateurEventsView extends VerticalLayout {

    private final EventService eventService;
    private final ReservationService reservationService;
    private final UserService userService;
    private User currentUser;

    private ComboBox<EventStatut> statusFilter;
    private List<Event> allEvents;
    private Div cardsContainer;
    private Span eventCountSpan;

    @Autowired
    public OrganizateurEventsView(EventService eventService, ReservationService reservationService,
                                  UserService userService) {
        this.eventService = eventService;
        this.reservationService = reservationService;
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("overflow-y", "auto");

        currentUser = getCurrentUser();
        if (currentUser == null || !isOrganizer(currentUser)) {
            showAccessDenied();
            return;
        }

        initializeLayout();
        loadEvents();
    }

    private void initializeLayout() {
        removeAll();
        add(createHeader(), createFiltersSection(), createEventsGrid());
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

        H1 title = new H1("Mes Événements");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Button createButton = new Button("Créer un événement", new Icon(VaadinIcon.PLUS));
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/event/new"))
        );

        header.add(title, createButton);
        header.setFlexGrow(1, title);
        header.setFlexGrow(0, createButton);

        return header;
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

        H3 title = new H3("Filtres");
        title.getStyle()
                .set("margin", "0 0 15px 0")
                .set("color", "var(--lumo-primary-text-color)");

        HorizontalLayout filtersRow = new HorizontalLayout();
        filtersRow.setWidthFull();
        filtersRow.setSpacing(true);

        statusFilter = new ComboBox<>("Statut");
        statusFilter.setItems(EventStatut.values());
        statusFilter.setItemLabelGenerator(EventStatut::getLabel);
        statusFilter.setPlaceholder("Tous les statuts");
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("200px");
        statusFilter.addValueChangeListener(e -> applyFilters());

        Button resetButton = new Button("Réinitialiser", new Icon(VaadinIcon.REFRESH));
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetButton.addClickListener(e -> {
            statusFilter.clear();
            applyFilters();
        });

        filtersRow.add(statusFilter, resetButton);
        section.add(title, filtersRow);

        return section;
    }

    private Component createEventsGrid() {
        VerticalLayout gridSection = new VerticalLayout();
        gridSection.setWidthFull();
        gridSection.setPadding(true);
        gridSection.setSpacing(true);
        gridSection.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        eventCountSpan = new Span("Événements");
        eventCountSpan.getStyle()
                .set("font-size", "18px")
                .set("font-weight", "600")
                .set("color", "var(--lumo-primary-text-color)");

        headerLayout.add(eventCountSpan);

        cardsContainer = new Div();
        cardsContainer.setWidthFull();
        cardsContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(300px, 1fr))")
                .set("gap", "24px")
                .set("margin-top", "10px");

        gridSection.add(headerLayout, cardsContainer);
        return gridSection;
    }

    private void loadEvents() {
        try {
            allEvents = eventService.getOrganizerEvents(currentUser);
            applyFilters();
        } catch (Exception e) {
            Notification.show("Erreur lors du chargement des événements: " + e.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void applyFilters() {
        if (allEvents == null) return;

        List<Event> filtered = allEvents;
        EventStatut selectedStatus = statusFilter != null ? statusFilter.getValue() : null;

        if (selectedStatus != null) {
            filtered = filtered.stream()
                    .filter(event -> event.getStatut() == selectedStatus)
                    .toList();
        }

        eventCountSpan.setText(filtered.size() + " événement(s) trouvé(s)");
        cardsContainer.removeAll();

        if (filtered.isEmpty()) {
            cardsContainer.add(createEmptyState());
        } else {
            for (Event event : filtered) {
                cardsContainer.add(createEventCard(event));
            }
        }
    }

    private Component createEventCard(Event event) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("transition", "transform 0.2s, box-shadow 0.2s")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("overflow", "hidden")
                .set("height", "100%")
                .set("min-height", "360px");

        card.getElement().executeJs(
                "this.addEventListener('mouseenter', () => {" +
                        "  this.style.transform = 'translateY(-4px)';" +
                        "  this.style.boxShadow = '0 8px 16px rgba(0,0,0,0.15)';" +
                        "});" +
                        "this.addEventListener('mouseleave', () => {" +
                        "  this.style.transform = 'translateY(0)';" +
                        "  this.style.boxShadow = '0 2px 8px rgba(0,0,0,0.1)';" +
                        "});"
        );

        // Image section
        Div imageSection = createImageSection(event);

        // Content section
        VerticalLayout contentSection = createContentSection(event);

        card.add(imageSection, contentSection);
        return card;
    }

    private Div createImageSection(Event event) {
        Div imageSection = new Div();
        imageSection.setWidthFull();
        imageSection.setHeight("50%");
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
                    .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("justify-content", "center");

            Icon categoryIcon = getCategoryIcon(event.getCategorie());
            categoryIcon.setSize("48px");
            categoryIcon.setColor("rgba(255, 255, 255, 0.9)");
            imageSection.add(categoryIcon);
        }

        // Badges
        HorizontalLayout badgesRow = new HorizontalLayout();
        badgesRow.setWidthFull();
        badgesRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        badgesRow.setPadding(true);
        badgesRow.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0")
                .set("right", "0")
                .set("z-index", "10");

        Span categoryBadge = new Span(event.getCategorie().getLabel());
        categoryBadge.getStyle()
                .set("background", "rgba(255, 255, 255, 0.95)")
                .set("color", "#1e293b")
                .set("padding", "6px 14px")
                .set("border-radius", "20px")
                .set("font-size", "12px")
                .set("font-weight", "700")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.2)");

        Span statusBadge = new Span(event.getStatut().getLabel());
        statusBadge.getStyle()
                .set("background", getStatusColor(event.getStatut()))
                .set("color", "white")
                .set("padding", "6px 14px")
                .set("border-radius", "20px")
                .set("font-size", "12px")
                .set("font-weight", "700")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.3)");

        badgesRow.add(categoryBadge, statusBadge);
        imageSection.add(badgesRow);

        return imageSection;
    }

    private VerticalLayout createContentSection(Event event) {
        VerticalLayout contentSection = new VerticalLayout();
        contentSection.setWidthFull();
        contentSection.setHeight("50%");
        contentSection.setPadding(true);
        contentSection.setSpacing(false);
        contentSection.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("justify-content", "space-between");

        H3 eventTitle = new H3(event.getTitre());
        eventTitle.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "700")
                .set("font-size", "18px")
                .set("margin", "0 0 12px 0")
                .set("line-height", "1.3")
                .set("display", "-webkit-box")
                .set("-webkit-line-clamp", "2")
                .set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden")
                .set("min-height", "35px");

        HorizontalLayout dateRow = new HorizontalLayout();
        dateRow.setSpacing(true);
        dateRow.setAlignItems(FlexComponent.Alignment.CENTER);
        dateRow.getStyle().set("margin-bottom", "8px");

        Icon dateIcon = new Icon(VaadinIcon.CALENDAR);
        dateIcon.setSize("16px");
        dateIcon.setColor("var(--lumo-secondary-text-color)");

        Span dateText = new Span(formatShortDate(event.getDateDebut()));
        dateText.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "14px")
                .set("font-weight", "500");
        dateRow.add(dateIcon, dateText);

        int reservedPlaces = reservationService.getTotalReservedPlaces(event.getId());
        int totalPlaces = event.getCapaciteMax();

        HorizontalLayout placesRow = new HorizontalLayout();
        placesRow.setSpacing(true);
        placesRow.setAlignItems(FlexComponent.Alignment.CENTER);
        placesRow.getStyle().set("margin-bottom", "12px");

        Icon placesIcon = new Icon(VaadinIcon.USERS);
        placesIcon.setSize("16px");
        placesIcon.setColor("var(--lumo-secondary-text-color)");

        Span placesText = new Span(reservedPlaces + "/" + totalPlaces + " places");
        placesText.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "14px")
                .set("font-weight", "500");
        placesRow.add(placesIcon, placesText);

        HorizontalLayout actionsRow = createActionsRow(event);

        contentSection.add(eventTitle, dateRow, placesRow, actionsRow);
        return contentSection;
    }

    private HorizontalLayout createActionsRow(Event event) {
        HorizontalLayout actionsRow = new HorizontalLayout();
        actionsRow.setWidthFull();
        actionsRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        actionsRow.setSpacing(true);
        actionsRow.getStyle()
                .set("padding-top", "10px")
                .set("border-top", "1px solid var(--lumo-contrast-10pct)");

        Button reservationsBtn = new Button("Réservations", new Icon(VaadinIcon.LIST));
        reservationsBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        reservationsBtn.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/event-reservations/" + event.getId()))
        );

        Button editBtn = new Button("Modifier", new Icon(VaadinIcon.EDIT));
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        editBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("organizer/event/edit/" + event.getId())));

        Button moreBtn = new Button(new Icon(VaadinIcon.ELLIPSIS_DOTS_V));
        moreBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        ContextMenu contextMenu = new ContextMenu(moreBtn);
        contextMenu.setOpenOnClick(true);

        MenuItem detailsItem = contextMenu.addItem("Détails", e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/eventDetail/" + event.getId()))
        );
        detailsItem.addComponentAsFirst(new Icon(VaadinIcon.INFO_CIRCLE));

        if (event.getStatut() == EventStatut.BROUILLON) {
            MenuItem publishItem = contextMenu.addItem("Publier", e -> confirmPublishEvent(event));
            publishItem.addComponentAsFirst(new Icon(VaadinIcon.CHECK));
        }

        actionsRow.add(reservationsBtn, editBtn, moreBtn);
        return actionsRow;
    }

    private Component createEmptyState() {
        VerticalLayout emptyState = new VerticalLayout();
        emptyState.setWidthFull();
        emptyState.setHeight("300px");
        emptyState.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        emptyState.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon emptyIcon = new Icon(VaadinIcon.CALENDAR);
        emptyIcon.setSize("64px");
        emptyIcon.setColor("var(--lumo-secondary-text-color)");

        H3 emptyTitle = new H3("Aucun événement trouvé");
        emptyTitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "20px 0 10px 0");

        Button createButton = new Button("Créer un événement", new Icon(VaadinIcon.PLUS));
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/event/new"))
        );

        emptyState.add(emptyIcon, emptyTitle, createButton);
        return emptyState;
    }

    private void confirmPublishEvent(Event event) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Publier l'événement");
        dialog.setText("Voulez-vous publier \"" + event.getTitre() + "\" ?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Publier");
        dialog.setConfirmButtonTheme("success primary");
        dialog.addConfirmListener(e -> publishEvent(event));
        dialog.open();
    }

    private void publishEvent(Event event) {
        try {
            event.setStatut(EventStatut.PUBLIE);
            eventService.saveEvent(event);
            Notification.show("Événement publié", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            loadEvents();
        } catch (Exception e) {
            Notification.show("Erreur: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private Icon getCategoryIcon(EventCategorie categorie) {
        VaadinIcon iconType = switch (categorie) {
            case CONCERT -> VaadinIcon.MUSIC;
            case CONFERENCE -> VaadinIcon.COMMENTS;
            case THEATRE -> VaadinIcon.GROUP;
            case SPORT -> VaadinIcon.TROPHY;
            default -> VaadinIcon.CALENDAR;
        };
        return new Icon(iconType);
    }

    private String getStatusColor(EventStatut statut) {
        return switch (statut) {
            case PUBLIE -> "#10b981";
            case BROUILLON -> "#f59e0b";
            case ANNULE -> "#ef4444";
            case TERMINE -> "#6b7280";
        };
    }

    private String formatShortDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
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

        H2 title = new H2("Accès refusé");
        title.getStyle().set("color", "#dc2626");

        Button homeBtn = new Button("Retour", e ->
                getUI().ifPresent(ui -> ui.navigate("dashboard")));
        homeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(icon, title, homeBtn);
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