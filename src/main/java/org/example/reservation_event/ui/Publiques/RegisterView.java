package org.example.reservation_event.ui.Publiques;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.example.reservation_event.Enums.UserRole;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.repositories.UserRepository;
import org.example.reservation_event.email.EmailService;
import org.example.reservation_event.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@Route("register")
@AnonymousAllowed
@PageTitle("Inscription - Système de Réservation")
public class RegisterView extends VerticalLayout {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private TextField nomField;
    private TextField prenomField;
    private EmailField emailField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private TextField telephoneField;
    private ComboBox<UserRole> roleComboBox;
    private Button submitButton;
    private Button loginButton;

    private Binder<User> binder;

    // Verification dialog components
    private Dialog verificationDialog;
    private TextField verificationCodeField;
    private Button verifyButton;
    private Button resendCodeButton;
    private Span countdownTimer;
    private Thread countdownThread;
    private static final String VERIFICATION_MAP_KEY = "pendingVerifications";

    // Pattern pour validation mot de passe fort
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    );

    @Autowired
    public RegisterView(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;

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

        createThemeToggle();
        createRegisterCard();
        setupBinder();
        createVerificationDialog();
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
                .set("transition", "all 0.3s ease")
                .set("z-index", "1000");
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

    private void createRegisterCard() {
        VerticalLayout registerCard = new VerticalLayout();
        registerCard.setWidth("700px");
        registerCard.setPadding(true);
        registerCard.setSpacing(true);
        registerCard.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-xl)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "var(--lumo-space-xl)")
                .set("margin", "var(--lumo-space-m) 0");

        // Logo et titre
        HorizontalLayout logoSection = new HorizontalLayout();
        logoSection.setAlignItems(FlexComponent.Alignment.CENTER);
        logoSection.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        logoSection.setWidthFull();
        logoSection.setSpacing(true);
        logoSection.getStyle().set("margin-bottom", "var(--lumo-space-s)");

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
        H2 subtitle = new H2("Créer un compte");
        subtitle.getStyle()
                .set("margin", "0 0 var(--lumo-space-s) 0")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "1.5em")
                .set("text-align", "center");

        Paragraph welcomeText = new Paragraph("Remplissez tous les champs pour créer votre compte EventBooking.");
        welcomeText.getStyle()
                .set("margin", "0 0 var(--lumo-space-l) 0")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("font-size", "0.95em");

        // Formulaire
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        // Nom
        nomField = new TextField("Nom");
        nomField.setPlaceholder("Ex: Alami");
        nomField.setPrefixComponent(VaadinIcon.USER.create());
        nomField.setRequiredIndicatorVisible(true);
        nomField.setWidthFull();

        // Prénom
        prenomField = new TextField("Prénom");
        prenomField.setPlaceholder("Ex: Hassan");
        prenomField.setPrefixComponent(VaadinIcon.USER.create());
        prenomField.setRequiredIndicatorVisible(true);
        prenomField.setWidthFull();

        // Email
        emailField = new EmailField("Email");
        emailField.setPlaceholder("exemple@email.com");
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setRequiredIndicatorVisible(true);
        emailField.setWidthFull();
        emailField.setHelperText("Utilisez un email valide");

        // Téléphone
        telephoneField = new TextField("Téléphone");
        telephoneField.setPlaceholder("+212 6 XX XX XX XX");
        telephoneField.setPrefixComponent(VaadinIcon.PHONE.create());
        telephoneField.setWidthFull();
        telephoneField.setHelperText("Optionnel");

        // Mot de passe
        passwordField = new PasswordField("Mot de passe");
        passwordField.setPlaceholder("Minimum 8 caractères");
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setWidthFull();
        passwordField.setHelperText("Doit contenir: majuscule, minuscule, chiffre et caractère spécial (@#$%^&+=!)");

        // Confirmation mot de passe
        confirmPasswordField = new PasswordField("Confirmer le mot de passe");
        confirmPasswordField.setPlaceholder("Retapez votre mot de passe");
        confirmPasswordField.setPrefixComponent(VaadinIcon.LOCK.create());
        confirmPasswordField.setRequiredIndicatorVisible(true);
        confirmPasswordField.setWidthFull();

        // Rôle
        roleComboBox = new ComboBox<>("Rôle");
        roleComboBox.setItems(UserRole.values());
        roleComboBox.setItemLabelGenerator(role -> {
            switch (role) {
                case ADMIN: return "Administrateur";
                case ORGANIZER: return "Organisateur";
                case CLIENT: return "Client";
                default: return role.name();
            }
        });
        roleComboBox.setValue(UserRole.CLIENT);
        roleComboBox.setRequiredIndicatorVisible(true);
        roleComboBox.setWidthFull();
        roleComboBox.setPrefixComponent(VaadinIcon.STAR.create());
        roleComboBox.setHelperText("Sélectionnez votre type de compte");

        // Ajouter les champs au formulaire
        formLayout.add(nomField, prenomField);
        formLayout.add(emailField, telephoneField);
        formLayout.add(passwordField, confirmPasswordField);
        formLayout.add(roleComboBox, 2);

        // Indicateur de force du mot de passe
        Div passwordStrengthIndicator = new Div();
        passwordStrengthIndicator.getStyle()
                .set("margin-top", "var(--lumo-space-s)")
                .set("padding", "var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("display", "none");

        Span strengthText = new Span();
        strengthText.setId("strength-text");
        passwordStrengthIndicator.add(strengthText);

        passwordField.addValueChangeListener(e -> {
            String password = e.getValue();
            if (password == null || password.isEmpty()) {
                passwordStrengthIndicator.getStyle().set("display", "none");
            } else {
                passwordStrengthIndicator.getStyle().set("display", "block");
                int strength = calculatePasswordStrength(password);

                if (strength < 2) {
                    passwordStrengthIndicator.getStyle().set("background", "var(--lumo-error-color-10pct)");
                    strengthText.setText("⚠ Mot de passe faible");
                    strengthText.getStyle().set("color", "var(--lumo-error-text-color)");
                } else if (strength < 4) {
                    passwordStrengthIndicator.getStyle().set("background", "#fff3cd");
                    strengthText.setText("⚡ Mot de passe moyen");
                    strengthText.getStyle().set("color", "#856404");
                } else {
                    passwordStrengthIndicator.getStyle().set("background", "var(--lumo-success-color-10pct)");
                    strengthText.setText("✓ Mot de passe fort");
                    strengthText.getStyle().set("color", "var(--lumo-success-text-color)");
                }
            }
        });

        // Bouton d'inscription
        submitButton = new Button("S'inscrire", VaadinIcon.USER_CHECK.create());
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        submitButton.setWidthFull();
        submitButton.setIconAfterText(true);
        submitButton.getStyle()
                .set("font-weight", "bold")
                .set("margin-top", "var(--lumo-space-m)");

        submitButton.addClickListener(e -> handleRegistration());

        // Séparateur
        HorizontalLayout separator = new HorizontalLayout();
        separator.setWidthFull();
        separator.setAlignItems(FlexComponent.Alignment.CENTER);
        separator.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        separator.getStyle().set("margin", "var(--lumo-space-m) 0");

        Div line1 = new Div();
        line1.getStyle()
                .set("height", "1px")
                .set("background", "var(--lumo-contrast-20pct)")
                .set("flex", "1");

        Span orText = new Span("Déjà inscrit ?");
        orText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("padding", "0 var(--lumo-space-m)")
                .set("font-size", "0.9em");

        Div line2 = new Div();
        line2.getStyle()
                .set("height", "1px")
                .set("background", "var(--lumo-contrast-20pct)")
                .set("flex", "1");

        separator.add(line1, orText, line2);

        // Bouton connexion
        loginButton = new Button("Se connecter", VaadinIcon.SIGN_IN.create());
        loginButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE);
        loginButton.setWidthFull();
        loginButton.setIconAfterText(true);
        loginButton.getStyle().set("font-weight", "bold");

        loginButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("login"))
        );

        // Lien retour à l'accueil
        HorizontalLayout backLayout = new HorizontalLayout();
        backLayout.setWidthFull();
        backLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        backLayout.getStyle().set("margin-top", "var(--lumo-space-m)");

        Button backButton = new Button("Retour à l'accueil", VaadinIcon.ARROW_LEFT.create());
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backButton.getStyle().set("color", "var(--lumo-secondary-text-color)");
        backButton.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(""))
        );

        backLayout.add(backButton);

        // Ajouter tous les composants
        registerCard.add(
                logoSection,
                subtitle,
                welcomeText,
                formLayout,
                passwordStrengthIndicator,
                submitButton,
                separator,
                loginButton,
                backLayout
        );

        add(registerCard);
    }

    private void setupBinder() {
        binder = new BeanValidationBinder<>(User.class);

        // Nom
        binder.forField(nomField)
                .asRequired("Le nom est obligatoire")
                .withValidator(nom -> nom.length() >= 2,
                        "Le nom doit contenir au moins 2 caractères")
                .bind(User::getNom, User::setNom);

        // Prénom
        binder.forField(prenomField)
                .asRequired("Le prénom est obligatoire")
                .withValidator(prenom -> prenom.length() >= 2,
                        "Le prénom doit contenir au moins 2 caractères")
                .bind(User::getPrenom, User::setPrenom);

        // Email
        binder.forField(emailField)
                .asRequired("L'email est obligatoire")
                .withValidator(this::validateEmailFormat)
                .bind(User::getEmail, User::setEmail);

        // Téléphone (optionnel)
        binder.forField(telephoneField)
                .withValidator(this::validateTelephone)
                .bind(User::getTelephone, User::setTelephone);

        // Mot de passe
        binder.forField(passwordField)
                .asRequired("Le mot de passe est obligatoire")
                .withValidator(this::validatePasswordStrength)
                .bind(User::getPassword, User::setPassword);

        // Confirmation mot de passe
        binder.forField(confirmPasswordField)
                .asRequired("La confirmation du mot de passe est obligatoire")
                .withValidator(this::validatePasswordMatch)
                .bind(user -> "", (user, value) -> {});

        // Rôle
        binder.forField(roleComboBox)
                .asRequired("Le rôle est obligatoire")
                .bind(User::getRole, User::setRole);
    }

    private ValidationResult validateEmailFormat(String email, ValueContext context) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.error("L'email est obligatoire");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return ValidationResult.error("Format d'email invalide");
        }

        if (userRepository.existsByEmail(email)) {
            return ValidationResult.error("Cet email est déjà utilisé");
        }

        return ValidationResult.ok();
    }

    private ValidationResult validatePasswordStrength(String password, ValueContext context) {
        if (password == null || password.trim().isEmpty()) {
            return ValidationResult.error("Le mot de passe est obligatoire");
        }

        if (password.length() < 8) {
            return ValidationResult.error("Le mot de passe doit contenir au moins 8 caractères");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return ValidationResult.error(
                    "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial (@#$%^&+=!)"
            );
        }

        return ValidationResult.ok();
    }

    private ValidationResult validatePasswordMatch(String confirmPassword, ValueContext context) {
        String password = passwordField.getValue();

        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            return ValidationResult.error("Veuillez confirmer votre mot de passe");
        }

        if (!confirmPassword.equals(password)) {
            return ValidationResult.error("Les mots de passe ne correspondent pas");
        }

        return ValidationResult.ok();
    }

    private ValidationResult validateTelephone(String telephone, ValueContext context) {
        if (telephone != null && !telephone.trim().isEmpty()) {
            if (!telephone.matches("^[+]?[0-9\\s\\-\\(\\)]{10,}$")) {
                return ValidationResult.error("Format de téléphone invalide");
            }
        }
        return ValidationResult.ok();
    }

    private int calculatePasswordStrength(String password) {
        int strength = 0;

        if (password.length() >= 8) strength++;
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        if (password.matches(".*[@#$%^&+=!].*")) strength++;

        return strength;
    }

    private void handleRegistration() {
        User user = new User();

        if (binder.writeBeanIfValid(user)) {
            submitButton.setEnabled(false);
            submitButton.setText("Inscription en cours...");

            try {
                if (userRepository.existsByEmail(user.getEmail())) {
                    Notification.show("Cet email est déjà utilisé");
                    submitButton.setEnabled(true);
                    submitButton.setText("S'inscrire");
                    return;
                }

                String encodedPassword = passwordEncoder.encode(user.getPassword());
                user.setPassword(encodedPassword);
                user.setDateInscription(LocalDateTime.now());
                user.setActif(true);

                String verificationCode = generateVerificationCode();

                Map<String, Object> verificationData = new HashMap<>();
                verificationData.put("user", user);
                verificationData.put("code", verificationCode);
                verificationData.put("expiry", LocalDateTime.now().plusMinutes(10));

                VaadinSession.getCurrent().setAttribute(VERIFICATION_MAP_KEY, verificationData);

                boolean emailSent = emailService.sendVerificationEmail(
                        user.getEmail(),
                        verificationCode
                );

                if (emailSent) {
                    showVerificationDialog(user);
                } else {
                    userRepository.save(user);
                    Notification.show(
                            "Compte créé, mais échec d'envoi d'email de vérification."
                    );
                    getUI().ifPresent(ui -> ui.navigate("login"));
                }

            } catch (Exception e) {
                Notification.show("Erreur lors de l'inscription : " + e.getMessage());
                e.printStackTrace();
            } finally {
                submitButton.setEnabled(true);
                submitButton.setText("S'inscrire");
            }
        } else {
            Notification.show("Veuillez corriger les erreurs dans le formulaire");
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void createVerificationDialog() {
        verificationDialog = new Dialog();
        verificationDialog.setModal(true);
        verificationDialog.setCloseOnEsc(false);
        verificationDialog.setCloseOnOutsideClick(false);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        dialogLayout.setAlignItems(Alignment.CENTER);
        dialogLayout.setWidth("400px");
        dialogLayout.getStyle()
                .set("background", "var(--lumo-base-color)");

        Icon emailIcon = VaadinIcon.ENVELOPE.create();
        emailIcon.setSize("48px");
        emailIcon.setColor("var(--lumo-success-color)");

        H3 dialogTitle = new H3("Vérification d'Email");
        dialogTitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)")
                .set("text-align", "center");

        Paragraph instructions = new Paragraph(
                "Un code de vérification à 6 chiffres a été envoyé à votre adresse email. " +
                        "Veuillez l'entrer ci-dessous pour activer votre compte."
        );
        instructions.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("font-size", "0.9em");

        Span emailDisplay = new Span();
        emailDisplay.setId("email-display");
        emailDisplay.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)")
                .set("margin", "var(--lumo-space-s) 0");

        countdownTimer = new Span("Le code expire dans: 10:00");
        countdownTimer.getStyle()
                .set("color", "var(--lumo-error-text-color)")
                .set("font-weight", "bold")
                .set("margin", "var(--lumo-space-s) 0");

        verificationCodeField = new TextField("Code de vérification");
        verificationCodeField.setPlaceholder("Ex: 123456");
        verificationCodeField.setWidth("200px");
        verificationCodeField.setMaxLength(6);
        verificationCodeField.setPattern("[0-9]*");
        verificationCodeField.setInvalid(true);

        Span errorMessage = new Span();
        errorMessage.getStyle()
                .set("color", "var(--lumo-error-text-color)")
                .set("font-size", "0.8em")
                .set("min-height", "20px");

        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSpacing(true);

        verifyButton = new Button("Vérifier", VaadinIcon.CHECK.create());
        verifyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        verifyButton.addClickListener(e -> verifyCode(errorMessage));

        resendCodeButton = new Button("Renvoyer", VaadinIcon.REFRESH.create());
        resendCodeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resendCodeButton.addClickListener(e -> resendVerificationCode(errorMessage));

        buttonsLayout.add(verifyButton, resendCodeButton);

        dialogLayout.add(emailIcon, dialogTitle, instructions,
                emailDisplay, countdownTimer, verificationCodeField,
                errorMessage, buttonsLayout);

        verificationDialog.add(dialogLayout);

        verificationDialog.addOpenedChangeListener(event -> {
            if (!event.isOpened() && countdownThread != null) {
                countdownThread.interrupt();
            }
        });
    }

    private void showVerificationDialog(User user) {
        getElement().executeJs(
                "document.getElementById('email-display').textContent = $0;",
                user.getEmail()
        );

        verificationCodeField.clear();
        verificationCodeField.setEnabled(true);
        verifyButton.setEnabled(true);

        startCountdownTimer(10);
        verificationCodeField.focus();
        verificationDialog.open();
    }

    private void startCountdownTimer(int minutes) {
        if (countdownThread != null && countdownThread.isAlive()) {
            countdownThread.interrupt();
        }

        int totalSeconds = minutes * 60;

        countdownThread = new Thread(() -> {
            try {
                for (int i = totalSeconds; i >= 0; i--) {
                    int minutesLeft = i / 60;
                    int secondsLeft = i % 60;

                    final String timeText = String.format(
                            "Le code expire dans: %02d:%02d",
                            minutesLeft, secondsLeft
                    );

                    int finalI = i;
                    getUI().ifPresent(ui -> ui.access(() -> {
                        countdownTimer.setText(timeText);

                        if (finalI < 60) {
                            countdownTimer.getStyle().set("color", "var(--lumo-error-text-color)");
                        } else if (finalI < 120) {
                            countdownTimer.getStyle().set("color", "#f39c12");
                        } else {
                            countdownTimer.getStyle().set("color", "var(--lumo-success-text-color)");
                        }

                        if (finalI == 0) {
                            verificationCodeField.setEnabled(false);
                            verifyButton.setEnabled(false);
                            countdownTimer.setText("Code expiré !");
                            countdownTimer.getStyle().set("color", "var(--lumo-error-text-color)");
                        }
                    }));

                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        countdownThread.start();
    }

    private void verifyCode(Span errorMessage) {
        String enteredCode = verificationCodeField.getValue();

        if (enteredCode == null || enteredCode.trim().isEmpty()) {
            errorMessage.setText("Veuillez entrer le code de vérification");
            return;
        }

        if (enteredCode.length() != 6) {
            errorMessage.setText("Le code doit contenir exactement 6 chiffres");
            return;
        }

        errorMessage.setText("");

        try {
            Map<String, Object> verificationData =
                    (Map<String, Object>) VaadinSession.getCurrent().getAttribute(VERIFICATION_MAP_KEY);

            if (verificationData == null) {
                errorMessage.setText("Session expirée. Veuillez réessayer.");
                return;
            }

            User user = (User) verificationData.get("user");
            String storedCode = (String) verificationData.get("code");
            LocalDateTime expiry = (LocalDateTime) verificationData.get("expiry");

            if (!enteredCode.equals(storedCode)) {
                errorMessage.setText("Code de vérification incorrect");
                verificationCodeField.focus();
                return;
            }

            if (expiry.isBefore(LocalDateTime.now())) {
                errorMessage.setText("Le code de vérification a expiré");
                verificationCodeField.setEnabled(false);
                verifyButton.setEnabled(false);
                return;
            }

            userRepository.save(user);

            VaadinSession.getCurrent().setAttribute(VERIFICATION_MAP_KEY, null);

            if (countdownThread != null) {
                countdownThread.interrupt();
            }

            verificationDialog.close();

            Notification successNotification = Notification.show(
                    "🎉 Félicitations " + user.getPrenom() + " !\n" +
                            "Votre compte a été créé avec succès.\n" +
                            "Vous pouvez maintenant vous connecter.",
                    5000, Notification.Position.MIDDLE
            );
            successNotification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            getUI().ifPresent(ui -> {
                ui.access(() -> {
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000);
                            ui.access(() -> ui.navigate("login"));
                        } catch (InterruptedException e) {
                            ui.access(() -> ui.navigate("login"));
                        }
                    }).start();
                });
            });

        } catch (Exception e) {
            errorMessage.setText("Erreur lors de la vérification: " + e.getMessage());
        }
    }

    private void resendVerificationCode(Span errorMessage) {
        Map<String, Object> verificationData =
                (Map<String, Object>) VaadinSession.getCurrent().getAttribute(VERIFICATION_MAP_KEY);

        if (verificationData == null) {
            errorMessage.setText("Session expirée.");
            return;
        }

        User user = (User) verificationData.get("user");

        resendCodeButton.setEnabled(false);
        resendCodeButton.setText("Envoi...");

        try {
            String newCode = generateVerificationCode();

            verificationData.put("code", newCode);
            verificationData.put("expiry", LocalDateTime.now().plusMinutes(10));
            VaadinSession.getCurrent().setAttribute(VERIFICATION_MAP_KEY, verificationData);

            boolean emailSent = emailService.sendVerificationEmail(
                    user.getEmail(),
                    newCode
            );

            if (emailSent) {
                errorMessage.setText("");
                verificationCodeField.clear();
                verificationCodeField.focus();

                startCountdownTimer(10);

                errorMessage.getStyle().set("color", "var(--lumo-success-text-color)");
                errorMessage.setText("✅ Nouveau code envoyé à " + user.getEmail());

                verificationCodeField.setEnabled(true);
                verifyButton.setEnabled(true);

                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        getUI().ifPresent(ui -> ui.access(() -> {
                            errorMessage.setText("");
                            errorMessage.getStyle().set("color", "var(--lumo-error-text-color)");
                        }));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

            } else {
                errorMessage.setText("❌ Échec d'envoi du nouveau code");
            }

        } catch (Exception e) {
            errorMessage.setText("Erreur: " + e.getMessage());
        } finally {
            new Thread(() -> {
                try {
                    Thread.sleep(30000); // 30 seconds cooldown
                    getUI().ifPresent(ui -> ui.access(() -> {
                        resendCodeButton.setEnabled(true);
                        resendCodeButton.setText("Renvoyer");
                    }));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}