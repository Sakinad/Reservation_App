package org.example.reservation_event.ui.Organizateurs;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.Review;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.repositories.ReviewRepository;
import org.example.reservation_event.services.EventService;
import org.example.reservation_event.services.ReservationService;
import org.example.reservation_event.services.ReviewService;
import org.example.reservation_event.services.UserService;
import org.example.reservation_event.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "organizer/dashboard", layout = MainLayout.class)
@PageTitle("Tableau de bord - Organisateur")
public class OrganizerDashboardView extends VerticalLayout {

    private final UserService userService;
    private final EventService eventService;
    private final ReservationService reservationService;
    private User currentUser;
    private final ReviewService reviewService;
    private ReviewRepository reviewRepository;

    @Autowired
    public OrganizerDashboardView(UserService userService,
                                  EventService eventService,
                                  ReservationService reservationService,
                                  ReviewService reviewService,
                                  ReviewRepository reviewRepository) {
        this.userService = userService;
        this.eventService = eventService;
        this.reservationService = reservationService;
        this.reviewService = reviewService;
        this.reviewRepository=reviewRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Style adaptatif pour dark mode
        getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("overflow-y", "auto");

        currentUser = getCurrentUser();
        if (currentUser == null || !isOrganizer(currentUser)) {
            showAccessDenied();
            return;
        }

        initializeContent();
    }

    private void initializeContent() {
        add(
                createHeader(),
                createWelcomeSection(),
                createStatisticsSection(),
                createReviewsStatisticsSection(),
                createRecentEventsSection()
        );
    }

    private Component createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        // Style adaptatif
        header.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.1)")
                .set("border-radius", "12px")
                .set("margin-bottom", "20px");

        VerticalLayout titleSection = new VerticalLayout();
        titleSection.setSpacing(false);
        titleSection.setPadding(false);

        H1 welcome = new H1("Bonjour " + currentUser.getPrenom() + " !");
        welcome.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "var(--lumo-font-size-xl)");

        Span subtitle = new Span("Voici votre tableau de bord personnel");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-m)");

        titleSection.add(welcome, subtitle);

        // Date du jour
        Span today = new Span(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", new Locale("fr", "FR"))));
        today.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("white-space", "nowrap");

        header.add(titleSection, today);
        header.setFlexGrow(1, titleSection);
        header.setFlexGrow(0, today);

        return header;
    }
    private Component createReviewsStatisticsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)")
                .set("margin-top", "20px");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        H3 sectionTitle = new H3("⭐ Avis sur vos événements");
        sectionTitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        header.add(sectionTitle);

        try {
            // ✅ FIXED: Get ALL reviews for organizer's events in ONE QUERY with JOIN FETCH
            List<Review> allReviews = reviewRepository.findReviewsForOrganizerEvents(currentUser.getId());

            if (allReviews.isEmpty()) {
                Div emptyState = createReviewsEmptyState();
                section.add(header, emptyState);
                return section;
            }

            // ✅ Now we can safely access review.getEvent().getTitre() because
            // the event is already loaded with JOIN FETCH

            // Group reviews by event for statistics
            Map<Long, List<Review>> reviewsByEvent = allReviews.stream()
                    .collect(Collectors.groupingBy(r -> r.getEvent().getId()));

            // Calculate statistics
            long totalReviews = allReviews.size();
            double globalAverage = allReviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);

            long eventsWithReviews = reviewsByEvent.size();

            // Get organizer events count
            List<Event> organizerEvents = eventService.getEventsByOrganizer(currentUser);

            // Cartes de statistiques
            HorizontalLayout statsRow = new HorizontalLayout();
            statsRow.setWidthFull();
            statsRow.setSpacing(true);

            statsRow.add(
                    createStatCard("Total d'avis",
                            String.valueOf(totalReviews),
                            VaadinIcon.COMMENT, "#f59e0b"),
                    createStatCard("Note moyenne globale",
                            String.format("%.1f / 5 ⭐", globalAverage),
                            VaadinIcon.STAR, "#8b5cf6"),
                    createStatCard("Événements notés",
                            String.format("%d / %d", eventsWithReviews, organizerEvents.size()),
                            VaadinIcon.CALENDAR, "#10b981")
            );

            // Meilleur événement (le mieux noté)
            Component bestEvent = createBestRatedEventCard(reviewsByEvent);

            // Derniers avis reçus
            H3 recentTitle = new H3("Derniers avis reçus");
            recentTitle.getStyle()
                    .set("margin", "24px 0 12px 0")
                    .set("color", "var(--lumo-primary-text-color)");

            VerticalLayout recentReviews = new VerticalLayout();
            recentReviews.setSpacing(true);
            recentReviews.setPadding(false);

            // ✅ SAFE: Can access review.getEvent() because it's already loaded
            allReviews.stream()
                    .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                    .limit(3)
                    .forEach(review -> recentReviews.add(createOrganizerReviewItem(review)));

            section.add(header, statsRow, bestEvent, recentTitle, recentReviews);

        } catch (Exception e) {
            System.err.println("Erreur chargement avis organisateur: " + e.getMessage());
            e.printStackTrace();
            Div errorState = createErrorState("Impossible de charger les avis");
            section.add(header, errorState);
        }

        return section;
    }

    // 4️⃣ NOUVELLE MÉTHODE : Carte meilleur événement
    private Component createBestRatedEventCard(Map<Long, List<Review>> reviewsByEvent) {
        // Trouver l'événement avec la meilleure moyenne
        Map.Entry<Long, Double> bestEntry = reviewsByEvent.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .mapToInt(Review::getRating)
                                .average()
                                .orElse(0.0)
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (bestEntry == null || bestEntry.getValue() < 4.0) {
            return new Div(); // Ne rien afficher si pas de bon événement
        }

        // Récupérer l'événement
        Event bestEvent = eventService.getEventById(bestEntry.getKey());
        if (bestEvent == null) return new Div();

        List<Review> eventReviews = reviewsByEvent.get(bestEntry.getKey());

        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
                .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 4px 12px rgba(102, 126, 234, 0.4)")
                .set("color", "white")
                .set("margin-top", "16px");

        HorizontalLayout cardHeader = new HorizontalLayout();
        cardHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        cardHeader.setSpacing(true);

        Icon trophy = new Icon(VaadinIcon.TROPHY);
        trophy.setSize("32px");
        trophy.setColor("#fbbf24");

        H3 title = new H3("🏆 Votre événement le mieux noté");
        title.getStyle()
                .set("margin", "0")
                .set("color", "white")
                .set("text-shadow", "0 2px 4px rgba(0,0,0,0.2)");

        cardHeader.add(trophy, title);

        H2 eventName = new H2(bestEvent.getTitre());
        eventName.getStyle()
                .set("margin", "8px 0")
                .set("color", "white")
                .set("font-size", "var(--lumo-font-size-xxl)");

        HorizontalLayout stars = new HorizontalLayout();
        stars.setSpacing(false);
        stars.getStyle().set("gap", "4px");

        int fullStars = (int) Math.round(bestEntry.getValue());
        for (int i = 0; i < fullStars; i++) {
            Icon star = new Icon(VaadinIcon.STAR);
            star.setSize("24px");
            star.setColor("#fbbf24");
            stars.add(star);
        }

        Span ratingText = new Span(String.format("%.1f / 5 ⭐ (%d avis)",
                bestEntry.getValue(), eventReviews.size()));
        ratingText.getStyle()
                .set("color", "rgba(255,255,255,0.95)")
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin-left", "8px");

        HorizontalLayout ratingRow = new HorizontalLayout(stars, ratingText);
        ratingRow.setAlignItems(FlexComponent.Alignment.CENTER);

        card.add(cardHeader, eventName, ratingRow);
        return card;
    }

    // 5️⃣ NOUVELLE MÉTHODE : Carte d'avis pour organisateur
    private Component createOrganizerReviewItem(Review review) {
        HorizontalLayout item = new HorizontalLayout();
        item.setWidthFull();
        item.setPadding(true);
        item.setSpacing(true);
        item.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px");
        item.setAlignItems(FlexComponent.Alignment.START);

        Icon starIcon = new Icon(VaadinIcon.STAR);
        starIcon.setSize("20px");
        starIcon.setColor("#f59e0b");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        content.setWidthFull();

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout eventInfo = new VerticalLayout();
        eventInfo.setSpacing(false);
        eventInfo.setPadding(false);

        Span eventName = new Span(review.getEvent().getTitre());
        eventName.getStyle()
                .set("font-weight", "600")
                .set("color", "var(--lumo-primary-text-color)");

        Span userName = new Span("Par " + review.getUser().getPrenom() + " " + review.getUser().getNom());
        userName.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        eventInfo.add(eventName, userName);

        HorizontalLayout starsDisplay = new HorizontalLayout();
        starsDisplay.setSpacing(false);
        starsDisplay.getStyle().set("gap", "2px");

        for (int i = 0; i < 5; i++) {
            Icon star = new Icon(i < review.getRating() ? VaadinIcon.STAR : VaadinIcon.STAR_O);
            star.setSize("14px");
            star.setColor("#f59e0b");
            starsDisplay.add(star);
        }

        header.add(eventInfo, starsDisplay);

        if (review.getComment() != null && !review.getComment().trim().isEmpty()) {
            Span comment = new Span(review.getComment());
            comment.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("margin-top", "8px")
                    .set("font-style", "italic");
            content.add(header, comment);
        } else {
            content.add(header);
        }

        Span date = new Span(review.getCreatedAt().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        date.getStyle()
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("margin-top", "8px");
        content.add(date);

        item.add(starIcon, content);
        item.expand(content);

        return item;
    }

    // 6️⃣ NOUVELLE MÉTHODE : État vide
    private Div createReviewsEmptyState() {
        Div emptyState = new Div();
        emptyState.getStyle()
                .set("text-align", "center")
                .set("padding", "40px 20px")
                .set("border-radius", "8px")
                .set("border", "2px dashed var(--lumo-contrast-20pct)");

        Icon emptyIcon = new Icon(VaadinIcon.STAR_O);
        emptyIcon.setSize("48px");
        emptyIcon.setColor("var(--lumo-contrast-40pct)");

        H3 emptyTitle = new H3("Aucun avis pour le moment");
        emptyTitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "16px 0 8px 0");

        Span emptyDesc = new Span("Les participants pourront laisser des avis après vos événements");
        emptyDesc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        emptyState.add(emptyIcon, emptyTitle, emptyDesc);
        return emptyState;
    }

    // 7️⃣ État d'erreur (si pas déjà présent)
    private Div createErrorState(String message) {
        Div errorState = new Div();
        errorState.getStyle()
                .set("text-align", "center")
                .set("padding", "20px")
                .set("background", "var(--lumo-error-color-10pct)")
                .set("border-radius", "8px")
                .set("color", "var(--lumo-error-text-color)");

        Icon errorIcon = new Icon(VaadinIcon.WARNING);
        errorIcon.setSize("24px");

        Span errorText = new Span(message);
        errorText.getStyle().set("margin-left", "8px");

        errorState.add(errorIcon, errorText);
        return errorState;
    }
    private Component createWelcomeSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);

        // Gradient adaptatif
        section.getStyle()
                .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
                .set("border-radius", "12px")
                .set("color", "white")
                .set("box-shadow", "0 4px 12px rgba(0,0,0,0.15)");

        H2 welcome = new H2("Bienvenue, " + currentUser.getPrenom() + " " + currentUser.getNom());
        welcome.getStyle()
                .set("margin", "0 0 10px 0")
                .set("color", "white");

        Span subtitle = new Span("Voici un aperçu de vos événements et performances");
        subtitle.getStyle()
                .set("color", "rgba(255,255,255,0.9)")
                .set("font-size", "var(--lumo-font-size-m)");

        section.add(welcome, subtitle);
        return section;
    }

    private Component createStatisticsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(false);
        section.setSpacing(true);

        H3 sectionTitle = new H3("Statistiques");
        sectionTitle.getStyle()
                .set("margin", "0 0 15px 0")
                .set("color", "var(--lumo-primary-text-color)");

        Map<String, Object> stats = eventService.getOrganizerStats(currentUser);

        HorizontalLayout statsGrid = new HorizontalLayout();
        statsGrid.setWidthFull();
        statsGrid.setSpacing(true);
        statsGrid.getStyle().set("flex-wrap", "wrap");

        int totalEvents = ((Number) stats.getOrDefault("totalEvents", 0)).intValue();
        int publishedEvents = ((Number) stats.getOrDefault("publishedEvents", 0)).intValue();
        int draftEvents = ((Number) stats.getOrDefault("draftEvents", 0)).intValue();
        int totalReservations = ((Number) stats.getOrDefault("totalReservations", 0)).intValue();
        double totalRevenue = ((Number) stats.getOrDefault("totalRevenue", 0.0)).doubleValue();

        statsGrid.add(
                createStatCard("Total Événements", String.valueOf(totalEvents),
                        VaadinIcon.CALENDAR, "#4f46e5"),
                createStatCard("Publiés", String.valueOf(publishedEvents),
                        VaadinIcon.CHECK_CIRCLE, "#10b981"),
                createStatCard("Brouillons", String.valueOf(draftEvents),
                        VaadinIcon.EDIT, "#f59e0b"),
                createStatCard("Réservations", String.valueOf(totalReservations),
                        VaadinIcon.USERS, "#8b5cf6"),
                createStatCard("Revenus", String.format("%.2f DH", totalRevenue),
                        VaadinIcon.MONEY, "#06b6d4")
        );

        section.add(sectionTitle, statsGrid);
        return section;
    }

    private Component createStatCard(String label, String value, VaadinIcon icon, String color) {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);

        // Style adaptatif pour dark mode
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)")
                .set("border-left", "4px solid " + color)
                .set("transition", "transform 0.2s, box-shadow 0.2s")
                .set("min-width", "180px");

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

        Icon cardIcon = new Icon(icon);
        cardIcon.setSize("32px");
        cardIcon.setColor(color);

        Span cardLabel = new Span(label);
        cardLabel.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        H2 cardValue = new H2(value);
        cardValue.getStyle()
                .set("margin", "5px 0 0 0")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "var(--lumo-font-size-xxl)");

        card.add(cardIcon, cardLabel, cardValue);
        return card;
    }

    private Component createRecentEventsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);

        // Style adaptatif
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        H3 sectionTitle = new H3("Événements récents");
        sectionTitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Button viewAllBtn = new Button("Voir tous", new Icon(VaadinIcon.ARROW_RIGHT));
        viewAllBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        viewAllBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("organizer/events")));

        header.add(sectionTitle, viewAllBtn);

        List<Event> recentEvents = eventService.getOrganizerRecentEvents(currentUser.getId(), 5);

        if (recentEvents.isEmpty()) {
            Div emptyState = new Div();
            emptyState.getStyle()
                    .set("text-align", "center")
                    .set("padding", "40px")
                    .set("color", "var(--lumo-secondary-text-color)");

            Icon emptyIcon = new Icon(VaadinIcon.CALENDAR_O);
            emptyIcon.setSize("64px");
            emptyIcon.getStyle()
                    .set("margin-bottom", "20px")
                    .set("color", "var(--lumo-tertiary-text-color)");

            Span emptyText = new Span("Aucun événement créé pour le moment");
            emptyText.getStyle()
                    .set("display", "block")
                    .set("font-size", "var(--lumo-font-size-l)");

            Button createBtn = new Button("Créer votre premier événement", new Icon(VaadinIcon.PLUS));
            createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            createBtn.getStyle().set("margin-top", "20px");
            createBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("organizer/event/new")));

            emptyState.add(emptyIcon, emptyText, createBtn);
            section.add(header, emptyState);
        } else {
            VerticalLayout eventsList = new VerticalLayout();
            eventsList.setWidthFull();
            eventsList.setPadding(false);
            eventsList.setSpacing(true);

            for (Event event : recentEvents) {
                eventsList.add(createEventCard(event));
            }

            section.add(header, eventsList);
        }

        return section;
    }

    private Component createEventCard(Event event) {
        HorizontalLayout card = new HorizontalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.setAlignItems(FlexComponent.Alignment.CENTER);

        // Style adaptatif
        card.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px")
                .set("border-left", "4px solid " + getStatusColor(event.getStatut()))
                .set("cursor", "pointer")
                .set("transition", "all 0.2s");

        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle().set("background", "var(--lumo-contrast-10pct)");
        });

        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle().set("background", "var(--lumo-contrast-5pct)");
        });

        // Event info
        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);

        H4 title = new H4(event.getTitre());
        title.getStyle()
                .set("margin", "0 0 5px 0")
                .set("color", "var(--lumo-primary-text-color)");

        Span details = new Span(
                event.getCategorie().getLabel() + " • " +
                        formatDate(event.getDateDebut()) + " • " +
                        event.getLieu()
        );
        details.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        info.add(title, details);

        // Status badge
        Span statusBadge = new Span(event.getStatut().getLabel());
        statusBadge.getStyle()
                .set("background", getStatusColor(event.getStatut()))
                .set("color", "white")
                .set("padding", "4px 12px")
                .set("border-radius", "12px")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "500");

        // Reservations info
        int reservedPlaces = reservationService.getTotalReservedPlaces(event.getId());
        VerticalLayout reservationsInfo = new VerticalLayout();
        reservationsInfo.setPadding(false);
        reservationsInfo.setSpacing(false);
        reservationsInfo.setAlignItems(FlexComponent.Alignment.END);

        Span placesText = new Span(reservedPlaces + "/" + event.getCapaciteMax());
        placesText.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        Span placesLabel = new Span("places réservées");
        placesLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        reservationsInfo.add(placesText, placesLabel);

        card.add(info, statusBadge, reservationsInfo);
        card.expand(info);

        card.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/event/edit/" + event.getId()))
        );

        return card;
    }

    private String getStatusColor(EventStatut statut) {
        return switch (statut) {
            case PUBLIE -> "#10b981";
            case BROUILLON -> "#f59e0b";
            case ANNULE -> "#ef4444";
            case TERMINE -> "#6b7280";
        };
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
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

        Span message = new Span("Vous devez être organisateur pour accéder à cette page");

        Button homeBtn = new Button("Retour à l'accueil", e ->
                getUI().ifPresent(ui -> ui.navigate("dashboard")));
        homeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(icon, title, message, homeBtn);
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