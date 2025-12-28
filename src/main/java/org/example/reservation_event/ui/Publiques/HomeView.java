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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.example.reservation_event.Enums.EventCategorie;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.services.EventService;
import org.example.reservation_event.ui.MainLayout;
import org.example.reservation_event.utils.ImageUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Route(value = "", layout = MainLayout.class)
@AnonymousAllowed
@PageTitle("Accueil - EventBooking")
public class HomeView extends VerticalLayout {

    private final EventService eventService;

    private TextField searchField;
    private ComboBox<EventCategorie> categorieComboBox;
    private DatePicker datePicker;
    private TextField villeField;
    private Button searchButton;
    private VerticalLayout eventsContainer;

    @Autowired
    public HomeView(EventService eventService) {
        this.eventService = eventService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Appliquer le thème au chargement
        applyDarkModeStyles();

        createHeroSection();
        createSearchSection();
        createPopularEventsSection();
    }

    private void applyDarkModeStyles() {
        // Le fond s'adapte automatiquement avec var(--lumo-contrast-5pct)
        getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("overflow-y", "auto");
    }

    private void createHeroSection() {
        VerticalLayout hero = new VerticalLayout();
        hero.setWidthFull();
        hero.setPadding(true);
        hero.setSpacing(true);
        hero.setAlignItems(FlexComponent.Alignment.CENTER);
        hero.getStyle()
                .set("background", "linear-gradient(135deg, var(--lumo-primary-color) 0%, var(--lumo-primary-color-50pct) 100%)")
                .set("padding", "60px 20px")
                .set("color", "white");

        H1 mainTitle = new H1("Découvrez les Événements qui Vous Passionnent");
        mainTitle.getStyle()
                .set("color", "white")
                .set("text-align", "center")
                .set("font-size", "2.5em")
                .set("margin", "0 0 10px 0")
                .set("font-weight", "bold")
                .set("text-shadow", "2px 2px 4px rgba(0,0,0,0.3)");

        Paragraph subtitle = new Paragraph("Concerts, théâtres, conférences, événements sportifs et bien plus encore...");
        subtitle.getStyle()
                .set("color", "rgba(255, 255, 255, 0.9)")
                .set("text-align", "center")
                .set("font-size", "1.2em")
                .set("margin", "0");

        hero.add(mainTitle, subtitle);
        add(hero);
    }

    private void createSearchSection() {
        VerticalLayout searchSection = new VerticalLayout();
        searchSection.setWidthFull();
        searchSection.setPadding(true);
        searchSection.setSpacing(true);
        searchSection.setAlignItems(FlexComponent.Alignment.CENTER);
        searchSection.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("padding", "40px 20px");

        VerticalLayout searchForm = new VerticalLayout();
        searchForm.setMaxWidth("800px");
        searchForm.setWidthFull();
        searchForm.setPadding(true);
        searchForm.setSpacing(true);
        searchForm.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "var(--lumo-box-shadow-m)")
                .set("border", "1px solid var(--lumo-contrast-10pct)");

        H2 searchTitle = new H2("Recherche Rapide");
        searchTitle.getStyle()
                .set("margin", "0 0 20px 0")
                .set("color", "var(--lumo-primary-text-color)")
                .set("text-align", "center");

        searchField = new TextField();
        searchField.setPlaceholder("Rechercher un événement...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidthFull();
        searchField.setClearButtonVisible(true);

        HorizontalLayout filtersLayout = new HorizontalLayout();
        filtersLayout.setWidthFull();
        filtersLayout.setSpacing(true);

        categorieComboBox = new ComboBox<>("Catégorie");
        categorieComboBox.setItems(EventCategorie.values());
        categorieComboBox.setItemLabelGenerator(EventCategorie::getLabel);
        categorieComboBox.setPlaceholder("Toutes");
        categorieComboBox.setClearButtonVisible(true);
        categorieComboBox.setWidth("200px");

        datePicker = new DatePicker("Date");
        datePicker.setPlaceholder("Sélectionner");
        datePicker.setClearButtonVisible(true);
        datePicker.setLocale(new Locale("fr", "FR"));
        datePicker.setWidth("200px");

        villeField = new TextField("Ville");
        villeField.setPlaceholder("Ex: Casablanca");
        villeField.setPrefixComponent(VaadinIcon.MAP_MARKER.create());
        villeField.setClearButtonVisible(true);
        villeField.setWidth("200px");

        filtersLayout.add(categorieComboBox, datePicker, villeField);

        searchButton = new Button("Rechercher", VaadinIcon.SEARCH.create());
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        searchButton.setWidthFull();
        searchButton.addClickListener(e -> performSearch());

        searchForm.add(searchTitle, searchField, filtersLayout, searchButton);
        searchSection.add(searchForm);
        add(searchSection);
    }

    private void createPopularEventsSection() {
        VerticalLayout popularSection = new VerticalLayout();
        popularSection.setWidthFull();
        popularSection.setPadding(true);
        popularSection.setSpacing(true);
        popularSection.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("padding", "40px 20px");

        H2 sectionTitle = new H2("Événements à la Une");
        sectionTitle.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("text-align", "center")
                .set("margin", "0 0 30px 0");

        Icon starIcon = VaadinIcon.STAR.create();
        starIcon.setSize("24px");
        starIcon.getStyle().set("color", "#FFD700");

        HorizontalLayout titleLayout = new HorizontalLayout(starIcon, sectionTitle);
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        titleLayout.setWidthFull();

        eventsContainer = new VerticalLayout();
        eventsContainer.setWidthFull();
        eventsContainer.setPadding(false);
        eventsContainer.setSpacing(true);

        popularSection.add(titleLayout, eventsContainer);
        add(popularSection);

        loadPopularEvents();
    }

    private void loadPopularEvents() {
        eventsContainer.removeAll();

        try {
            List<Event> popularEvents = eventService.getPopularEvents(6);

            if (popularEvents.isEmpty()) {
                Paragraph noEvents = new Paragraph("Aucun événement disponible pour le moment.");
                noEvents.getStyle()
                        .set("text-align", "center")
                        .set("color", "#666")
                        .set("font-size", "1.1em")
                        .set("margin", "40px 0");
                eventsContainer.add(noEvents);
                return;
            }

            HorizontalLayout row = null;
            int count = 0;

            for (Event event : popularEvents) {
                if (count % 3 == 0) {
                    row = new HorizontalLayout();
                    row.setWidthFull();
                    row.setSpacing(true);
                    row.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
                    eventsContainer.add(row);
                }

                row.add(createEventCard(event));
                count++;
            }

        } catch (Exception e) {
            Paragraph error = new Paragraph("Erreur lors du chargement des événements.");
            error.getStyle().set("color", "red").set("text-align", "center");
            eventsContainer.add(error);
        }
    }

    private VerticalLayout createEventCard(Event event) {
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
            // Display category icon when no image is available
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

        // Badge catégorie
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
        Span locationText = new Span(event.getVille() + " - " + event.getLieu());
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

        // Prix et places
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        footer.getStyle().set("margin-top", "15px");

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

        // Bouton
        Button detailsButton = new Button("Voir les détails", VaadinIcon.ARROW_RIGHT.create());
        detailsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        detailsButton.setWidthFull();
        detailsButton.setIconAfterText(true);
        detailsButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("event/" + event.getId()))
        );

        content.add(title, locationLayout, dateLayout, footer, detailsButton);
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

        String[] months = {"Jan", "Fév", "Mar", "Avr", "Mai", "Juin",
                "Juil", "Août", "Sep", "Oct", "Nov", "Déc"};

        return dateTime.getDayOfMonth() + " " +
                months[dateTime.getMonthValue() - 1] + " " +
                dateTime.getYear() + " à " +
                String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
    }

    private void performSearch() {
        String keyword = searchField.getValue();
        EventCategorie categorie = categorieComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String ville = villeField.getValue();

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        if (date != null) {
            startDateTime = date.atStartOfDay();
            endDateTime = date.atTime(23, 59, 59);
        }

        try {
            List<Event> results = eventService.searchEvents(
                    keyword, ville, startDateTime, endDateTime,
                    null, null, EventStatut.PUBLIE
            );

            if (categorie != null) {
                results = results.stream()
                        .filter(e -> e.getCategorie() == categorie)
                        .collect(Collectors.toList());
            }

            displaySearchResults(results);

        } catch (Exception e) {
            eventsContainer.removeAll();
            Paragraph error = new Paragraph("Erreur lors de la recherche: " + e.getMessage());
            error.getStyle().set("color", "red").set("text-align", "center");
            eventsContainer.add(error);
        }
    }

    private void displaySearchResults(List<Event> results) {
        eventsContainer.removeAll();

        if (results.isEmpty()) {
            Paragraph noResults = new Paragraph("Aucun événement trouvé pour votre recherche.");
            noResults.getStyle()
                    .set("text-align", "center")
                    .set("color", "#666")
                    .set("font-size", "1.1em")
                    .set("margin", "40px 0");

            Button resetButton = new Button("Réinitialiser", VaadinIcon.REFRESH.create());
            resetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            resetButton.addClickListener(e -> {
                searchField.clear();
                categorieComboBox.clear();
                datePicker.clear();
                villeField.clear();
                loadPopularEvents();
            });

            VerticalLayout noResultsLayout = new VerticalLayout(noResults, resetButton);
            noResultsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            eventsContainer.add(noResultsLayout);
            return;
        }

        Paragraph resultCount = new Paragraph(results.size() + " événement(s) trouvé(s)");
        resultCount.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "bold")
                .set("font-size", "1.1em")
                .set("margin", "0 0 20px 0");
        eventsContainer.add(resultCount);

        HorizontalLayout row = null;
        int count = 0;

        for (Event event : results) {
            if (count % 3 == 0) {
                row = new HorizontalLayout();
                row.setWidthFull();
                row.setSpacing(true);
                row.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
                eventsContainer.add(row);
            }

            row.add(createEventCard(event));
            count++;
        }
    }
}