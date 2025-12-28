package org.example.reservation_event.ui.Publiques;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.example.reservation_event.Enums.EventCategorie;
import org.example.reservation_event.dtos.EventLightDTO;
import org.example.reservation_event.services.EventService;
import org.example.reservation_event.ui.MainLayout;
import org.example.reservation_event.utils.ImageUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "events", layout = MainLayout.class)
@AnonymousAllowed
@PageTitle("Liste des Événements - EventBooking")
public class EventListView extends VerticalLayout {

    private final EventService eventService;

    private TextField searchField;
    private ComboBox<EventCategorie> categorieComboBox;
    private DatePicker dateDebutPicker;
    private DatePicker dateFinPicker;
    private TextField villeField;
    private NumberField prixMinField;
    private NumberField prixMaxField;
    private ComboBox<String> sortComboBox;
    private Button filterButton;
    private Button resetButton;

    private List<EventLightDTO> allEvents;
    private List<EventLightDTO> filteredEvents;

    private int currentPage = 0;
    private int pageSize = 12;
    private HorizontalLayout paginationLayout;

    @Autowired
    public EventListView(EventService eventService) {
        this.eventService = eventService;
        this.allEvents = new ArrayList<>();
        this.filteredEvents = new ArrayList<>();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        createFilterSection();
        createEventGridSection();
        createPaginationSection();

        loadEvents();
    }

    private void createFilterSection() {
        VerticalLayout filterSection = new VerticalLayout();
        filterSection.setWidthFull();
        filterSection.setPadding(true);
        filterSection.setSpacing(true);
        filterSection.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("margin-bottom", "20px");

        H2 filterTitle = new H2("Filtres Avancés");
        filterTitle.getStyle()
                .set("margin", "0 0 15px 0")
                .set("color", "var(--lumo-primary-text-color)");

        // Première ligne
        HorizontalLayout firstRow = new HorizontalLayout();
        firstRow.setWidthFull();
        firstRow.setSpacing(true);

        searchField = new TextField("Recherche");
        searchField.setPlaceholder("Titre, description...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidth("300px");
        searchField.setClearButtonVisible(true);

        categorieComboBox = new ComboBox<>("Catégorie");
        categorieComboBox.setItems(EventCategorie.values());
        categorieComboBox.setItemLabelGenerator(EventCategorie::getLabel);
        categorieComboBox.setPlaceholder("Toutes");
        categorieComboBox.setClearButtonVisible(true);
        categorieComboBox.setWidth("200px");

        villeField = new TextField("Ville");
        villeField.setPlaceholder("Ex: Casablanca");
        villeField.setPrefixComponent(VaadinIcon.MAP_MARKER.create());
        villeField.setClearButtonVisible(true);
        villeField.setWidth("200px");

        firstRow.add(searchField, categorieComboBox, villeField);

        // Deuxième ligne
        HorizontalLayout secondRow = new HorizontalLayout();
        secondRow.setWidthFull();
        secondRow.setSpacing(true);

        dateDebutPicker = new DatePicker("Date de début");
        dateDebutPicker.setPlaceholder("Sélectionner");
        dateDebutPicker.setLocale(new Locale("fr", "FR"));
        dateDebutPicker.setClearButtonVisible(true);
        dateDebutPicker.setWidth("180px");

        dateFinPicker = new DatePicker("Date de fin");
        dateFinPicker.setPlaceholder("Sélectionner");
        dateFinPicker.setLocale(new Locale("fr", "FR"));
        dateFinPicker.setClearButtonVisible(true);
        dateFinPicker.setWidth("180px");

        prixMinField = new NumberField("Prix min (DH)");
        prixMinField.setPlaceholder("0");
        prixMinField.setMin(0);
        prixMinField.setWidth("150px");
        prixMinField.setClearButtonVisible(true);

        prixMaxField = new NumberField("Prix max (DH)");
        prixMaxField.setPlaceholder("1000");
        prixMaxField.setMin(0);
        prixMaxField.setWidth("150px");
        prixMaxField.setClearButtonVisible(true);

        sortComboBox = new ComboBox<>("Trier par");
        sortComboBox.setItems("Date (croissant)", "Date (décroissant)",
                "Prix (croissant)", "Prix (décroissant)", "Popularité");
        sortComboBox.setValue("Date (croissant)");
        sortComboBox.setWidth("180px");

        secondRow.add(dateDebutPicker, dateFinPicker, prixMinField, prixMaxField, sortComboBox);

        // Boutons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        filterButton = new Button("Appliquer", VaadinIcon.FILTER.create());
        filterButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        filterButton.addClickListener(e -> applyFilters());

        resetButton = new Button("Réinitialiser", VaadinIcon.REFRESH.create());
        resetButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        resetButton.addClickListener(e -> resetFilters());

        buttonLayout.add(filterButton, resetButton);

        filterSection.add(filterTitle, firstRow, secondRow, buttonLayout);
        add(filterSection);
    }

    private void createEventGridSection() {
        VerticalLayout gridSection = new VerticalLayout();
        gridSection.setSizeFull();
        gridSection.setPadding(true);
        gridSection.setSpacing(true);
        gridSection.setId("cards-container");

        add(gridSection);
    }

    private void createPaginationSection() {
        paginationLayout = new HorizontalLayout();
        paginationLayout.setWidthFull();
        paginationLayout.setPadding(true);
        paginationLayout.setSpacing(true);
        paginationLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        paginationLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        paginationLayout.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-top", "1px solid var(--lumo-contrast-10pct)");

        add(paginationLayout);
    }

    private void loadEvents() {
        try {
            allEvents = eventService.getAllClientEvents(null, null, null, null, null, null);
            filteredEvents = new ArrayList<>(allEvents);
            currentPage = 0;
            displayEvents();
        } catch (Exception e) {
            showError("Erreur lors du chargement: " + e.getMessage());
        }
    }

    private void applyFilters() {
        String keyword = searchField.getValue();
        EventCategorie categorie = categorieComboBox.getValue();
        LocalDate dateDebut = dateDebutPicker.getValue();
        LocalDate dateFin = dateFinPicker.getValue();
        String ville = villeField.getValue();
        Double prixMin = prixMinField.getValue();
        Double prixMax = prixMaxField.getValue();
        String sortBy = sortComboBox.getValue();

        LocalDateTime startDateTime = dateDebut != null ? dateDebut.atStartOfDay() : null;
        LocalDateTime endDateTime = dateFin != null ? dateFin.atTime(23, 59, 59) : null;

        try {
            filteredEvents = eventService.getAllClientEvents(
                    keyword, ville, startDateTime, endDateTime,
                    prixMin, prixMax
            );

            if (categorie != null) {
                filteredEvents = filteredEvents.stream()
                        .filter(e -> e.getCategorie() == categorie)
                        .collect(Collectors.toList());
            }

            applySorting(sortBy);
            currentPage = 0;
            displayEvents();

        } catch (Exception e) {
            showError("Erreur lors de l'application des filtres: " + e.getMessage());
        }
    }

    private void applySorting(String sortBy) {
        if (sortBy == null) return;

        switch (sortBy) {
            case "Date (croissant)":
                filteredEvents.sort(Comparator.comparing(EventLightDTO::getDateDebut));
                break;
            case "Date (décroissant)":
                filteredEvents.sort(Comparator.comparing(EventLightDTO::getDateDebut).reversed());
                break;
            case "Prix (croissant)":
                filteredEvents.sort(Comparator.comparing(EventLightDTO::getPrixUnitaire));
                break;
            case "Prix (décroissant)":
                filteredEvents.sort(Comparator.comparing(EventLightDTO::getPrixUnitaire).reversed());
                break;
            case "Popularité":
                filteredEvents.sort((e1, e2) ->
                        Integer.compare(e2.getPlacesDisponibles(), e1.getPlacesDisponibles()));
                break;
        }
    }

    private void resetFilters() {
        searchField.clear();
        categorieComboBox.clear();
        dateDebutPicker.clear();
        dateFinPicker.clear();
        villeField.clear();
        prixMinField.clear();
        prixMaxField.clear();
        sortComboBox.setValue("Date (croissant)");
        loadEvents();
    }

    private void displayEvents() {
        VerticalLayout cardsContainer = (VerticalLayout) getComponentAt(1);
        cardsContainer.removeAll();

        HorizontalLayout resultHeader = new HorizontalLayout();
        resultHeader.setWidthFull();
        resultHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        resultHeader.setAlignItems(FlexComponent.Alignment.CENTER);

        Span resultCount = new Span(filteredEvents.size() + " événement(s) trouvé(s)");
        resultCount.getStyle()
                .set("font-size", "1.1em")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-color)");

        resultHeader.add(resultCount);
        cardsContainer.add(resultHeader);

        if (filteredEvents.isEmpty()) {
            Paragraph noResults = new Paragraph("Aucun événement ne correspond à vos critères.");
            noResults.getStyle()
                    .set("text-align", "center")
                    .set("color", "#666")
                    .set("margin", "40px 0");
            cardsContainer.add(noResults);
            updatePagination();
            return;
        }

        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, filteredEvents.size());
        List<EventLightDTO> pageEvents = filteredEvents.subList(start, end);

        HorizontalLayout row = null;
        int count = 0;

        for (EventLightDTO event : pageEvents) {
            if (count % 3 == 0) {
                row = new HorizontalLayout();
                row.setWidthFull();
                row.setSpacing(true);
                row.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
                cardsContainer.add(row);
            }

            row.add(createEventCard(event));
            count++;
        }

        updatePagination();
    }

    private VerticalLayout createEventCard(EventLightDTO event) {
        VerticalLayout card = new VerticalLayout();
        card.setWidth("350px");
        card.setPadding(false);
        card.setSpacing(false);
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "var(--lumo-box-shadow-m)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("overflow", "hidden")
                .set("transition", "transform 0.3s, box-shadow 0.3s")
                .set("cursor", "pointer");

        // Image
        Div imageContainer = new Div();
        imageContainer.setWidthFull();
        imageContainer.setHeight("200px");
        imageContainer.getStyle()
                .set("overflow", "hidden")
                .set("position", "relative");

        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            // Use ImageUtils to create the image
            Image eventImage = ImageUtils.createEventImage(event.getImageUrl(), "200px");
            eventImage.setWidth("100%");
            eventImage.setHeight("100%");
            eventImage.getStyle()
                    .set("object-fit", "cover")
                    .set("border-radius", "12px 12px 0 0");
            imageContainer.add(eventImage);
        } else {
            imageContainer.getStyle()
                    .set("background", "linear-gradient(135deg, var(--lumo-primary-color) 0%, var(--lumo-primary-color-50pct) 100%)")
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("justify-content", "center");

            Icon defaultIcon = getIconForCategory(event.getCategorie());
            defaultIcon.setSize("64px");
            defaultIcon.getStyle().set("color", "white");
            imageContainer.add(defaultIcon);
        }

        // Badge
        Span categoryBadge = new Span(event.getCategorie().getLabel());
        categoryBadge.getStyle()
                .set("position", "absolute")
                .set("top", "10px")
                .set("right", "10px")
                .set("background", event.getCategorie().getColor())
                .set("color", "white")
                .set("padding", "5px 15px")
                .set("border-radius", "20px")
                .set("font-size", "0.85em")
                .set("font-weight", "bold");

        imageContainer.getStyle().set("position", "relative");
        imageContainer.add(categoryBadge);

        // Contenu
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        H3 title = new H3(event.getTitre());
        title.getStyle()
                .set("margin", "0 0 10px 0")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "1.3em")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");

        // Lieu
        HorizontalLayout locationLayout = new HorizontalLayout();
        locationLayout.setSpacing(true);
        locationLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon locationIcon = VaadinIcon.MAP_MARKER.create();
        locationIcon.setSize("16px");
        locationIcon.getStyle().set("color", "var(--lumo-primary-color)");
        Span locationText = new Span(event.getVille() + " - " + event.getVille());
        locationText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.9em");
        locationLayout.add(locationIcon, locationText);

        // Date
        HorizontalLayout dateLayout = new HorizontalLayout();
        dateLayout.setSpacing(true);
        dateLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon dateIcon = VaadinIcon.CALENDAR.create();
        dateIcon.setSize("16px");
        dateIcon.getStyle().set("color", "var(--lumo-primary-color)");
        Span dateText = new Span(formatDateTime(event.getDateDebut()));
        dateText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.9em");
        dateLayout.add(dateIcon, dateText);

        // Footer
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);

        Span price = new Span(String.format("%.2f DH", event.getPrixUnitaire()));
        price.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("font-size", "1.3em")
                .set("font-weight", "bold");

        Span places = new Span(event.getPlacesDisponibles() + " places");
        places.getStyle()
                .set("color", event.getPlacesDisponibles() > 10 ? "#28a745" : "#dc3545")
                .set("font-size", "0.9em")
                .set("font-weight", "bold");

        footer.add(price, places);

        // Boutons
        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setWidthFull();
        buttonsLayout.setSpacing(true);

        Button detailsButton = new Button("Détails", VaadinIcon.EYE.create());
        detailsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        detailsButton.setWidthFull();
        detailsButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("event/" + event.getId()))
        );

        Button reserveButton = new Button("Réserver", VaadinIcon.TICKET.create());
        reserveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        reserveButton.setWidthFull();
        reserveButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("event/" + event.getId()))
        );

        if (event.getPlacesDisponibles() <= 0) {
            reserveButton.setEnabled(false);
            reserveButton.setText("Complet");
        }

        buttonsLayout.add(detailsButton, reserveButton);

        content.add(title, locationLayout, dateLayout, footer, buttonsLayout);
        card.add(imageContainer, content);

        // Hover
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle()
                    .set("transform", "translateY(-5px)")
                    .set("box-shadow", "var(--lumo-box-shadow-xl)");
        });

        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle()
                    .set("transform", "translateY(0)")
                    .set("box-shadow", "var(--lumo-box-shadow-m)");
        });

        return card;
    }

    private void updatePagination() {
        paginationLayout.removeAll();

        if (filteredEvents.isEmpty()) return;

        int totalPages = (int) Math.ceil((double) filteredEvents.size() / pageSize);

        Button prevButton = new Button("Précédent", VaadinIcon.ANGLE_LEFT.create());
        prevButton.setEnabled(currentPage > 0);
        prevButton.addClickListener(e -> {
            currentPage--;
            displayEvents();
        });

        HorizontalLayout pageNumbers = new HorizontalLayout();
        pageNumbers.setSpacing(true);

        for (int i = 0; i < totalPages; i++) {
            int pageIndex = i;
            Button pageButton = new Button(String.valueOf(i + 1));

            if (i == currentPage) {
                pageButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            } else {
                pageButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            }

            pageButton.addClickListener(e -> {
                currentPage = pageIndex;
                displayEvents();
            });

            pageNumbers.add(pageButton);
        }

        Button nextButton = new Button("Suivant", VaadinIcon.ANGLE_RIGHT.create());
        nextButton.setIconAfterText(true);
        nextButton.setEnabled(currentPage < totalPages - 1);
        nextButton.addClickListener(e -> {
            currentPage++;
            displayEvents();
        });

        Span pageInfo = new Span(String.format("Page %d sur %d", currentPage + 1, totalPages));
        pageInfo.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "0 20px");

        paginationLayout.add(prevButton, pageNumbers, pageInfo, nextButton);
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
        if (dateTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy 'à' HH:mm", new Locale("fr", "FR"));
        return dateTime.format(formatter);
    }

    private void showError(String message) {
        VerticalLayout cardsContainer = (VerticalLayout) getComponentAt(1);
        cardsContainer.removeAll();
        Paragraph error = new Paragraph(message);
        error.getStyle().set("color", "red").set("text-align", "center");
        cardsContainer.add(error);
    }
}