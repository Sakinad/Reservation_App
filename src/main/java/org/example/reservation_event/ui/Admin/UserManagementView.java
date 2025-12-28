package org.example.reservation_event.ui.Admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.example.reservation_event.Enums.UserRole;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.services.UserService;
import org.example.reservation_event.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * UserManagementView - Gestion des utilisateurs
 *
 * Fonctionnalités :
 * - Grille de tous les utilisateurs
 * - Filtres par rôle et statut (actif/inactif)
 * - Affichage : nom, email, rôle, date inscription, statut
 * - Actions : voir détails, activer/désactiver, changer rôle
 * - Recherche par nom ou email
 * - Pagination
 */
@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("Gestion des Utilisateurs | Event Manager")
@RolesAllowed("ADMIN")
public class UserManagementView extends VerticalLayout {

    private final UserService userService;

    // Composants UI
    private Grid<User> grid;
    private TextField searchField;
    private ComboBox<UserRole> roleFilter;
    private ComboBox<String> statusFilter;

    // Filtres actuels
    private UserRole currentRoleFilter = null;
    private Boolean currentStatusFilter = null;
    private String currentSearch = "";

    @Autowired
    public UserManagementView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createFilters();
        createGrid();

        loadUsers();
    }

    private void createHeader() {
        H1 title = new H1("Gestion des Utilisateurs");
        title.getStyle()
                .set("margin", "0")
                .set("color", "#1976D2");

        Span subtitle = new Span("Gérer tous les utilisateurs de la plateforme");
        subtitle.getStyle()
                .set("color", "#666")
                .set("font-size", "14px");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setSpacing(false);
        header.setPadding(false);

        add(header);
    }

    private void createFilters() {
        // Champ de recherche
        searchField = new TextField();
        searchField.setPlaceholder("Rechercher par nom ou email...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidth("300px");
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            loadUsers();
        });

        // Filtre par rôle
        roleFilter = new ComboBox<>("Rôle");
        roleFilter.setItems(UserRole.values());
        roleFilter.setItemLabelGenerator(UserRole::getLabel);
        roleFilter.setPlaceholder("Tous les rôles");
        roleFilter.setClearButtonVisible(true);
        roleFilter.addValueChangeListener(e -> {
            currentRoleFilter = e.getValue();
            loadUsers();
        });

        // Filtre par statut
        statusFilter = new ComboBox<>("Statut");
        statusFilter.setItems("Tous", "Actifs", "Inactifs");
        statusFilter.setValue("Tous");
        statusFilter.addValueChangeListener(e -> {
            String value = e.getValue();
            if ("Actifs".equals(value)) {
                currentStatusFilter = true;
            } else if ("Inactifs".equals(value)) {
                currentStatusFilter = false;
            } else {
                currentStatusFilter = null;
            }
            loadUsers();
        });

        // Bouton rafraîchir
        Button refreshButton = new Button("Rafraîchir", VaadinIcon.REFRESH.create());
        refreshButton.addClickListener(e -> loadUsers());

        HorizontalLayout filters = new HorizontalLayout(
                searchField, roleFilter, statusFilter, refreshButton
        );
        filters.setAlignItems(Alignment.END);
        filters.getStyle().set("margin-top", "20px");

        add(filters);
    }

    private void createGrid() {
        grid = new Grid<>(User.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("600px");

        // Colonne Nom complet
        grid.addColumn(user -> user.getPrenom() + " " + user.getNom())
                .setHeader("Nom Complet")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Email
        grid.addColumn(User::getEmail)
                .setHeader("Email")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Rôle avec badge
        grid.addColumn(new ComponentRenderer<>(user -> {
                    Span badge = new Span(user.getRole().getLabel());
                    badge.getElement().getThemeList().add("badge");

                    String color = switch (user.getRole()) {
                        case ADMIN -> "error";
                        case ORGANIZER -> "primary";
                        case CLIENT -> "success";
                    };
                    badge.getElement().getThemeList().add(color);

                    return badge;
                }))
                .setHeader("Rôle")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Date d'inscription
        grid.addColumn(user -> user.getDateInscription()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setHeader("Date Inscription")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Statut avec badge
        grid.addColumn(new ComponentRenderer<>(user -> {
                    Span badge = new Span(user.getActif() ? "Actif" : "Inactif");
                    badge.getElement().getThemeList().add("badge");
                    badge.getElement().getThemeList().add(user.getActif() ? "success" : "error");
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

    private Component createActionsLayout(User user) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        // Bouton Détails
        Button detailsButton = new Button(VaadinIcon.EYE.create());
        detailsButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        detailsButton.getElement().setAttribute("title", "Voir les détails");
        detailsButton.addClickListener(e -> showUserDetails(user));

        // Bouton Activer/Désactiver
        Button toggleButton = new Button(
                user.getActif() ? VaadinIcon.BAN.create() : VaadinIcon.CHECK_CIRCLE.create()
        );
        toggleButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        toggleButton.addThemeVariants(user.getActif() ?
                ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS);
        toggleButton.getElement().setAttribute("title",
                user.getActif() ? "Désactiver" : "Activer");
        toggleButton.addClickListener(e -> toggleUserStatus(user));

        // Bouton Changer rôle
        Button roleButton = new Button(VaadinIcon.USER_STAR.create());
        roleButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        roleButton.getElement().setAttribute("title", "Changer le rôle");
        roleButton.addClickListener(e -> changeUserRole(user));

        actions.add(detailsButton, toggleButton, roleButton);
        return actions;
    }

    private void loadUsers() {
        try {
            List<User> users = userService.listUsers(
                    currentRoleFilter,
                    currentStatusFilter,
                    currentSearch
            );
            grid.setItems(users);
        } catch (Exception e) {
            showErrorNotification("Erreur lors du chargement des utilisateurs: " + e.getMessage());
        }
    }

    private void showUserDetails(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Détails de l'utilisateur");
        dialog.setWidth("500px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        TextField nomField = new TextField("Nom");
        nomField.setValue(user.getNom());
        nomField.setReadOnly(true);

        TextField prenomField = new TextField("Prénom");
        prenomField.setValue(user.getPrenom());
        prenomField.setReadOnly(true);

        TextField emailField = new TextField("Email");
        emailField.setValue(user.getEmail());
        emailField.setReadOnly(true);

        TextField roleField = new TextField("Rôle");
        roleField.setValue(user.getRole().getLabel());
        roleField.setReadOnly(true);

        TextField telephoneField = new TextField("Téléphone");
        telephoneField.setValue(user.getTelephone() != null ? user.getTelephone() : "Non renseigné");
        telephoneField.setReadOnly(true);

        TextField inscriptionField = new TextField("Date d'inscription");
        inscriptionField.setValue(user.getDateInscription()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        inscriptionField.setReadOnly(true);

        TextField statutField = new TextField("Statut");
        statutField.setValue(user.getActif() ? "Actif" : "Inactif");
        statutField.setReadOnly(true);

        form.add(nomField, prenomField, emailField, telephoneField,
                roleField, inscriptionField, statutField);
        form.setColspan(emailField, 2);
        form.setColspan(inscriptionField, 2);

        // Statistiques
        try {
            var stats = userService.getUserStats(user.getId());

            Div statsDiv = new Div();
            statsDiv.getStyle()
                    .set("margin-top", "20px")
                    .set("padding", "15px")
                    .set("background", "#f5f5f5")
                    .set("border-radius", "4px");

            Span statsTitle = new Span("Statistiques");
            statsTitle.getStyle()
                    .set("font-weight", "bold")
                    .set("display", "block")
                    .set("margin-bottom", "10px");

            Span eventsCreated = new Span("Événements créés: " + stats.get("eventsCreated"));
            Span reservations = new Span("Réservations: " + stats.get("reservations"));
            Span totalSpent = new Span(String.format("Montant dépensé: %.2f DH",
                    (Double) stats.get("totalSpent")));

            eventsCreated.getStyle().set("display", "block");
            reservations.getStyle().set("display", "block");
            totalSpent.getStyle().set("display", "block");

            statsDiv.add(statsTitle, eventsCreated, reservations, totalSpent);

            VerticalLayout content = new VerticalLayout(form, statsDiv);
            content.setPadding(false);
            dialog.add(content);
        } catch (Exception e) {
            dialog.add(form);
        }

        Button closeButton = new Button("Fermer", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeButton);

        dialog.open();
    }

    private void toggleUserStatus(User user) {
        String action = user.getActif() ? "désactiver" : "activer";

        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Confirmer l'action");
        confirmDialog.setText("Voulez-vous vraiment " + action + " cet utilisateur ?");

        confirmDialog.setCancelable(true);
        confirmDialog.setCancelText("Annuler");

        confirmDialog.setConfirmText("Confirmer");
        confirmDialog.setConfirmButtonTheme("primary");

        confirmDialog.addConfirmListener(e -> {
            try {
                userService.setActive(user.getId(), !user.getActif());
                showSuccessNotification("Utilisateur " + action + " avec succès");
                loadUsers();
            } catch (Exception ex) {
                showErrorNotification("Erreur: " + ex.getMessage());
            }
        });

        confirmDialog.open();
    }

    private void changeUserRole(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Changer le rôle");
        dialog.setWidth("400px");

        ComboBox<UserRole> roleCombo = new ComboBox<>("Nouveau rôle");
        roleCombo.setItems(UserRole.values());
        roleCombo.setItemLabelGenerator(UserRole::getLabel);
        roleCombo.setValue(user.getRole());
        roleCombo.setWidthFull();

        Span warning = new Span("⚠️ Cette action modifiera les droits d'accès de l'utilisateur");
        warning.getStyle()
                .set("color", "#FF9800")
                .set("font-size", "14px")
                .set("display", "block")
                .set("margin-top", "10px");

        VerticalLayout content = new VerticalLayout(roleCombo, warning);
        content.setPadding(false);
        dialog.add(content);

        Button cancelButton = new Button("Annuler", e -> dialog.close());

        Button confirmButton = new Button("Confirmer", e -> {
            UserRole newRole = roleCombo.getValue();
            if (newRole != null && newRole != user.getRole()) {
                try {
                    // ✅ UTILISER LA NOUVELLE MÉTHODE
                    userService.changeUserRole(user.getId(), newRole);

                    showSuccessNotification("Rôle modifié avec succès");
                    loadUsers();
                    dialog.close();
                } catch (Exception ex) {
                    showErrorNotification("Erreur: " + ex.getMessage());
                }
            }
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelButton, confirmButton);
        dialog.open();
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