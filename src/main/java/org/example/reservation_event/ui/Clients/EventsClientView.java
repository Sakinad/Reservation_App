package org.example.reservation_event.ui.Clients;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.example.reservation_event.Enums.EventCategorie;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.dtos.EventLightDTO;
import org.example.reservation_event.services.EventService;
import org.example.reservation_event.ui.MainLayout;
import org.example.reservation_event.utils.ImageUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "eventsClient", layout = MainLayout.class)
@PageTitle("Événements Disponibles")
public class EventsClientView extends VerticalLayout {

    private final EventService eventService;

    // Filter fields
    private TextField searchField;
    private ComboBox<EventCategorie> categorieComboBox;
    private TextField villeField;
    private DatePicker dateDebutPicker;
    private DatePicker dateFinPicker;
    private NumberField prixMinField;
    private NumberField prixMaxField;

    // Content area
    private VerticalLayout eventsContainer;

    // Filter values
    private String searchKeyword = null;
    private EventCategorie selectedCategorie = null;
    private String selectedVille = null;
    private LocalDateTime dateDebut = null;
    private LocalDateTime dateFin = null;
    private Double prixMin = null;
    private Double prixMax = null;

    private int currentPage = 0;

    @Autowired
    public EventsClientView(EventService eventService) {
        this.eventService = eventService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        // Initialize layout
        initializeLayout();
    }

    private void initializeLayout() {
        add(
                createHeader(),
                createFiltersSection(),
                createEventsContainer()
        );

        // Load initial events
        loadEvents();
    }

    private Component createHeader() {
        H1 title = new H1("Événements Disponibles");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");
        return title;
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

        H3 filtersTitle = new H3("Filtrer les événements");
        filtersTitle.getStyle()
                .set("margin", "0 0 15px 0")
                .set("color", "var(--lumo-primary-text-color)");

        // First row
        HorizontalLayout firstRow = new HorizontalLayout();
        firstRow.setWidthFull();
        firstRow.setSpacing(true);

        searchField = new TextField("Rechercher");
        searchField.setPlaceholder("Titre de l'événement...");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setWidthFull();

        categorieComboBox = new ComboBox<>("Catégorie");
        categorieComboBox.setItems(EventCategorie.values());
        categorieComboBox.setItemLabelGenerator(EventCategorie::getLabel);
        categorieComboBox.setPlaceholder("Toutes les catégories");
        categorieComboBox.setWidthFull();

        villeField = new TextField("Ville");
        villeField.setPlaceholder("Ex: Casablanca");
        villeField.setPrefixComponent(new Icon(VaadinIcon.MAP_MARKER));
        villeField.setWidthFull();

        firstRow.add(searchField, categorieComboBox, villeField);

        // Second row
        HorizontalLayout secondRow = new HorizontalLayout();
        secondRow.setWidthFull();
        secondRow.setSpacing(true);

        dateDebutPicker = new DatePicker("Date début");
        dateDebutPicker.setPlaceholder("Sélectionnez une date");
        dateDebutPicker.setWidthFull();

        dateFinPicker = new DatePicker("Date fin");
        dateFinPicker.setPlaceholder("Sélectionnez une date");
        dateFinPicker.setWidthFull();

        prixMinField = new NumberField("Prix min (DH)");
        prixMinField.setPlaceholder("0");
        prixMinField.setMin(0);
        prixMinField.setWidthFull();

        prixMaxField = new NumberField("Prix max (DH)");
        prixMaxField.setPlaceholder("1000");
        prixMaxField.setMin(0);
        prixMaxField.setWidthFull();

        secondRow.add(dateDebutPicker, dateFinPicker, prixMinField, prixMaxField);

        // Action buttons
        HorizontalLayout actionButtons = new HorizontalLayout();
        actionButtons.setSpacing(true);

        Button searchButton = new Button("Rechercher", new Icon(VaadinIcon.SEARCH));
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchButton.addClickListener(e -> applyFilters());

        Button resetButton = new Button("Réinitialiser", new Icon(VaadinIcon.REFRESH));
        resetButton.addClickListener(e -> resetFilters());

        actionButtons.add(searchButton, resetButton);

        filtersSection.add(filtersTitle, firstRow, secondRow, actionButtons);
        return filtersSection;
    }

    private Component createEventsContainer() {
        VerticalLayout container = new VerticalLayout();
        container.setWidthFull();
        container.setPadding(false);
        container.setSpacing(true);

        eventsContainer = new VerticalLayout();
        eventsContainer.setWidthFull();
        eventsContainer.setPadding(false);
        eventsContainer.setSpacing(true);

        container.add(eventsContainer);
        return container;
    }

    private Component createEventCard(EventLightDTO event) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidth("280px");
        card.setHeight("560px");
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)")
                .set("transition", "transform 0.2s, box-shadow 0.2s")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("cursor", "pointer")
                .set("overflow", "hidden");

        // Image section
        Div imageWithBadge = new Div();
        imageWithBadge.setHeight("180px");
        imageWithBadge.setWidthFull();
        imageWithBadge.getStyle()
                .set("position", "relative")
                .set("margin-bottom", "16px")
                .set("flex-shrink", "0");

        Div imageContainer = new Div();
        imageContainer.setHeight("180px");
        imageContainer.setWidthFull();
        imageContainer.getStyle()
                .set("border-radius", "8px")
                .set("overflow", "hidden")
                .set("position", "relative")
                .set("background-color", "var(--lumo-contrast-10pct)");

        // Charger l'image avec ImageUtils
        boolean hasImage = false;
        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            try {
                Image image = ImageUtils.createEventImage(event.getImageUrl());
                image.setWidthFull();
                image.setHeight("100%");
                image.getStyle()
                        .set("object-fit", "cover")
                        .set("position", "absolute")
                        .set("top", "0")
                        .set("left", "0");
                imageContainer.add(image);
                hasImage = true;
            } catch (Exception e) {
                System.err.println("Error loading image for event " + event.getId() + ": " + e.getMessage());
            }
        }

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

        // Category badge
        Span categoryBadge = new Span(event.getCategorie() != null ? event.getCategorie().getLabel() : "—");
        categoryBadge.getStyle()
                .set("position", "absolute")
                .set("top", "10px")
                .set("right", "10px")
                .set("background", event.getCategorie() != null ? event.getCategorie().getColor() : "#94a3b8")
                .set("color", "white")
                .set("padding", "6px 14px")
                .set("border-radius", "20px")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("box-shadow", "0 2px 6px rgba(0,0,0,0.3)")
                .set("z-index", "10");

        imageContainer.add(categoryBadge);
        imageWithBadge.add(imageContainer);

        // Title
        H3 title = new H3(event.getTitre());
        title.getStyle()
                .set("margin", "0 0 12px 0")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "17px")
                .set("line-height", "1.4")
                .set("height", "50px")
                .set("overflow", "hidden")
                .set("display", "-webkit-box")
                .set("-webkit-line-clamp", "2")
                .set("-webkit-box-orient", "vertical")
                .set("flex-shrink", "0");

        // Info section
        VerticalLayout infoSection = new VerticalLayout();
        infoSection.setPadding(false);
        infoSection.setSpacing(false);
        infoSection.setHeight("150px");
        infoSection.getStyle()
                .set("gap", "10px")
                .set("margin-bottom", "12px")
                .set("flex-shrink", "0");

        // Date
        HorizontalLayout dateLayout = new HorizontalLayout();
        dateLayout.setSpacing(true);
        dateLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        dateLayout.setHeight("24px");
        Icon dateIcon = new Icon(VaadinIcon.CALENDAR);
        dateIcon.setSize("16px");
        dateIcon.setColor("var(--lumo-secondary-text-color)");
        Span dateText = new Span(formatDate(event.getDateDebut()));
        dateText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "13px")
                .set("line-height", "1");
        dateLayout.add(dateIcon, dateText);

        // Location
        HorizontalLayout locationLayout = new HorizontalLayout();
        locationLayout.setSpacing(true);
        locationLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        locationLayout.setHeight("24px");
        Icon locationIcon = new Icon(VaadinIcon.MAP_MARKER);
        locationIcon.setSize("16px");
        locationIcon.setColor("var(--lumo-secondary-text-color)");
        Span locationText = new Span(event.getVille() != null ? event.getVille() : "—");
        locationText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "13px")
                .set("line-height", "1");
        locationLayout.add(locationIcon, locationText);

        Hr divider = new Hr();
        divider.getStyle()
                .set("margin", "12px 0")
                .set("border", "none")
                .set("border-top", "1px solid var(--lumo-contrast-10pct)")
                .set("width", "100%");

        // Price
        HorizontalLayout priceRow = new HorizontalLayout();
        priceRow.setWidthFull();
        priceRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        priceRow.setAlignItems(FlexComponent.Alignment.CENTER);
        priceRow.setHeight("32px");

        Span priceLabel = new Span("Prix:");
        priceLabel.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "14px");

        Span priceValue = new Span(event.getPrixUnitaire() != null ?
                String.format("%.2f DH", event.getPrixUnitaire()) : "—");
        priceValue.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "700")
                .set("font-size", "18px");

        priceRow.add(priceLabel, priceValue);

        // Places
        Integer placesDisponibles = event.getPlacesDisponibles() != null ?
                event.getPlacesDisponibles() : 0;

        HorizontalLayout placesRow = new HorizontalLayout();
        placesRow.setWidthFull();
        placesRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        placesRow.setAlignItems(FlexComponent.Alignment.CENTER);
        placesRow.setHeight("28px");

        Span placesLabel = new Span("Places:");
        placesLabel.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "14px");

        Span placesBadge = new Span(String.valueOf(placesDisponibles));
        String badgeColor = placesDisponibles > 10 ? "#059669" :
                placesDisponibles > 0 ? "#d97706" : "#dc2626";
        String badgeBackground = placesDisponibles > 10 ? "#d1fae5" :
                placesDisponibles > 0 ? "#fef3c7" : "#fee2e2";

        placesBadge.getStyle()
                .set("background", badgeBackground)
                .set("color", badgeColor)
                .set("padding", "4px 10px")
                .set("border-radius", "12px")
                .set("font-weight", "600")
                .set("font-size", "13px");

        placesRow.add(placesLabel, placesBadge);
        infoSection.add(dateLayout, locationLayout, divider, priceRow, placesRow);

        // Buttons
        VerticalLayout buttonsSection = new VerticalLayout();
        buttonsSection.setWidthFull();
        buttonsSection.setPadding(false);
        buttonsSection.setSpacing(false);
        buttonsSection.setHeight("96px");
        buttonsSection.getStyle()
                .set("margin-top", "auto")
                .set("gap", "8px")
                .set("flex-shrink", "0");

        Long eventId = event.getId();
        EventStatut statut = event.getStatut();

        String buttonText;
        VaadinIcon buttonIcon;
        boolean isEnabled = false;
        ButtonVariant buttonVariant;

        switch (statut) {
            case BROUILLON:
                buttonText = "Brouillon";
                buttonIcon = VaadinIcon.EDIT;
                buttonVariant = ButtonVariant.LUMO_TERTIARY;
                break;

            case PUBLIE:
                boolean isReservable = event.getReservable() != null ? event.getReservable() : false;
                if (isReservable && placesDisponibles > 0) {
                    buttonText = "Réserver maintenant";
                    buttonIcon = VaadinIcon.TICKET;
                    buttonVariant = ButtonVariant.LUMO_PRIMARY;
                    isEnabled = true;
                } else {
                    buttonText = "Publié";
                    buttonIcon = VaadinIcon.CHECK;
                    buttonVariant = ButtonVariant.LUMO_CONTRAST;
                }
                break;

            case ANNULE:
                buttonText = "Annulé";
                buttonIcon = VaadinIcon.CLOSE_CIRCLE;
                buttonVariant = ButtonVariant.LUMO_ERROR;
                break;

            case TERMINE:
                buttonText = "Terminé";
                buttonIcon = VaadinIcon.CHECK_CIRCLE;
                buttonVariant = ButtonVariant.LUMO_CONTRAST;
                break;

            default:
                buttonText = "Inconnu";
                buttonIcon = VaadinIcon.QUESTION;
                buttonVariant = ButtonVariant.LUMO_TERTIARY;
        }

        Button statusButton = new Button(buttonText, buttonIcon.create());
        statusButton.addThemeVariants(buttonVariant);
        statusButton.setWidthFull();
        statusButton.setEnabled(isEnabled);
        statusButton.setHeight("44px");
        statusButton.getStyle()
                .set("font-weight", "600")
                .set("flex-shrink", "0");

        if (isEnabled) {
            statusButton.addClickListener(e ->
                    getUI().ifPresent(ui -> ui.navigate("eventClient/" + eventId + "/reserve"))
            );
        }

        Button detailsButton = new Button("Voir détails", new Icon(VaadinIcon.INFO_CIRCLE));
        detailsButton.setWidthFull();
        detailsButton.setHeight("44px");
        detailsButton.getStyle()
                .set("background", "transparent")
                .set("box-shadow", "none")
                .set("border", "1px solid var(--lumo-contrast-30pct)")
                .set("color", "var(--lumo-primary-text-color)")
                .set("flex-shrink", "0");

        detailsButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("eventClient/" + eventId))
        );

        buttonsSection.add(statusButton, detailsButton);
        card.add(imageWithBadge, title, infoSection, buttonsSection);

        // Effet hover avec des listeners d'événements DOM
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle()
                    .set("transform", "translateY(-4px)")
                    .set("box-shadow", "0 8px 16px rgba(0,0,0,0.15)");
        });

        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle()
                    .set("transform", "translateY(0)")
                    .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");
        });

        return card;
    }

    private Component createEventsCardsLayout(List<EventLightDTO> events) {
        int pageSize = 12;
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, events.size());
        List<EventLightDTO> pageEvents = events.subList(start, end);

        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setWidthFull();
        mainContainer.setPadding(false);
        mainContainer.setSpacing(true);
        mainContainer.getStyle().set("gap", "20px");

        HorizontalLayout currentRow = null;
        int cardCount = 0;

        for (EventLightDTO event : pageEvents) {
            if (cardCount % 4 == 0) {
                currentRow = new HorizontalLayout();
                currentRow.setWidthFull();
                currentRow.setSpacing(true);
                currentRow.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
                currentRow.getStyle().set("gap", "20px");
                mainContainer.add(currentRow);
            }

            currentRow.add(createEventCard(event));
            cardCount++;
        }

        return mainContainer;
    }

    private Component createPaginationControls(int totalEvents) {
        int pageSize = 12;
        int totalPages = (int) Math.ceil((double) totalEvents / pageSize);

        if (totalPages <= 1) {
            return new Div();
        }

        HorizontalLayout pagination = new HorizontalLayout();
        pagination.setWidthFull();
        pagination.setPadding(true);
        pagination.setSpacing(true);
        pagination.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        pagination.setAlignItems(FlexComponent.Alignment.CENTER);
        pagination.getStyle()
                .set("margin-top", "30px")
                .set("padding", "20px")
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        Button prevButton = new Button("Précédent", new Icon(VaadinIcon.ANGLE_LEFT));
        prevButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        prevButton.setEnabled(currentPage > 0);
        prevButton.addClickListener(e -> {
            currentPage--;
            loadEvents();
        });

        HorizontalLayout pageNumbers = new HorizontalLayout();
        pageNumbers.setSpacing(true);

        int startPage = Math.max(0, currentPage - 3);
        int endPage = Math.min(totalPages, startPage + 7);

        if (endPage - startPage < 7) {
            startPage = Math.max(0, endPage - 7);
        }

        if (startPage > 0) {
            Button firstButton = new Button("1");
            firstButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            firstButton.addClickListener(e -> {
                currentPage = 0;
                loadEvents();
            });
            pageNumbers.add(firstButton);

            if (startPage > 1) {
                Span dots = new Span("...");
                dots.getStyle().set("padding", "0 10px");
                pageNumbers.add(dots);
            }
        }

        for (int i = startPage; i < endPage; i++) {
            int pageIndex = i;
            Button pageButton = new Button(String.valueOf(i + 1));

            if (i == currentPage) {
                pageButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            } else {
                pageButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            }

            pageButton.addClickListener(e -> {
                currentPage = pageIndex;
                loadEvents();
            });

            pageNumbers.add(pageButton);
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                Span dots = new Span("...");
                dots.getStyle().set("padding", "0 10px");
                pageNumbers.add(dots);
            }

            Button lastButton = new Button(String.valueOf(totalPages));
            lastButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            lastButton.addClickListener(e -> {
                currentPage = totalPages - 1;
                loadEvents();
            });
            pageNumbers.add(lastButton);
        }

        Button nextButton = new Button("Suivant", new Icon(VaadinIcon.ANGLE_RIGHT));
        nextButton.setIconAfterText(true);
        nextButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        nextButton.setEnabled(currentPage < totalPages - 1);
        nextButton.addClickListener(e -> {
            currentPage++;
            loadEvents();
        });

        Span pageInfo = new Span(String.format("Page %d sur %d", currentPage + 1, totalPages));
        pageInfo.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "0 20px")
                .set("font-weight", "500");

        pagination.add(prevButton, pageNumbers, pageInfo, nextButton);
        return pagination;
    }

    private void loadEvents() {
        try {
            List<EventLightDTO> events = eventService.getAllClientEvents(
                    searchKeyword,
                    selectedVille,
                    dateDebut,
                    dateFin,
                    prixMin,
                    prixMax
            );

            if (selectedCategorie != null) {
                events = events.stream()
                        .filter(e -> selectedCategorie.equals(e.getCategorie()))
                        .collect(Collectors.toList());
            }

            eventsContainer.removeAll();

            if (events.isEmpty()) {
                eventsContainer.add(createEmptyState());
            } else {
                HorizontalLayout headerInfo = new HorizontalLayout();
                headerInfo.setWidthFull();
                headerInfo.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                headerInfo.setAlignItems(FlexComponent.Alignment.CENTER);
                headerInfo.getStyle().set("margin-bottom", "20px");

                Span resultCount = new Span(events.size() + " événement(s) trouvé(s)");
                resultCount.getStyle()
                        .set("font-size", "18px")
                        .set("font-weight", "600")
                        .set("color", "var(--lumo-primary-text-color)");

                headerInfo.add(resultCount);
                eventsContainer.add(headerInfo);

                eventsContainer.add(createEventsCardsLayout(events));
                eventsContainer.add(createPaginationControls(events.size()));
            }

        } catch (Exception e) {
            System.out.println("Erreur chargement événements: " + e.getMessage());
            e.printStackTrace();
            Notification.show("Erreur lors du chargement des événements: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE);
        }
    }

    private void applyFilters() {
        currentPage = 0;
        searchKeyword = (searchField.getValue() == null || searchField.getValue().isEmpty()) ? null : searchField.getValue();
        selectedCategorie = categorieComboBox.getValue();
        selectedVille = (villeField.getValue() == null || villeField.getValue().isEmpty()) ? null : villeField.getValue();

        dateDebut = dateDebutPicker.getValue() != null ? dateDebutPicker.getValue().atStartOfDay() : null;
        dateFin = dateFinPicker.getValue() != null ? dateFinPicker.getValue().atTime(23, 59, 59) : null;

        prixMin = prixMinField.getValue();
        prixMax = prixMaxField.getValue();

        loadEvents();
    }

    private void resetFilters() {
        currentPage = 0;
        searchField.clear();
        categorieComboBox.clear();
        villeField.clear();
        dateDebutPicker.clear();
        dateFinPicker.clear();
        prixMinField.clear();
        prixMaxField.clear();

        searchKeyword = null;
        selectedCategorie = null;
        selectedVille = null;
        dateDebut = null;
        dateFin = null;
        prixMin = null;
        prixMax = null;

        loadEvents();
    }

    private Component createEmptyState() {
        VerticalLayout emptyState = new VerticalLayout();
        emptyState.setSizeFull();
        emptyState.setAlignItems(FlexComponent.Alignment.CENTER);
        emptyState.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        emptyState.getStyle().set("padding", "60px");

        Icon emptyIcon = new Icon(VaadinIcon.SEARCH);
        emptyIcon.setSize("64px");
        emptyIcon.setColor("var(--lumo-contrast-40pct)");

        H3 emptyTitle = new H3("Aucun événement trouvé");
        emptyTitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)");

        Span emptyText = new Span("Essayez de modifier vos critères de recherche");
        emptyText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)");
        Button resetFiltersButton = new Button("Réinitialiser les filtres", new Icon(VaadinIcon.REFRESH));
        resetFiltersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        resetFiltersButton.addClickListener(e -> resetFilters());

        emptyState.add(emptyIcon, emptyTitle, emptyText, resetFiltersButton);
        return emptyState;
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "Date non spécifiée";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateTime.format(formatter);
    }
}