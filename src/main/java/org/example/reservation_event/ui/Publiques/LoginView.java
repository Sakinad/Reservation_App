package org.example.reservation_event.ui.Publiques;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.Lumo;
import org.example.reservation_event.Enums.UserRole;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.repositories.UserRepository;
import org.example.reservation_event.services.PasswordResetService;
import org.example.reservation_event.services.UserService;
import org.example.reservation_event.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;

@Route("login")
@AnonymousAllowed
@PageTitle("Connexion - EventBooking")
public class LoginView extends VerticalLayout {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;

    private TextField emailField;
    private PasswordField passwordField;
    private Button loginButton;
    private Button registerButton;
    private Button forgotPasswordLink;

    @Autowired
    public LoginView(UserService userService, UserRepository userRepository, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordResetService = passwordResetService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Appliquer le thème actuel
        MainLayout.applyTheme();

        getStyle()
                .set("background", "linear-gradient(135deg, var(--lumo-primary-color) 0%, var(--lumo-primary-color-50pct) 100%)")
                .set("overflow-y", "auto");

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        createLoginCard();
        createThemeToggle();
    }

    private void createThemeToggle() {
        // Bouton Dark Mode flottant en haut à droite
        Button themeToggle = new Button();
        themeToggle.setIcon(MainLayout.isDarkModeEnabled()
                ? VaadinIcon.SUN_O.create()
                : VaadinIcon.MOON_O.create());
        themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_CONTRAST);
        themeToggle.getStyle()
                .set("position", "absolute")
                .set("top", "20px")
                .set("right", "20px")
                .set("border-radius", "50%")
                .set("background", "rgba(255, 255, 255, 0.2)")
                .set("transition", "all 0.3s ease");
        themeToggle.getElement().setAttribute("title", MainLayout.isDarkModeEnabled() ? "Mode clair" : "Mode sombre");

        themeToggle.addClickListener(e -> {
            MainLayout.toggleDarkMode();
            themeToggle.setIcon(MainLayout.isDarkModeEnabled()
                    ? VaadinIcon.SUN_O.create()
                    : VaadinIcon.MOON_O.create());
            themeToggle.getElement().setAttribute("title", MainLayout.isDarkModeEnabled() ? "Mode clair" : "Mode sombre");

            // Recharger la page pour appliquer le thème
            UI.getCurrent().getPage().reload();
        });

        add(themeToggle);
    }

    private void createLoginCard() {
        VerticalLayout loginCard = new VerticalLayout();
        loginCard.setWidth("450px");
        loginCard.setPadding(true);
        loginCard.setSpacing(true);
        loginCard.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "16px")
                .set("box-shadow", "var(--lumo-box-shadow-xl)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "40px");

        // Logo et titre
        HorizontalLayout logoSection = new HorizontalLayout();
        logoSection.setAlignItems(FlexComponent.Alignment.CENTER);
        logoSection.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        logoSection.setWidthFull();
        logoSection.setSpacing(true);
        logoSection.getStyle().set("margin-bottom", "10px");

        Icon logoIcon = VaadinIcon.CALENDAR_CLOCK.create();
        logoIcon.setSize("40px");
        logoIcon.getStyle().set("color", "var(--lumo-primary-color)");

        H1 title = new H1("EventBooking");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "bold")
                .set("font-size", "2em");

        logoSection.add(logoIcon, title);

        // Sous-titre
        H2 subtitle = new H2("Connexion");
        subtitle.getStyle()
                .set("margin", "0 0 10px 0")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "1.5em")
                .set("text-align", "center");

        Paragraph welcomeText = new Paragraph("Bienvenue ! Connectez-vous pour accéder à votre compte.");
        welcomeText.getStyle()
                .set("margin", "0 0 30px 0")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("font-size", "0.95em");

        // Champ Email
        emailField = new TextField("Email");
        emailField.setPlaceholder("exemple@email.com");
        emailField.setPrefixComponent(VaadinIcon.USER.create());
        emailField.setWidthFull();
        emailField.setRequired(true);
        emailField.setRequiredIndicatorVisible(true);
        emailField.getStyle().set("margin-bottom", "15px");

        // Champ Mot de passe
        passwordField = new PasswordField("Mot de passe");
        passwordField.setPlaceholder("Entrez votre mot de passe");
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordField.setWidthFull();
        passwordField.setRequired(true);
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.getStyle().set("margin-bottom", "10px");

        // Lien "Mot de passe oublié"
        forgotPasswordLink = new Button("Mot de passe oublié ?");
        forgotPasswordLink.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        forgotPasswordLink.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("margin-bottom", "20px")
                .set("padding", "0")
                .set("font-size", "0.9em");

        forgotPasswordLink.addClickListener(e -> showForgotPasswordDialog());

        // Bouton de connexion
        loginButton = new Button("Se connecter", VaadinIcon.SIGN_IN.create());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        loginButton.setWidthFull();
        loginButton.setIconAfterText(true);
        loginButton.getStyle()
                .set("font-weight", "bold")
                .set("margin-bottom", "20px");

        loginButton.addClickListener(e -> handleLogin());

        passwordField.addKeyPressListener(event -> {
            if (event.getKey().getKeys().contains("Enter")) {
                handleLogin();
            }
        });

        // Séparateur
        HorizontalLayout separator = new HorizontalLayout();
        separator.setWidthFull();
        separator.setAlignItems(FlexComponent.Alignment.CENTER);
        separator.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        separator.getStyle().set("margin", "20px 0");

        Div line1 = new Div();
        line1.getStyle()
                .set("height", "1px")
                .set("background", "var(--lumo-contrast-20pct)")
                .set("flex", "1");

        Span orText = new Span("ou");
        orText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("padding", "0 15px")
                .set("font-size", "0.9em");

        Div line2 = new Div();
        line2.getStyle()
                .set("height", "1px")
                .set("background", "var(--lumo-contrast-20pct)")
                .set("flex", "1");

        separator.add(line1, orText, line2);

        // Bouton d'inscription
        registerButton = new Button("Créer un compte", VaadinIcon.USER_CHECK.create());
        registerButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE);
        registerButton.setWidthFull();
        registerButton.setIconAfterText(true);
        registerButton.getStyle().set("font-weight", "bold");

        registerButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("register"))
        );

        // Lien retour à l'accueil
        HorizontalLayout backLayout = new HorizontalLayout();
        backLayout.setWidthFull();
        backLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        backLayout.getStyle().set("margin-top", "20px");

        Button backButton = new Button("Retour à l'accueil", VaadinIcon.ARROW_LEFT.create());
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backButton.getStyle().set("color", "var(--lumo-secondary-text-color)");
        backButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(""))
        );

        backLayout.add(backButton);

        loginCard.add(
                logoSection,
                subtitle,
                welcomeText,
                emailField,
                passwordField,
                forgotPasswordLink,
                loginButton,
                separator,
                registerButton,
                backLayout
        );

        add(loginCard);
    }

    private void showForgotPasswordDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("450px");
        dialog.setCloseOnOutsideClick(false);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        header.getStyle().set("margin-bottom", "10px");

        Icon keyIcon = VaadinIcon.KEY.create();
        keyIcon.setSize("32px");
        keyIcon.getStyle().set("color", "var(--lumo-primary-color)");

        H2 dialogTitle = new H2("Mot de passe oublié");
        dialogTitle.getStyle()
                .set("margin", "0 0 0 10px")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "1.5em");

        header.add(keyIcon, dialogTitle);

        Paragraph instructions = new Paragraph(
                "Entrez votre adresse email et nous vous enverrons un code de vérification."
        );
        instructions.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("margin", "0 0 20px 0");

        TextField resetEmailField = new TextField("Email");
        resetEmailField.setPlaceholder("exemple@email.com");
        resetEmailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        resetEmailField.setWidthFull();
        resetEmailField.getStyle().set("margin-bottom", "20px");

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setSpacing(true);

        Button cancelButton = new Button("Annuler");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> dialog.close());

        Button sendButton = new Button("Envoyer le code", VaadinIcon.PAPERPLANE.create());
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.setIconAfterText(true);
        sendButton.addClickListener(e -> {
            String email = resetEmailField.getValue();
            if (email == null || email.trim().isEmpty()) {
                Notification.show("Veuillez entrer votre email", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            sendButton.setEnabled(false);
            sendButton.setText("Envoi en cours...");

            try {
                boolean success = passwordResetService.createPasswordResetRequest(email);
                if (success) {
                    dialog.close();
                    showResetPasswordDialog(email);
                } else {
                    Notification.show("Erreur lors de l'envoi de l'email", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } finally {
                sendButton.setEnabled(true);
                sendButton.setText("Envoyer le code");
            }
        });

        buttons.add(cancelButton, sendButton);

        dialogLayout.add(header, instructions, resetEmailField, buttons);
        dialog.add(dialogLayout);
        dialog.open();
    }

    private void showResetPasswordDialog(String email) {
        Dialog dialog = new Dialog();
        dialog.setWidth("450px");
        dialog.setCloseOnOutsideClick(false);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        header.getStyle().set("margin-bottom", "10px");

        Icon shieldIcon = VaadinIcon.SHIELD.create();
        shieldIcon.setSize("32px");
        shieldIcon.getStyle().set("color", "var(--lumo-primary-color)");

        H2 dialogTitle = new H2("Réinitialiser le mot de passe");
        dialogTitle.getStyle()
                .set("margin", "0 0 0 10px")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "1.5em");

        header.add(shieldIcon, dialogTitle);

        Paragraph instructions = new Paragraph("Un code de vérification a été envoyé à " + email);
        instructions.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("margin", "0 0 20px 0");

        TextField codeField = new TextField("Code de vérification");
        codeField.setPlaceholder("Entrez le code à 6 chiffres");
        codeField.setPrefixComponent(VaadinIcon.KEY_O.create());
        codeField.setWidthFull();
        codeField.setMaxLength(6);
        codeField.setPattern("[0-9]{6}");
        codeField.getStyle().set("margin-bottom", "15px");

        PasswordField newPasswordField = new PasswordField("Nouveau mot de passe");
        newPasswordField.setPlaceholder("Minimum 6 caractères");
        newPasswordField.setPrefixComponent(VaadinIcon.LOCK.create());
        newPasswordField.setWidthFull();
        newPasswordField.getStyle().set("margin-bottom", "15px");

        PasswordField confirmPasswordField = new PasswordField("Confirmer le mot de passe");
        confirmPasswordField.setPlaceholder("Retapez votre mot de passe");
        confirmPasswordField.setPrefixComponent(VaadinIcon.LOCK.create());
        confirmPasswordField.setWidthFull();
        confirmPasswordField.getStyle().set("margin-bottom", "20px");

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setSpacing(true);

        Button cancelButton = new Button("Annuler");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> dialog.close());

        Button resetButton = new Button("Réinitialiser", VaadinIcon.CHECK_CIRCLE.create());
        resetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        resetButton.setIconAfterText(true);
        resetButton.addClickListener(e -> {
            String code = codeField.getValue();
            String newPassword = newPasswordField.getValue();
            String confirmPassword = confirmPasswordField.getValue();

            if (code == null || code.trim().isEmpty()) {
                Notification.show("Veuillez entrer le code", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (code.length() != 6) {
                Notification.show("Le code doit contenir 6 chiffres", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (newPassword == null || newPassword.trim().isEmpty()) {
                Notification.show("Veuillez entrer un nouveau mot de passe", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (newPassword.length() < 6) {
                Notification.show("Le mot de passe doit contenir au moins 6 caractères", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Notification.show("Les mots de passe ne correspondent pas", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            resetButton.setEnabled(false);
            resetButton.setText("Réinitialisation...");

            try {
                boolean success = passwordResetService.resetPassword(email, code, newPassword);
                if (success) {
                    dialog.close();
                    Notification.show("✅ Mot de passe réinitialisé avec succès !", 4000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("Code invalide ou expiré", 4000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } finally {
                resetButton.setEnabled(true);
                resetButton.setText("Réinitialiser");
            }
        });

        buttons.add(cancelButton, resetButton);

        dialogLayout.add(header, instructions, codeField, newPasswordField, confirmPasswordField, buttons);
        dialog.add(dialogLayout);
        dialog.open();
    }

    private void handleLogin() {
        String email = emailField.getValue();
        String password = passwordField.getValue();

        if (email == null || email.trim().isEmpty()) {
            Notification.show("Veuillez entrer votre email",
                            3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            emailField.focus();
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            Notification.show("Veuillez entrer votre mot de passe",
                            3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            passwordField.focus();
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Connexion en cours...");

        try {
            boolean authenticated = userService.authenticate(email, password);

            if (authenticated) {
                User user = userRepository.findByEmail(email).orElse(null);

                if (user == null) {
                    showError("Erreur lors de la récupération des informations utilisateur");
                    return;
                }

                if (!user.getActif()) {
                    showError("Votre compte est désactivé. Veuillez contacter l'administrateur.");
                    return;
                }

                VaadinSession.getCurrent().setAttribute(User.class, user);

                Notification.show("Connexion réussie ! Bienvenue " + user.getPrenom() + " !",
                                3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                getUI().ifPresent(ui -> {
                    String targetRoute = getRouteByRole(user.getRole());
                    ui.navigate(targetRoute);
                });

            } else {
                showError("Email ou mot de passe incorrect");
            }

        } catch (Exception e) {
            showError("Erreur lors de la connexion : " + e.getMessage());
            e.printStackTrace();
        } finally {
            loginButton.setEnabled(true);
            loginButton.setText("Se connecter");
        }
    }

    private String getRouteByRole(UserRole role) {
        switch (role) {
            case ADMIN:
                return "admin/dashboard";
            case ORGANIZER:
                return "organizer/dashboard";
            case CLIENT:
                return "dashboard";
            default:
                return "";
        }
    }

    private void showError(String message) {
        Notification notification = Notification.show(message,
                4000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        emailField.focus();
    }
}