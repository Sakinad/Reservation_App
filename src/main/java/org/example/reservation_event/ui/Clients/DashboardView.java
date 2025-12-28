package org.example.reservation_event.ui.Clients;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.example.reservation_event.Enums.ReservationStatut;
import org.example.reservation_event.classes.Review;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.dtos.ReservationSummaryDTO;
import org.example.reservation_event.dtos.ReviewSummaryDTO;
import org.example.reservation_event.services.EventService;
import org.example.reservation_event.services.ReservationService;
import org.example.reservation_event.services.ReviewService;
import org.example.reservation_event.services.UserService;
import org.example.reservation_event.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Tableau de bord")
public class DashboardView extends VerticalLayout {

    private final UserService userService;
    private final ReservationService reservationService;
    private final EventService eventService;
    private final ReviewService reviewService;


    @Autowired
    public DashboardView(UserService userService, ReservationService reservationService,
                         EventService eventService, ReviewService reviewService) {
        this.userService = userService;
        this.reservationService = reservationService;
        this.eventService = eventService;
        this.reviewService = reviewService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        initializeLayout();
    }

    private void initializeLayout() {
        User currentUser = getCurrentUser();

        add(createHeader(currentUser));
        add(createStatsSection(currentUser));
        add(createReviewStatsSection(currentUser));
        add(createQuickActionsSection());
        add(createUpcomingReservationsSection(currentUser));
        add(createNotificationsSection(currentUser));
    }

    private Component createHeader(User currentUser) {
        VerticalLayout header = new VerticalLayout();
        header.setSpacing(false);
        header.setPadding(false);

        String welcomeName = currentUser != null ? currentUser.getPrenom() : "Client";
        H1 welcome = new H1("Bonjour " + welcomeName + " !");
        welcome.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Span subtitle = new Span("Voici votre tableau de bord personnel");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-m)");

        Span today = new Span(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")));
        today.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("margin-top", "8px");

        header.add(welcome, subtitle, today);
        return header;
    }

    private Component createStatsSection(User currentUser) {
        HorizontalLayout statsRow = new HorizontalLayout();
        statsRow.setWidthFull();
        statsRow.setSpacing(true);

        if (currentUser != null) {
            try {
                List<ReservationSummaryDTO> upcomingReservations = reservationService.getUpcomingReservationsByUser(currentUser);
                List<ReservationSummaryDTO> allUserReservations = reservationService.getReservationsByUserAsDTO(currentUser);

                long totalReservations = allUserReservations.size();
                long upcomingEvents = upcomingReservations.size();
                double totalSpent = allUserReservations.stream()
                        .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                        .mapToDouble(ReservationSummaryDTO::getMontantTotal)
                        .sum();

                Map<String, Object> stats = Map.of(
                        "nombreReservations", totalReservations,
                        "evenementsAVenir", upcomingEvents,
                        "montantTotalDepense", totalSpent
                );

                statsRow.add(
                        createStatCard("Réservations",
                                String.valueOf(stats.get("nombreReservations")),
                                VaadinIcon.CALENDAR, "#3b82f6"),
                        createStatCard("Événements à venir",
                                String.valueOf(stats.get("evenementsAVenir")),
                                VaadinIcon.CLOCK, "#10b981"),
                        createStatCard("Total dépensé",
                                String.format("%.2f DH", stats.get("montantTotalDepense")),
                                VaadinIcon.EURO, "#8b5cf6")
                );
            } catch (Exception e) {
                statsRow.add(
                        createStatCard("Réservations", "0", VaadinIcon.CALENDAR, "#3b82f6"),
                        createStatCard("Événements à venir", "0", VaadinIcon.CLOCK, "#10b981"),
                        createStatCard("Total dépensé", "0 DH", VaadinIcon.EURO, "#8b5cf6")
                );
            }
        }

        return statsRow;
    }
    private Component createReviewStatsSection(User currentUser) {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setSpacing(true);
        section.setPadding(false);

        H2 sectionTitle = new H2("Mes avis et notes");
        sectionTitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        try {
            if (currentUser != null) {
                // ✅ Utiliser le DTO au lieu de Review
                List<ReviewSummaryDTO> userReviews = reviewService.getReviewsByUserAsDTO(currentUser.getId());

                if (userReviews.isEmpty()) {
                    Div emptyState = createReviewEmptyState();
                    section.add(sectionTitle, emptyState);
                } else {
                    // Calculer les statistiques
                    long totalReviews = userReviews.size();
                    double averageRating = userReviews.stream()
                            .mapToInt(ReviewSummaryDTO::getRating)
                            .average()
                            .orElse(0.0);

                    // Cartes statistiques
                    HorizontalLayout statsRow = new HorizontalLayout();
                    statsRow.setWidthFull();
                    statsRow.setSpacing(true);

                    statsRow.add(
                            createStatCard("Événements notés",
                                    String.valueOf(totalReviews),
                                    VaadinIcon.STAR, "#f59e0b"),
                            createStatCard("Note moyenne donnée",
                                    String.format("%.1f / 5 ⭐", averageRating),
                                    VaadinIcon.CHART, "#8b5cf6")
                    );

                    // Liste des derniers avis (max 3)
                    VerticalLayout recentReviews = new VerticalLayout();
                    recentReviews.setSpacing(true);
                    recentReviews.setPadding(false);

                    H3 recentTitle = new H3("Derniers avis laissés");
                    recentTitle.getStyle()
                            .set("margin", "16px 0 8px 0")
                            .set("color", "var(--lumo-primary-text-color)");

                    userReviews.stream()
                            .limit(3)
                            .forEach(review -> recentReviews.add(createReviewItemFromDTO(review)));

                    section.add(sectionTitle, statsRow, recentTitle, recentReviews);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement avis: " + e.getMessage());
            e.printStackTrace();
            section.add(sectionTitle, createErrorState("Impossible de charger vos avis"));
        }

        return section;
    }

    // NOUVELLE MÉTHODE : Carte d'avis à partir du DTO
    private Component createReviewItemFromDTO(ReviewSummaryDTO review) {
        HorizontalLayout item = new HorizontalLayout();
        item.setWidthFull();
        item.setPadding(true);
        item.setSpacing(true);
        item.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "8px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.05)");
        item.setAlignItems(Alignment.START);

        // Icône étoile
        Icon starIcon = new Icon(VaadinIcon.STAR);
        starIcon.setSize("20px");
        starIcon.setColor("#f59e0b");

        // Contenu
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        content.setWidthFull();

        // Titre de l'événement + note
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        Span eventName = new Span(review.getEventTitle());
        eventName.getStyle()
                .set("font-weight", "600")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "var(--lumo-font-size-m)");

        // Affichage des étoiles
        HorizontalLayout stars = new HorizontalLayout();
        stars.setSpacing(false);
        stars.getStyle().set("gap", "2px");

        for (int i = 0; i < 5; i++) {
            Icon star = new Icon(i < review.getRating() ? VaadinIcon.STAR : VaadinIcon.STAR_O);
            star.setSize("14px");
            star.setColor("#f59e0b");
            stars.add(star);
        }

        header.add(eventName, stars);

        // Commentaire (si existe)
        if (review.hasComment()) {
            Span comment = new Span(review.getComment());
            comment.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("margin-top", "4px")
                    .set("font-style", "italic");
            content.add(header, comment);
        } else {
            content.add(header);
        }

        // Date
        Span date = new Span(review.getFormattedDate());
        date.getStyle()
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("margin-top", "4px");
        content.add(date);

        item.add(starIcon, content);
        item.expand(content);

        return item;
    }

    // État vide
    private Div createReviewEmptyState() {
        Div emptyState = new Div();
        emptyState.getStyle()
                .set("text-align", "center")
                .set("padding", "40px 20px")
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("border", "2px dashed var(--lumo-contrast-20pct)");

        Icon emptyIcon = new Icon(VaadinIcon.STAR_O);
        emptyIcon.setSize("48px");
        emptyIcon.setColor("var(--lumo-contrast-40pct)");

        H3 emptyTitle = new H3("Aucun avis pour le moment");
        emptyTitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "16px 0 8px 0");

        Span emptyDesc = new Span("Après avoir assisté à un événement, revenez donner votre avis !");
        emptyDesc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button actionBtn = new Button("Voir mes réservations",
                e -> getUI().ifPresent(ui -> ui.navigate("my-reservations")));
        actionBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        emptyState.add(emptyIcon, emptyTitle, emptyDesc, actionBtn);
        return emptyState;
    }

    // État d'erreur
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
    private Component createStatCard(String title, String value, VaadinIcon icon, String color) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 6px rgba(0,0,0,0.05)")
                .set("border-left", "4px solid " + color);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        Icon cardIcon = new Icon(icon);
        cardIcon.setColor(color);

        Span label = new Span(title);
        label.getStyle()
                .set("font-size", "14px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "500");

        header.add(cardIcon, label);

        H2 val = new H2(value);
        val.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("margin", "10px 0 0 0")
                .set("font-size", "var(--lumo-font-size-xxl)");

        card.add(header, val);
        return card;
    }

    private Component createQuickActionsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setSpacing(true);
        section.setPadding(false);

        H2 sectionTitle = new H2("Accès rapide");
        sectionTitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        HorizontalLayout actionsRow = new HorizontalLayout();
        actionsRow.setWidthFull();
        actionsRow.setSpacing(true);

        actionsRow.add(
                createQuickActionCard("Réserver un événement", "Trouvez et réservez de nouveaux événements",
                        VaadinIcon.PLUS, "#4f46e5", "eventsClient"),
                createQuickActionCard("Mes réservations", "Consultez toutes vos réservations",
                        VaadinIcon.LIST, "#059669", "my-reservations"),
                createQuickActionCard("Mon profil", "Gérez vos informations personnelles",
                        VaadinIcon.USER, "#dc2626", "profile")
        );

        section.add(sectionTitle, actionsRow);
        return section;
    }

    private Component createQuickActionCard(String title, String description, VaadinIcon icon, String color, String route) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(true);
        card.setWidthFull();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 6px rgba(0,0,0,0.05)")
                .set("cursor", "pointer")
                .set("transition", "transform 0.2s, box-shadow 0.2s");

        Icon cardIcon = new Icon(icon);
        cardIcon.setSize("24px");
        cardIcon.setColor(color);

        VerticalLayout textContent = new VerticalLayout();
        textContent.setSpacing(false);
        textContent.setPadding(false);

        H3 cardTitle = new H3(title);
        cardTitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Span cardDesc = new Span(description);
        cardDesc.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        textContent.add(cardTitle, cardDesc);
        card.add(cardIcon, textContent);

        card.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(route)));

        return card;
    }

    private Component createUpcomingReservationsSection(User currentUser) {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setSpacing(true);
        section.setPadding(false);

        H2 sectionTitle = new H2("Mes prochaines réservations");
        sectionTitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        try {
            if (currentUser != null) {
                List<ReservationSummaryDTO> upcomingReservations = reservationService.getUpcomingReservationsByUser(currentUser);

                if (upcomingReservations.isEmpty()) {
                    Div emptyState = createEmptyState("Aucune réservation à venir",
                            "Explorez nos événements pour faire votre première réservation !",
                            VaadinIcon.CALENDAR);
                    section.add(sectionTitle, emptyState);
                } else {
                    VerticalLayout reservationsList = new VerticalLayout();
                    reservationsList.setSpacing(true);
                    reservationsList.setPadding(false);

                    upcomingReservations.stream().limit(3).forEach(reservation -> {
                        reservationsList.add(createReservationItem(reservation));
                    });

                    if (upcomingReservations.size() > 3) {
                        Button seeAllBtn = new Button("Voir toutes mes réservations",
                                e -> getUI().ifPresent(ui -> ui.navigate("my-reservations")));
                        seeAllBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                        section.add(sectionTitle, reservationsList, seeAllBtn);
                    } else {
                        section.add(sectionTitle, reservationsList);
                    }
                }
            }
        } catch (Exception e) {
            section.add(sectionTitle,
                    createEmptyState("Erreur de chargement",
                            "Impossible de charger vos réservations pour le moment.",
                            VaadinIcon.WARNING));
        }

        return section;
    }

    private Component createReservationItem(ReservationSummaryDTO reservation) {
        HorizontalLayout item = new HorizontalLayout();
        item.setWidthFull();
        item.setPadding(true);
        item.setSpacing(true);
        item.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "8px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.05)");
        item.setAlignItems(Alignment.CENTER);

        Icon statusIcon = new Icon(VaadinIcon.CIRCLE);
        statusIcon.setSize("12px");
        statusIcon.setColor(getStatusColor(reservation.getStatut()));

        VerticalLayout info = new VerticalLayout();
        info.setSpacing(false);
        info.setPadding(false);

        Span eventName = new Span(reservation.getEvenementTitre());
        eventName.getStyle()
                .set("font-weight", "500")
                .set("color", "var(--lumo-primary-text-color)");

        HorizontalLayout details = new HorizontalLayout();
        details.setSpacing(true);
        details.setAlignItems(Alignment.CENTER);

        Span date = new Span(reservation.getEvenementDateDebut().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        date.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Span places = new Span(reservation.getNombrePlaces() + " place(s)");
        places.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Span amount = new Span(String.format("%.2f DH", reservation.getMontantTotal()));
        amount.getStyle()
                .set("color", "#059669")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "500");

        details.add(date, new Span("•"), places, new Span("•"), amount);
        info.add(eventName, details);

        Span code = new Span(reservation.getCodeReservation());
        code.getStyle()
                .set("font-family", "monospace")
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-s)");

        item.add(statusIcon, info);
        item.expand(info);
        item.add(code);

        return item;
    }

    private Component createNotificationsSection(User currentUser) {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setSpacing(true);
        section.setPadding(false);

        H2 sectionTitle = new H2("Notifications importantes");
        sectionTitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        VerticalLayout notificationsList = new VerticalLayout();
        notificationsList.setSpacing(true);
        notificationsList.setPadding(false);

        try {
            if (currentUser != null) {
                List<ReservationSummaryDTO> userReservations = reservationService.getReservationsByUserAsDTO(currentUser);

                long pendingReservations = userReservations.stream()
                        .filter(r -> r.getStatut() == ReservationStatut.EN_ATTENTE)
                        .count();

                if (pendingReservations > 0) {
                    notificationsList.add(createNotificationItem(
                            "Réservations en attente",
                            "Vous avez " + pendingReservations + " réservation(s) en attente de confirmation",
                            VaadinIcon.CLOCK,
                            "warning"
                    ));
                }

                long upcomingIn24h = userReservations.stream()
                        .filter(r -> r.getStatut() == ReservationStatut.CONFIRMEE)
                        .filter(r -> r.getEvenementDateDebut().isBefore(LocalDateTime.now().plusDays(1)))
                        .filter(r -> r.getEvenementDateDebut().isAfter(LocalDateTime.now()))
                        .count();

                if (upcomingIn24h > 0) {
                    notificationsList.add(createNotificationItem(
                            "Événements bientôt",
                            "Vous avez " + upcomingIn24h + " événement(s) dans les 24h",
                            VaadinIcon.EXCLAMATION_CIRCLE,
                            "info"
                    ));
                }
            }

            notificationsList.add(createNotificationItem(
                    "Nouveaux événements disponibles",
                    "Découvrez les derniers événements ajoutés à la plateforme",
                    VaadinIcon.BELL,
                    "success"
            ));

        } catch (Exception e) {
            notificationsList.add(createNotificationItem(
                    "Système temporairement indisponible",
                    "Certaines fonctionnalités peuvent être limitées",
                    VaadinIcon.WARNING,
                    "error"
            ));
        }

        if (notificationsList.getComponentCount() == 0) {
            notificationsList.add(createNotificationItem(
                    "Aucune notification",
                    "Vous êtes à jour avec toutes vos activités",
                    VaadinIcon.CHECK,
                    "success"
            ));
        }

        section.add(sectionTitle, notificationsList);
        return section;
    }

    private Component createNotificationItem(String title, String message, VaadinIcon icon, String type) {
        HorizontalLayout notification = new HorizontalLayout();
        notification.setWidthFull();
        notification.setPadding(true);
        notification.setSpacing(true);
        notification.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "8px")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.05)");
        notification.setAlignItems(Alignment.START);

        Icon notificationIcon = new Icon(icon);
        notificationIcon.setColor(getNotificationColor(type));

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);

        Span notificationTitle = new Span(title);
        notificationTitle.getStyle()
                .set("font-weight", "500")
                .set("color", "var(--lumo-primary-text-color)");

        Span notificationMessage = new Span(message);
        notificationMessage.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        content.add(notificationTitle, notificationMessage);
        notification.add(notificationIcon, content);
        notification.expand(content);

        return notification;
    }

    private Div createEmptyState(String title, String description, VaadinIcon icon) {
        Div emptyState = new Div();
        emptyState.getStyle()
                .set("text-align", "center")
                .set("padding", "40px 20px")
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("border", "2px dashed var(--lumo-contrast-20pct)");

        Icon emptyIcon = new Icon(icon);
        emptyIcon.setSize("48px");
        emptyIcon.setColor("var(--lumo-contrast-40pct)");

        H3 emptyTitle = new H3(title);
        emptyTitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "16px 0 8px 0");

        Span emptyDesc = new Span(description);
        emptyDesc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button actionBtn = new Button("Explorer les événements",
                e -> getUI().ifPresent(ui -> ui.navigate("eventsClient")));
        actionBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        emptyState.add(emptyIcon, emptyTitle, emptyDesc, actionBtn);
        return emptyState;
    }

    private String getStatusColor(ReservationStatut statut) {
        switch (statut) {
            case CONFIRMEE: return "#10b981";
            case EN_ATTENTE: return "#f59e0b";
            case ANNULEE: return "#ef4444";
            default: return "#6b7280";
        }
    }

    private String getNotificationColor(String type) {
        switch (type) {
            case "success": return "#10b981";
            case "warning": return "#f59e0b";
            case "error": return "#ef4444";
            case "info":
            default: return "#3b82f6";
        }
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
                User realUser = userService.findByEmail(username);
                if (realUser != null) {
                    VaadinSession.getCurrent().setAttribute(User.class, realUser);
                    return realUser;
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur getCurrentUser: " + e.getMessage());
        }
        return null;
    }
}