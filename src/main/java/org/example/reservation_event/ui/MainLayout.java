package org.example.reservation_event.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.Lumo;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.config.SecurityUtils;
import org.example.reservation_event.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;

public class MainLayout extends AppLayout implements BeforeEnterObserver {

    private static final String DARK_MODE_KEY = "dark-mode";

    private final UserService userService;
    private User currentUser;
    private String currentRoute;

    // Composants qui seront mis à jour
    private VerticalLayout menuContainer;

    @Autowired
    public MainLayout(UserService userService) {
        this.userService = userService;

        loadCurrentUser();   // 👤 charger utilisateur EN PREMIER
        applyTheme();        // 🌙 appliquer le thème
        createHeader();      // 🧭 header
        createDrawer();      // 📋 menu
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentRoute = event.getLocation().getPath();
        // Recharger l'utilisateur et le menu
        loadCurrentUser();
        updateMenu();
    }

    // =========================
    // HEADER - Design moderne
    // =========================

    private void createHeader() {
        // Toggle du menu
        DrawerToggle toggle = new DrawerToggle();
        toggle.getStyle()
                .set("color", "var(--lumo-primary-text-color)");

        // Logo et titre avec icône
        HorizontalLayout logoSection = new HorizontalLayout();
        logoSection.setAlignItems(FlexComponent.Alignment.CENTER);
        logoSection.setSpacing(true);

        Icon logoIcon = VaadinIcon.CALENDAR_CLOCK.create();
        logoIcon.setSize("28px");
        logoIcon.getStyle()
                .set("color", "var(--lumo-primary-color)");

        H2 title = new H2("EventBooking");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "700")
                .set("font-size", "1.5rem");

        logoSection.add(logoIcon, title);

        // Section droite avec infos utilisateur et boutons
        HorizontalLayout rightSection = new HorizontalLayout();
        rightSection.setAlignItems(FlexComponent.Alignment.CENTER);
        rightSection.setSpacing(true);

        // Info utilisateur avec avatar
        if (currentUser != null) {
            HorizontalLayout userInfo = new HorizontalLayout();
            userInfo.setAlignItems(FlexComponent.Alignment.CENTER);
            userInfo.setSpacing(true);
            userInfo.getStyle()
                    .set("padding", "0.5rem 1rem")
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "var(--lumo-border-radius-m)");

            Avatar avatar = new Avatar(currentUser.getPrenom() + " " + currentUser.getNom());
            avatar.setColorIndex(currentUser.getId().intValue() % 7);

            VerticalLayout userDetails = new VerticalLayout();
            userDetails.setSpacing(false);
            userDetails.setPadding(false);
            userDetails.getStyle().set("line-height", "1.2");

            Span name = new Span(currentUser.getPrenom() + " " + currentUser.getNom());
            name.getStyle()
                    .set("font-weight", "600")
                    .set("font-size", "0.9rem")
                    .set("color", "var(--lumo-primary-text-color)");

            Span role = new Span(getRoleLabel(currentUser.getRole().name()));
            role.getStyle()
                    .set("font-size", "0.75rem")
                    .set("color", "var(--lumo-secondary-text-color)");

            userDetails.add(name, role);
            userInfo.add(avatar, userDetails);
            rightSection.add(userInfo);
        }

        // Bouton Dark Mode avec animation
        Button themeToggle = new Button();
        themeToggle.setIcon(isDarkModeEnabled()
                ? VaadinIcon.SUN_O.create()
                : VaadinIcon.MOON_O.create());
        themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeToggle.getStyle()
                .set("border-radius", "50%")
                .set("transition", "all 0.3s ease");
        themeToggle.getElement().setAttribute("title", isDarkModeEnabled() ? "Mode clair" : "Mode sombre");

        themeToggle.addClickListener(e -> {
            toggleDarkMode();
            themeToggle.setIcon(isDarkModeEnabled()
                    ? VaadinIcon.SUN_O.create()
                    : VaadinIcon.MOON_O.create());
            themeToggle.getElement().setAttribute("title", isDarkModeEnabled() ? "Mode clair" : "Mode sombre");

            // Animation de rotation
            themeToggle.getElement().getStyle().set("transform", "rotate(360deg)");
            UI.getCurrent().getPage().executeJs(
                    "setTimeout(() => $0.style.transform = 'rotate(0deg)', 300)",
                    themeToggle.getElement()
            );
        });

        rightSection.add(themeToggle);

        // Bouton Déconnexion (seulement si authentifié)
        if (currentUser != null) {
            Button logout = new Button("Déconnexion", VaadinIcon.SIGN_OUT.create());
            logout.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            logout.getElement().setAttribute("title", "Se déconnecter");
            logout.addClickListener(e -> handleLogout());
            rightSection.add(logout);
        }

        // Layout principal du header
        HorizontalLayout header = new HorizontalLayout(toggle, logoSection, rightSection);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setPadding(true);
        header.getStyle()
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)")
                .set("background", "var(--lumo-base-color)");

        addToNavbar(header);
    }

    // =========================
    // DRAWER / MENU - Design amélioré
    // =========================

    private void createDrawer() {
        menuContainer = new VerticalLayout();
        menuContainer.setPadding(false);
        menuContainer.setSpacing(false);
        menuContainer.setSizeFull();
        menuContainer.getStyle()
                .set("background", "var(--lumo-base-color)");

        updateMenu();
        addToDrawer(menuContainer);
    }

    private VerticalLayout updateMenu() {
        menuContainer.removeAll();

        // DEBUG - À retirer après test
        System.out.println("=== DEBUG MENU ===");
        System.out.println("isAuthenticated: " + SecurityUtils.isAuthenticated());
        System.out.println("currentUser: " + (currentUser != null ? currentUser.getEmail() + " - " + currentUser.getRole() : "null"));
        System.out.println("hasADMIN: " + SecurityUtils.hasAuthority("ADMIN"));
        System.out.println("hasORGANIZER: " + SecurityUtils.hasAuthority("ORGANIZER"));
        System.out.println("hasCLIENT: " + SecurityUtils.hasAuthority("CLIENT"));

        // ===== Header menu =====
        VerticalLayout menuHeader = new VerticalLayout();
        menuHeader.setPadding(true);
        menuHeader.setSpacing(false);
        menuHeader.getStyle()
                .set("background", "linear-gradient(135deg, var(--lumo-primary-color) 0%, var(--lumo-primary-color-50pct) 100%)")
                .set("color", "white")
                .set("margin-bottom", "1rem");

        H3 menuTitle = new H3("📋 Navigation");
        menuTitle.getStyle()
                .set("margin", "0")
                .set("color", "white")
                .set("font-size", "1.2rem")
                .set("font-weight", "700");

        menuHeader.add(menuTitle);

        // ===== Items =====
        VerticalLayout menuItems = new VerticalLayout();
        menuItems.setPadding(true);
        menuItems.setSpacing(false);

        // 🔓 PUBLIC - Seulement si vraiment non connecté
        if (currentUser == null) {
            menuItems.add(createMenuSection("🌐 Public", new Component[]{
                    createMenuItem("Accueil", VaadinIcon.HOME, "", "Retour à l'accueil"),
                    createMenuItem("Événements", VaadinIcon.CALENDAR, "events", "Découvrir les événements"),
                    createMenuItem("Connexion", VaadinIcon.SIGN_IN, "login", "Se connecter"),
                    createMenuItem("Inscription", VaadinIcon.USER_CHECK, "register", "Créer un compte")
            }));
        }

        // Si l'utilisateur est connecté, afficher son menu selon son rôle
        if (currentUser != null) {
            String userRole = currentUser.getRole().name();

            // 👑 ADMIN
            if ("ADMIN".equals(userRole)) {
                menuItems.add(createMenuSection("👑 Administration", new Component[]{
                        createMenuItem("Dashboard", VaadinIcon.DASHBOARD, "admin/dashboard", "Vue d'ensemble"),
                        createMenuItem("Utilisateurs", VaadinIcon.USERS, "admin/users", "Gérer les utilisateurs"),
                        createMenuItem("Événements", VaadinIcon.CALENDAR, "admin/events", "Gérer les événements"),
                        createMenuItem("Réservations", VaadinIcon.TICKET, "admin/reservations", "Voir les réservations")
                }));
            }
            // 🧑‍💼 ORGANIZER
            else if ("ORGANIZER".equals(userRole)) {
                menuItems.add(createMenuSection("🧑‍💼 Organisateur", new Component[]{
                        createMenuItem("Dashboard", VaadinIcon.DASHBOARD, "organizer/dashboard", "Vue d'ensemble"),
                        createMenuItem("Mes Événements", VaadinIcon.CALENDAR, "organizer/events", "Gérer mes événements"),
                        createMenuItem("Créer événement", VaadinIcon.PLUS_CIRCLE, "organizer/event/new", "Nouvel événement"),
                        createMenuItem("Profil", VaadinIcon.USER, "organizer/profile", "Mon profil")
                }));
            }
            // 👤 CLIENT
            else if ("CLIENT".equals(userRole)) {
                menuItems.add(createMenuSection("👤 Mon Espace", new Component[]{
                        createMenuItem("Dashboard", VaadinIcon.DASHBOARD, "dashboard", "Vue d'ensemble"),
                        createMenuItem("Mes Réservations", VaadinIcon.TICKET, "my-reservations", "Voir mes réservations"),
                        createMenuItem("Événements", VaadinIcon.CALENDAR, "eventsClient", "Découvrir les événements"),
                        createMenuItem("Profil", VaadinIcon.USER, "profile", "Mon profil")
                }));
            }
        }

        // ===== Ajout FINAL =====
        menuContainer.add(menuHeader, menuItems);

        // Footer
        if (currentUser != null) {
            Div footer = new Div();
            footer.getStyle()
                    .set("padding", "1rem")
                    .set("margin-top", "auto")
                    .set("border-top", "1px solid var(--lumo-contrast-10pct)")
                    .set("font-size", "0.75rem")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("text-align", "center");
            footer.setText("EventBooking v1.0");
            menuContainer.add(footer);
        }

        return menuContainer;
    }

    private VerticalLayout createMenuSection(String title, Component[] items) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.getStyle().set("margin-bottom", "1.5rem");

        // Titre de section
        Span sectionTitle = new Span(title);
        sectionTitle.getStyle()
                .set("font-size", "0.85rem")
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.05em")
                .set("padding", "0.5rem 1rem")
                .set("display", "block");

        section.add(sectionTitle);

        for (Component item : items) {
            section.add(item);
        }

        return section;
    }

    private Component createMenuItem(String label, VaadinIcon icon, String route, String tooltip) {
        HorizontalLayout item = new HorizontalLayout();
        item.setWidthFull();
        item.setAlignItems(FlexComponent.Alignment.CENTER);
        item.setPadding(true);
        item.setSpacing(true);
        item.getStyle()
                .set("cursor", "pointer")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin", "0.25rem 0.5rem")
                .set("transition", "all 0.2s ease")
                .set("position", "relative");

        Icon itemIcon = icon.create();
        itemIcon.setSize("20px");
        itemIcon.getStyle().set("color", "var(--lumo-primary-color)");

        Span itemLabel = new Span(label);
        itemLabel.getStyle()
                .set("font-weight", "500")
                .set("color", "var(--lumo-primary-text-color)");

        item.add(itemIcon, itemLabel);

        // Style actif
        boolean isActive = currentRoute != null &&
                (currentRoute.equals(route) ||
                        (route.length() > 0 && currentRoute.startsWith(route + "/")));

        if (isActive) {
            item.getStyle()
                    .set("background", "var(--lumo-primary-color-10pct)")
                    .set("border-left", "4px solid var(--lumo-primary-color)");
            itemIcon.getStyle().set("color", "var(--lumo-primary-color)");
            itemLabel.getStyle()
                    .set("color", "var(--lumo-primary-color)")
                    .set("font-weight", "700");
        }

        // Hover effect
        item.getElement().addEventListener("mouseenter", e -> {
            if (!isActive) {
                item.getStyle()
                        .set("background", "var(--lumo-contrast-5pct)")
                        .set("padding-left", "1.2rem");
            }
        });

        item.getElement().addEventListener("mouseleave", e -> {
            if (!isActive) {
                item.getStyle()
                        .remove("background")
                        .set("padding-left", "1rem");
            }
        });

        // Navigation
        item.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(route)));

        // Tooltip avec attribut HTML standard
        if (tooltip != null && !tooltip.isEmpty()) {
            item.getElement().setAttribute("title", tooltip);
        }

        return item;
    }

    // =========================
    // UTILITAIRES
    // =========================

    private String getRoleLabel(String role) {
        switch (role) {
            case "ADMIN": return "Administrateur";
            case "ORGANIZER": return "Organisateur";
            case "CLIENT": return "Client";
            default: return role;
        }
    }

    private void loadCurrentUser() {
        currentUser = VaadinSession.getCurrent().getAttribute(User.class);

        if (currentUser == null) {
            SecurityUtils.getAuthenticatedUser().ifPresent(details -> {
                currentUser = userService.findByEmailOrNull(details.getUsername());
                if (currentUser != null) {
                    VaadinSession.getCurrent().setAttribute(User.class, currentUser);
                }
            });
        }
    }

    private void handleLogout() {
        VaadinSession.getCurrent().setAttribute(User.class, null);
        SecurityUtils.logout();

        Notification notification = Notification.show(
                "✅ Déconnexion réussie ! À bientôt.",
                3000,
                Notification.Position.TOP_CENTER
        );
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        getUI().ifPresent(ui -> ui.getPage().setLocation("/"));
    }

    // =========================
    // DARK MODE
    // =========================

    public static boolean isDarkModeEnabled() {
        Boolean dark = (Boolean) VaadinSession.getCurrent().getAttribute(DARK_MODE_KEY);
        return dark != null && dark;
    }

    public static void setDarkMode(boolean enabled) {
        VaadinSession.getCurrent().setAttribute(DARK_MODE_KEY, enabled);
        applyTheme();
    }

    public static void toggleDarkMode() {
        setDarkMode(!isDarkModeEnabled());
    }

    public static void applyTheme() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            if (isDarkModeEnabled()) {
                ui.getElement().setAttribute("theme", Lumo.DARK);
            } else {
                ui.getElement().removeAttribute("theme");
            }
        }
    }
}