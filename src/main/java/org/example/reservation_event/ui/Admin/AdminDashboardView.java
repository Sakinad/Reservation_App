package org.example.reservation_event.ui.Admin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.Enums.ReservationStatut;
import org.example.reservation_event.Enums.UserRole;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.classes.Review;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.repositories.EventRepository;
import org.example.reservation_event.repositories.ReservationRepository;
import org.example.reservation_event.repositories.ReviewRepository;
import org.example.reservation_event.repositories.UserRepository;
import org.example.reservation_event.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdminDashboardView - Vue d'ensemble globale de la plateforme
 * Version améliorée avec graphiques (Bonus du cahier des charges)
 *
 * Affiche :
 * - Nombre total d'utilisateurs (par rôle)
 * - Nombre total d'événements (par statut)
 * - Nombre total de réservations
 * - Revenus totaux de la plateforme
 * - Graphiques de revenus par mois
 * - Graphiques de réservations par catégorie
 * - Statistiques avancées
 */
@Route(value = "admin/dashboard", layout = MainLayout.class)
@PageTitle("Dashboard Administrateur | Event Manager")
@RolesAllowed("ADMIN")
public class AdminDashboardView extends VerticalLayout {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;

    // Composants UI
    private Div statsContainer;

    @Autowired
    public AdminDashboardView(
            UserRepository userRepository,
            EventRepository eventRepository,
            ReservationRepository reservationRepository,
            ReviewRepository reviewRepository) {

        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.reviewRepository = reviewRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createStatsCards();
        createDetailedStats();

        loadStatistics();
    }

    private void createHeader() {
        H1 title = new H1("📊 Dashboard Administrateur");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "700");

        Span subtitle = new Span("Vue d'ensemble de la plateforme");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "14px");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setSpacing(false);
        header.setPadding(false);

        add(header);
    }

    private void createStatsCards() {
        // ✅ Disposition HORIZONTALE des cartes principales
        statsContainer = new Div();
        statsContainer.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "20px")
                .set("margin-top", "20px")
                .set("justify-content", "space-between");

        add(statsContainer);
    }

    private void createDetailedStats() {
        // ✅ Section pour stats détaillées AVANT les graphiques
        H2 detailsTitle2 = new H2("📈 Répartition Détaillée");
        detailsTitle2.getStyle()
                .set("margin-top", "40px")
                .set("color", "var(--lumo-primary-text-color)");

        // ✅ Disposition HORIZONTALE pour les stats détaillées
        Div detailsContainer = new Div();
        detailsContainer.setId("detailsContainer");
        detailsContainer.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "20px")
                .set("margin-top", "20px")
                .set("justify-content", "space-between");

        add(detailsTitle2, detailsContainer);

        // ✅ Section graphiques APRÈS
        H2 detailsTitle = new H2("📊 Graphiques et Statistiques");
        detailsTitle.getStyle()
                .set("margin-top", "40px")
                .set("color", "var(--lumo-primary-text-color)");

        Div chartsContainer = new Div();
        chartsContainer.setId("chartsContainer");
        chartsContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(450px, 1fr))")
                .set("gap", "20px")
                .set("margin-top", "20px");

        add(detailsTitle, chartsContainer);
    }

    private void loadStatistics() {
        try {
            // Charger toutes les données
            List<User> allUsers = userRepository.findAll();
            List<Event> allEvents = eventRepository.findAll();
            List<Reservation> allReservations = reservationRepository.findAll();
            List<Review> allReviews = reviewRepository.findAll();
            // Calculer les statistiques
            Map<UserRole, Long> usersByRole = allUsers.stream()
                    .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));

            Map<EventStatut, Long> eventsByStatus = allEvents.stream()
                    .collect(Collectors.groupingBy(Event::getStatut, Collectors.counting()));

            long totalReservations = allReservations.size();

            long confirmedReservations = allReservations.stream()
                    .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                    .count();

            double totalRevenue = allReservations.stream()
                    .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                    .mapToDouble(Reservation::getMontantTotal)
                    .sum();

            // Créer les cartes de statistiques principales
            statsContainer.removeAll();

            statsContainer.add(createStatCard(
                    "👥 Utilisateurs Total",
                    String.valueOf(allUsers.size()),
                    VaadinIcon.USERS,
                    "#4CAF50",
                    "+" + usersByRole.getOrDefault(UserRole.CLIENT, 0L) + " clients"
            ));

            statsContainer.add(createStatCard(
                    "📅 Événements",
                    String.valueOf(allEvents.size()),
                    VaadinIcon.CALENDAR,
                    "#2196F3",
                    eventsByStatus.getOrDefault(EventStatut.PUBLIE, 0L) + " publiés"
            ));

            statsContainer.add(createStatCard(
                    "🎫 Réservations",
                    String.valueOf(totalReservations),
                    VaadinIcon.TICKET,
                    "#FF9800",
                    confirmedReservations + " confirmées"
            ));

            statsContainer.add(createStatCard(
                    "💰 Revenus Totaux",
                    String.format("%.2f DH", totalRevenue),
                    VaadinIcon.MONEY,
                    "#9C27B0",
                    "Total confirmé"
            ));
            statsContainer.add(createStatCard(
                    "⭐ Avis Total",
                    String.valueOf(allReviews.size()),
                    VaadinIcon.STAR,
                    "#f59e0b",
                    calculateAverageRating(allReviews) + " / 5 en moyenne"
            ));
            // ✅ AJOUTER LES GRAPHIQUES (Bonus du cahier des charges)
            createRevenueByMonthChart(allReservations);
            createReservationsByCategoryChart(allEvents);
            createEventStatusChart(eventsByStatus);
            createReviewsOverviewChart(allReviews);
            createTopRatedEventsChart(allEvents, allReviews);

            // Créer les statistiques détaillées
            createUserRoleStats(usersByRole);
            createEventStatusStats(eventsByStatus);
            createReservationStats(allReservations);

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des statistiques: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // 3️⃣ NOUVELLE MÉTHODE : Calculer la moyenne globale
    private String calculateAverageRating(List<Review> reviews) {
        if (reviews.isEmpty()) return "0.0";
        double average = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        return String.format("%.1f", average);
    }

    // 4️⃣ NOUVELLE MÉTHODE : Vue d'ensemble des avis
    private void createReviewsOverviewChart(List<Review> reviews) {
        Div chartContainer = createChartContainer("⭐ Distribution des Notes");

        if (reviews.isEmpty()) {
            Span noData = new Span("Aucun avis disponible");
            noData.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("text-align", "center")
                    .set("display", "block")
                    .set("padding", "40px");
            chartContainer.add(noData);
            addChartToContainer(chartContainer);
            return;
        }

        VerticalLayout chart = new VerticalLayout();
        chart.setPadding(false);
        chart.setSpacing(true);

        // Compter les avis par note (5 étoiles -> 1 étoile)
        Map<Integer, Long> reviewsByRating = reviews.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        long maxCount = reviewsByRating.values().stream().max(Long::compareTo).orElse(1L);

        // Afficher de 5 à 1 étoiles
        for (int rating = 5; rating >= 1; rating--) {
            long count = reviewsByRating.getOrDefault(rating, 0L);
            String label = rating + " ⭐";

            chart.add(createBarChartRow(
                    label,
                    count,
                    maxCount,
                    getRatingColor(rating)
            ));
        }

        // Statistiques supplémentaires
        Div statsBox = new Div();
        statsBox.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("margin-top", "16px");

        double average = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        HorizontalLayout statsRow = new HorizontalLayout();
        statsRow.setWidthFull();
        statsRow.setJustifyContentMode(
                FlexComponent.JustifyContentMode.AROUND
        );

        statsRow.add(
                createMiniStat("Total", String.valueOf(reviews.size())),
                createMiniStat("Moyenne", String.format("%.1f / 5", average)),
                createMiniStat("Satisfaits", calculateSatisfactionRate(reviews) + "%")
        );

        statsBox.add(statsRow);
        chart.add(statsBox);

        chartContainer.add(chart);
        addChartToContainer(chartContainer);
    }

    // 5️⃣ NOUVELLE MÉTHODE : Top événements les mieux notés
    private void createTopRatedEventsChart(List<Event> allEvents, List<Review> allReviews) {
        Div chartContainer = createChartContainer("🏆 Top 5 Événements Les Mieux Notés");

        // Grouper les avis par événement
        Map<Long, List<Review>> reviewsByEvent = allReviews.stream()
                .collect(Collectors.groupingBy(r -> r.getEvent().getId()));

        // Calculer la moyenne par événement (min 3 avis)
        Map<Event, Double> eventAverages = allEvents.stream()
                .filter(e -> reviewsByEvent.containsKey(e.getId()))
                .filter(e -> reviewsByEvent.get(e.getId()).size() >= 3)
                .collect(Collectors.toMap(
                        e -> e,
                        e -> reviewsByEvent.get(e.getId()).stream()
                                .mapToInt(Review::getRating)
                                .average()
                                .orElse(0.0)
                ));

        if (eventAverages.isEmpty()) {
            Span noData = new Span("Pas assez d'avis (minimum 3 par événement)");
            noData.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("text-align", "center")
                    .set("display", "block")
                    .set("padding", "40px");
            chartContainer.add(noData);
            addChartToContainer(chartContainer);
            return;
        }

        VerticalLayout chart = new VerticalLayout();
        chart.setPadding(false);
        chart.setSpacing(true);

        // Top 5
        eventAverages.entrySet().stream()
                .sorted(Map.Entry.<Event, Double>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    Event event = entry.getKey();
                    double average = entry.getValue();
                    int reviewCount = reviewsByEvent.get(event.getId()).size();

                    HorizontalLayout row = new HorizontalLayout();
                    row.setWidthFull();
                    row.setAlignItems(FlexComponent.Alignment.CENTER);
                    row.setSpacing(true);
                    row.getStyle()
                            .set("background", "var(--lumo-contrast-5pct)")
                            .set("border-radius", "8px")
                            .set("padding", "12px")
                            .set("margin-bottom", "8px");

                    VerticalLayout info = new VerticalLayout();
                    info.setSpacing(false);
                    info.setPadding(false);

                    Span title = new Span(event.getTitre());
                    title.getStyle()
                            .set("font-weight", "600")
                            .set("color", "var(--lumo-primary-text-color)");

                    Span details = new Span(reviewCount + " avis • " + event.getCategorie().getLabel());
                    details.getStyle()
                            .set("font-size", "var(--lumo-font-size-s)")
                            .set("color", "var(--lumo-secondary-text-color)");

                    info.add(title, details);

                    HorizontalLayout stars = new HorizontalLayout();
                    stars.setSpacing(false);
                    stars.getStyle().set("gap", "2px");

                    int fullStars = (int) Math.round(average);
                    for (int i = 0; i < fullStars; i++) {
                        Icon star = new Icon(VaadinIcon.STAR);
                        star.setSize("16px");
                        star.setColor("#f59e0b");
                        stars.add(star);
                    }

                    Span rating = new Span(String.format("%.1f", average));
                    rating.getStyle()
                            .set("font-weight", "700")
                            .set("color", "#f59e0b")
                            .set("font-size", "var(--lumo-font-size-l)")
                            .set("margin-left", "8px");

                    row.add(info);
                    row.expand(info);
                    row.add(stars, rating);

                    chart.add(row);
                });

        chartContainer.add(chart);
        addChartToContainer(chartContainer);
    }

    // 6️⃣ MÉTHODES HELPER
    private String getRatingColor(int rating) {
        return switch (rating) {
            case 5 -> "#10b981"; // Vert
            case 4 -> "#84cc16"; // Vert clair
            case 3 -> "#f59e0b"; // Orange
            case 2 -> "#f97316"; // Orange foncé
            case 1 -> "#ef4444"; // Rouge
            default -> "#6b7280"; // Gris
        };
    }

    private String calculateSatisfactionRate(List<Review> reviews) {
        if (reviews.isEmpty()) return "0";

        long satisfied = reviews.stream()
                .filter(r -> r.getRating() >= 4)
                .count();

        double rate = (satisfied * 100.0) / reviews.size();
        return String.format("%.0f", rate);
    }

    private VerticalLayout createMiniStat(String label, String value) {
        VerticalLayout stat = new VerticalLayout();
        stat.setSpacing(false);
        stat.setPadding(false);
        stat.setAlignItems(FlexComponent.Alignment.CENTER);

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "700")
                .set("color", "var(--lumo-primary-text-color)");

        stat.add(valueSpan, labelSpan);
        return stat;
    }
    private Div createStatCard(String title, String value, VaadinIcon icon, String color, String subtitle) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("padding", "24px")
                .set("box-shadow", "0 4px 12px var(--lumo-shade-10pct)")
                .set("border-left", "4px solid " + color)
                .set("flex", "1")
                .set("min-width", "220px")
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

        Icon cardIcon = icon.create();
        cardIcon.setSize("48px");
        cardIcon.getStyle()
                .set("color", color)
                .set("opacity", "0.9");

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-size", "13px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("display", "block")
                .set("margin-top", "12px")
                .set("font-weight", "500");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "32px")
                .set("font-weight", "700")
                .set("color", "var(--lumo-primary-text-color)")
                .set("display", "block")
                .set("margin", "8px 0")
                .set("letter-spacing", "-0.5px");

        Span subtitleSpan = new Span(subtitle);
        subtitleSpan.getStyle()
                .set("font-size", "12px")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("display", "block");

        card.add(cardIcon, titleSpan, valueSpan, subtitleSpan);
        return card;
    }

    // ========================================
    // 📊 GRAPHIQUES (BONUS - Cahier des charges)
    // ========================================

    /**
     * Graphique des revenus par mois (Bonus) - VERSION LINÉAIRE
     */
    private void createRevenueByMonthChart(List<Reservation> reservations) {
        Div chartContainer = createChartContainer("📈 Revenus par Mois");

        // Calculer les revenus par mois
        Map<String, Double> revenueByMonth = reservations.stream()
                .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                .collect(Collectors.groupingBy(
                        r -> r.getDateReservation().format(DateTimeFormatter.ofPattern("MMM yyyy")),
                        Collectors.summingDouble(Reservation::getMontantTotal)
                ));

        // Créer le graphique linéaire
        Div chart = new Div();
        chart.getStyle()
                .set("width", "100%")
                .set("height", "300px")
                .set("position", "relative")
                .set("padding", "20px 10px");

        if (revenueByMonth.isEmpty()) {
            Span noData = new Span("Aucune donnée disponible");
            noData.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("text-align", "center")
                    .set("display", "block")
                    .set("padding", "40px");
            chart.add(noData);
        } else {
            // Trier par date
            List<Map.Entry<String, Double>> sortedEntries = revenueByMonth.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toList());

            double maxRevenue = sortedEntries.stream()
                    .map(Map.Entry::getValue)
                    .max(Double::compareTo)
                    .orElse(1.0);

            // Créer le canvas SVG pour le graphique linéaire
            int width = 100;
            int height = 250;
            int pointCount = sortedEntries.size();

            // Points du graphique
            StringBuilder svgPath = new StringBuilder("M ");
            StringBuilder svgPoints = new StringBuilder();

            for (int i = 0; i < pointCount; i++) {
                Map.Entry<String, Double> entry = sortedEntries.get(i);
                double value = entry.getValue();

                double x = (double) i / (pointCount - 1) * 100;
                double y = 100 - (value / maxRevenue * 80); // Inverser Y et scale à 80%

                if (i == 0) {
                    svgPath.append(x).append(" ").append(y);
                } else {
                    svgPath.append(" L ").append(x).append(" ").append(y);
                }

                // Créer un point avec tooltip
                svgPoints.append(String.format(
                        "<circle cx='%.1f%%' cy='%.1f%%' r='4' fill='#2196F3' stroke='var(--lumo-base-color)' stroke-width='2'>" +
                                "<title>%s: %.2f DH</title></circle>",
                        x, y, entry.getKey(), value
                ));
            }

            // Créer le SVG
            String svg = String.format(
                    "<svg width='100%%' height='%dpx' style='overflow: visible;'>" +
                            "<defs>" +
                            "<linearGradient id='gradient' x1='0%%' y1='0%%' x2='0%%' y2='100%%'>" +
                            "<stop offset='0%%' style='stop-color:#2196F3;stop-opacity:0.3' />" +
                            "<stop offset='100%%' style='stop-color:#2196F3;stop-opacity:0' />" +
                            "</linearGradient>" +
                            "</defs>" +
                            "<path d='%s' fill='none' stroke='#2196F3' stroke-width='3' />" +
                            "<path d='%s L 100 100 L 0 100 Z' fill='url(#gradient)' />" +
                            "%s" +
                            "</svg>",
                    height,
                    svgPath.toString(),
                    svgPath.toString(),
                    svgPoints.toString()
            );

            Div svgContainer = new Div();
            svgContainer.getElement().setProperty("innerHTML", svg);
            chart.add(svgContainer);

            // Ajouter les labels en bas
            Div labelsContainer = new Div();
            labelsContainer.getStyle()
                    .set("display", "flex")
                    .set("justify-content", "space-between")
                    .set("margin-top", "10px")
                    .set("font-size", "11px")
                    .set("color", "var(--lumo-secondary-text-color)");

            // Afficher seulement le premier et dernier label si trop de points
            if (pointCount <= 6) {
                for (Map.Entry<String, Double> entry : sortedEntries) {
                    Span label = new Span(entry.getKey());
                    label.getStyle().set("flex", "1").set("text-align", "center");
                    labelsContainer.add(label);
                }
            } else {
                Span firstLabel = new Span(sortedEntries.get(0).getKey());
                Span lastLabel = new Span(sortedEntries.get(pointCount - 1).getKey());
                labelsContainer.add(firstLabel, lastLabel);
            }

            chart.add(labelsContainer);
        }

        chartContainer.add(chart);
        addChartToContainer(chartContainer);
    }

    /**
     * Graphique des réservations par catégorie (Bonus)
     */
    private void createReservationsByCategoryChart(List<Event> events) {
        Div chartContainer = createChartContainer("🎭 Réservations par Catégorie");

        // Calculer les réservations par catégorie
        Map<String, Long> reservationsByCategory = events.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategorie().getLabel(),
                        Collectors.summingLong(e ->
                                e.getReservations().stream()
                                        .filter(r -> r.getStatut() != ReservationStatut.ANNULEE)
                                        .count()
                        )
                ));

        VerticalLayout chart = new VerticalLayout();
        chart.setPadding(false);
        chart.setSpacing(true);
        chart.getStyle().set("width", "100%");

        if (reservationsByCategory.isEmpty()) {
            Span noData = new Span("Aucune donnée disponible");
            noData.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("text-align", "center")
                    .set("display", "block")
                    .set("padding", "40px");
            chart.add(noData);
        } else {
            long maxReservations = reservationsByCategory.values().stream()
                    .max(Long::compareTo)
                    .orElse(1L);

            String[] colors = {"#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#F44336"};
            int colorIndex = 0;

            for (Map.Entry<String, Long> entry : reservationsByCategory.entrySet()) {
                chart.add(createBarChartRow(
                        entry.getKey(),
                        entry.getValue().doubleValue(),
                        maxReservations,
                        colors[colorIndex % colors.length]
                ));
                colorIndex++;
            }
        }

        chartContainer.add(chart);
        addChartToContainer(chartContainer);
    }

    /**
     * Graphique du statut des événements
     */
    private void createEventStatusChart(Map<EventStatut, Long> eventsByStatus) {
        Div chartContainer = createChartContainer("📊 Distribution des Événements");

        VerticalLayout chart = new VerticalLayout();
        chart.setPadding(false);
        chart.setSpacing(true);
        chart.getStyle().set("width", "100%");

        long total = eventsByStatus.values().stream().mapToLong(Long::longValue).sum();

        if (total == 0) {
            Span noData = new Span("Aucun événement");
            noData.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("text-align", "center")
                    .set("display", "block")
                    .set("padding", "40px");
            chart.add(noData);
        } else {
            for (EventStatut statut : EventStatut.values()) {
                long count = eventsByStatus.getOrDefault(statut, 0L);
                if (count > 0) {
                    chart.add(createBarChartRow(
                            statut.getLabel(),
                            count,
                            total,
                            statut.getColor()
                    ));
                }
            }
        }

        chartContainer.add(chart);
        addChartToContainer(chartContainer);
    }

    /**
     * Crée un conteneur pour un graphique
     */
    private Div createChartContainer(String title) {
        Div container = new Div();
        container.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("padding", "24px")
                .set("box-shadow", "0 2px 8px var(--lumo-shade-10pct)");

        H3 chartTitle = new H3(title);
        chartTitle.getStyle()
                .set("margin", "0 0 20px 0")
                .set("font-size", "18px")
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "600");

        container.add(chartTitle);
        return container;
    }

    /**
     * Crée une barre de graphique
     */
    private HorizontalLayout createBarChartRow(String label, double value, double max, String color) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.getStyle().set("margin-bottom", "12px");

        // Label
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("min-width", "120px")
                .set("font-size", "13px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "500");

        // Barre de progression
        Div barContainer = new Div();
        barContainer.getStyle()
                .set("flex", "1")
                .set("height", "32px")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("border-radius", "6px")
                .set("position", "relative")
                .set("overflow", "hidden");

        double percentage = max > 0 ? (value / max) * 100 : 0;

        Div bar = new Div();
        bar.getStyle()
                .set("width", percentage + "%")
                .set("height", "100%")
                .set("background", color)
                .set("transition", "width 0.5s ease")
                .set("border-radius", "6px");

        barContainer.add(bar);

        // Valeur
        Span valueSpan = new Span(String.format("%.0f", value));
        valueSpan.getStyle()
                .set("min-width", "60px")
                .set("text-align", "right")
                .set("font-weight", "700")
                .set("color", color)
                .set("font-size", "14px");

        row.add(labelSpan, barContainer, valueSpan);
        return row;
    }

    /**
     * Ajoute un graphique au conteneur principal
     */
    private void addChartToContainer(Div chart) {
        Div chartsContainer = (Div) getChildren()
                .filter(c -> c.getId().orElse("").equals("chartsContainer"))
                .findFirst()
                .orElse(null);

        if (chartsContainer != null) {
            chartsContainer.add(chart);
        }
    }

    private void createUserRoleStats(Map<UserRole, Long> usersByRole) {
        Div container = new Div();
        container.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "8px")
                .set("padding", "20px")
                .set("box-shadow", "0 2px 4px var(--lumo-shade-10pct)")
                .set("flex", "1")
                .set("min-width", "300px");

        H2 title = new H2("👥 Utilisateurs par Rôle");
        title.getStyle()
                .set("font-size", "18px")
                .set("margin-top", "0")
                .set("color", "var(--lumo-primary-text-color)");

        VerticalLayout stats = new VerticalLayout();
        stats.setSpacing(true);
        stats.setPadding(false);

        for (UserRole role : UserRole.values()) {
            long count = usersByRole.getOrDefault(role, 0L);
            stats.add(createStatRow(role.getLabel(), count, getColorForRole(role)));
        }

        container.add(title, stats);

        Div detailsContainer = (Div) getChildren()
                .filter(c -> c.getId().orElse("").equals("detailsContainer"))
                .findFirst()
                .orElse(null);

        if (detailsContainer != null) {
            detailsContainer.add(container);
        }
    }

    private void createEventStatusStats(Map<EventStatut, Long> eventsByStatus) {
        Div container = new Div();
        container.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "8px")
                .set("padding", "20px")
                .set("box-shadow", "0 2px 4px var(--lumo-shade-10pct)")
                .set("flex", "1")
                .set("min-width", "300px");

        H2 title = new H2("📅 Événements par Statut");
        title.getStyle()
                .set("font-size", "18px")
                .set("margin-top", "0")
                .set("color", "var(--lumo-primary-text-color)");

        VerticalLayout stats = new VerticalLayout();
        stats.setSpacing(true);
        stats.setPadding(false);

        for (EventStatut statut : EventStatut.values()) {
            long count = eventsByStatus.getOrDefault(statut, 0L);
            stats.add(createStatRow(statut.getLabel(), count, statut.getColor()));
        }

        container.add(title, stats);

        Div detailsContainer = (Div) getChildren()
                .filter(c -> c.getId().orElse("").equals("detailsContainer"))
                .findFirst()
                .orElse(null);

        if (detailsContainer != null) {
            detailsContainer.add(container);
        }
    }

    private void createReservationStats(List<Reservation> reservations) {
        Div container = new Div();
        container.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "8px")
                .set("padding", "20px")
                .set("box-shadow", "0 2px 4px var(--lumo-shade-10pct)")
                .set("flex", "1")
                .set("min-width", "300px");

        H2 title = new H2("🎫 Statistiques de Réservations");
        title.getStyle()
                .set("font-size", "18px")
                .set("margin-top", "0")
                .set("color", "var(--lumo-primary-text-color)");

        VerticalLayout stats = new VerticalLayout();
        stats.setSpacing(true);
        stats.setPadding(false);

        Map<ReservationStatut, Long> byStatus = reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getStatut, Collectors.counting()));

        for (ReservationStatut statut : ReservationStatut.values()) {
            long count = byStatus.getOrDefault(statut, 0L);
            stats.add(createStatRow(statut.getLabel(), count, statut.getColor()));
        }

        container.add(title, stats);

        Div detailsContainer = (Div) getChildren()
                .filter(c -> c.getId().orElse("").equals("detailsContainer"))
                .findFirst()
                .orElse(null);

        if (detailsContainer != null) {
            detailsContainer.add(container);
        }
    }

    private HorizontalLayout createStatRow(String label, long value, String color) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.getStyle()
                .set("padding", "8px 0")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Div valueContainer = new Div();
        valueContainer.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "8px");

        Div colorDot = new Div();
        colorDot.getStyle()
                .set("width", "8px")
                .set("height", "8px")
                .set("border-radius", "50%")
                .set("background", color);

        Span valueSpan = new Span(String.valueOf(value));
        valueSpan.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        valueContainer.add(colorDot, valueSpan);

        row.add(labelSpan, valueContainer);
        return row;
    }

    private String getColorForRole(UserRole role) {
        return switch (role) {
            case ADMIN -> "#F44336";
            case ORGANIZER -> "#2196F3";
            case CLIENT -> "#4CAF50";
        };
    }
}