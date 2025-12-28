package org.example.reservation_event.ui.Clients;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.email.EmailService;
import org.example.reservation_event.services.UserService;
import org.example.reservation_event.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Mon Profil")
public class ProfileView extends VerticalLayout {

    private final UserService userService;
    private final EmailService emailService;
    private User currentUser;

    // Form fields
    private TextField nomField;
    private TextField prenomField;
    private EmailField emailField;
    private TextField telephoneField;
    private Binder<User> binder;

    // Email verification
    private String emailVerificationCode;
    private String pendingNewEmail;
    private User userWithPendingChanges;
    private Dialog verificationDialog;
    private TextField verificationCodeField;
    private Span countdownTimer;
    private Thread countdownThread;

    @Autowired
    public ProfileView(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        // Get current user
        currentUser = getCurrentUser();
        if (currentUser == null) {
            showLoginRequired();
            return;
        }

        // Initialize layout
        initializeLayout();
    }

    private void initializeLayout() {
        add(
                createHeader(),
                createProfileInfoSection(),
                createStatisticsSection(),
                createActionsSection()
        );
    }

    private Component createHeader() {
        H1 title = new H1("Mon Profil");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");
        return title;
    }

    private Component createProfileInfoSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");


        HorizontalLayout sectionHeader = new HorizontalLayout();
        sectionHeader.setWidthFull();
        sectionHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        sectionHeader.setAlignItems(FlexComponent.Alignment.CENTER);

        H2 sectionTitle = new H2("Informations personnelles");
        sectionTitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        sectionHeader.add(sectionTitle);

        // Profile form
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        nomField = new TextField("Nom");
        prenomField = new TextField("Prénom");
        emailField = new EmailField("Email");
        telephoneField = new TextField("Téléphone");

        // Role field (read-only)
        TextField roleField = new TextField("Rôle");
        roleField.setValue(currentUser.getRole().getLabel());
        roleField.setReadOnly(true);

        // Member since (read-only)
        TextField memberSinceField = new TextField("Membre depuis");
        memberSinceField.setValue(formatDate(currentUser.getDateInscription()));
        memberSinceField.setReadOnly(true);

        formLayout.add(nomField, prenomField, emailField, telephoneField, roleField, memberSinceField);

        // Binder for validation
        binder = new Binder<>(User.class);
        binder.forField(nomField)
                .asRequired("Le nom est requis")
                .bind(User::getNom, User::setNom);

        binder.forField(prenomField)
                .asRequired("Le prénom est requis")
                .bind(User::getPrenom, User::setPrenom);

        binder.forField(emailField)
                .asRequired("L'email est requis")
                .withValidator(new EmailValidator("Email invalide"))
                .bind(User::getEmail, User::setEmail);

        binder.forField(telephoneField)
                .bind(User::getTelephone, User::setTelephone);

        binder.readBean(currentUser);

        // Buttons
        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSpacing(true);

        Button saveButton = new Button("Enregistrer", new Icon(VaadinIcon.CHECK));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveProfile());

        Button cancelButton = new Button("Annuler", new Icon(VaadinIcon.CLOSE));
        cancelButton.addClickListener(e -> binder.readBean(currentUser));

        buttonsLayout.add(saveButton, cancelButton);

        section.add(sectionHeader, formLayout, buttonsLayout);
        return section;
    }

    private Component createStatisticsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        H2 sectionTitle = new H2("Mes statistiques");
        sectionTitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        // Get statistics from service
        Map<String, Object> stats = userService.getUserStats(currentUser.getId());

        // Extract values
        int totalReservations = ((Number) stats.getOrDefault("reservations", 0)).intValue();
        double totalSpent = ((Number) stats.getOrDefault("totalSpent", 0.0)).doubleValue();

        HorizontalLayout statsGrid = new HorizontalLayout();
        statsGrid.setWidthFull();
        statsGrid.setSpacing(true);

        Component totalReservationsCard = createStatCard(
                "Réservations totales",
                String.valueOf(totalReservations),
                VaadinIcon.CALENDAR,
                "#4f46e5"
        );

        Component totalSpentCard = createStatCard(
                "Montant dépensé",
                String.format("%.2f DH", totalSpent),
                VaadinIcon.MONEY,
                "#dc2626"
        );

        statsGrid.add(totalReservationsCard, totalSpentCard);

        section.add(sectionTitle, statsGrid);
        return section;
    }

    private Component createStatCard(String label, String value, VaadinIcon icon, String color) {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px")
                .set("border-left", "4px solid " + color);

        Icon cardIcon = new Icon(icon);
        cardIcon.setSize("32px");
        cardIcon.setColor(color);

        Span cardLabel = new Span(label);
        cardLabel.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        H3 cardValue = new H3(value);
        cardValue.getStyle()
                .set("margin", "5px 0 0 0")
                .set("color", "var(--lumo-primary-text-color)");

        card.add(cardIcon, cardLabel, cardValue);
        return card;
    }

    private Component createActionsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        H2 sectionTitle = new H2("Actions du compte");
        sectionTitle.getStyle()
                .set("margin", "0 0 20px 0")
                .set("color", "var(--lumo-primary-text-color)");

        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.setWidthFull();
        actionsLayout.setSpacing(true);

        // Change password button
        Button changePasswordButton = new Button("Changer le mot de passe", new Icon(VaadinIcon.LOCK));
        changePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changePasswordButton.addClickListener(e -> openChangePasswordDialog());

        // Deactivate account button
        Button deactivateButton = new Button("Désactiver mon compte", new Icon(VaadinIcon.BAN));
        deactivateButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deactivateButton.addClickListener(e -> confirmDeactivateAccount());

        actionsLayout.add(changePasswordButton, deactivateButton);

        section.add(sectionTitle, actionsLayout);
        return section;
    }

    private void saveProfile() {
        try {
            User editedUser = new User();
            binder.writeBean(editedUser);

            boolean emailChanged = !editedUser.getEmail().equals(currentUser.getEmail());

            // If email not changed → save normally
            if (!emailChanged) {
                applyProfileChanges(editedUser);
                Notification.show("Profil mis à jour avec succès", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                return;
            }

            // If email changed → verification required
            pendingNewEmail = editedUser.getEmail();

            // Check if email is already used
            User existing = userService.findByEmailOrNull(pendingNewEmail);
            if (existing != null && !existing.getId().equals(currentUser.getId())) {
                Notification.show("Cet email est déjà utilisé par un autre compte",
                                3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            userWithPendingChanges = editedUser;

            // Send verification code
            if (startEmailChangeVerification(pendingNewEmail)) {
                showEmailVerificationDialog();
            } else {
                Notification.show("Erreur: impossible d'envoyer un email de vérification.",
                                3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }

        } catch (ValidationException e) {
            Notification.show("Erreur de validation", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean startEmailChangeVerification(String newEmail) {
        try {
            emailVerificationCode = generateVerificationCode();
            return emailService.sendEmailChangeVerification(newEmail, emailVerificationCode);
        } catch (Exception e) {
            System.err.println("Error sending verification email: " + e.getMessage());
            return false;
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void showEmailVerificationDialog() {
        verificationDialog = new Dialog();
        verificationDialog.setModal(true);
        verificationDialog.setCloseOnEsc(false);
        verificationDialog.setCloseOnOutsideClick(false);
        verificationDialog.setWidth("400px");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon emailIcon = VaadinIcon.ENVELOPE.create();
        emailIcon.setSize("48px");
        emailIcon.setColor("#4CAF50");

        H3 dialogTitle = new H3("Vérification de l'email");
        dialogTitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)")
                .set("text-align", "center");

        Paragraph instructions = new Paragraph(
                "Un code de vérification à 6 chiffres a été envoyé à votre nouvelle adresse email:"
        );
        instructions.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("font-size", "0.9em");

        Span emailDisplay = new Span(pendingNewEmail);
        emailDisplay.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)")
                .set("margin", "10px 0")
                .set("font-size", "1.1em");

        countdownTimer = new Span("Le code expire dans: 10:00");
        countdownTimer.getStyle()
                .set("color", "#e74c3c")
                .set("font-weight", "bold")
                .set("margin", "10px 0");

        verificationCodeField = new TextField("Code de vérification");
        verificationCodeField.setPlaceholder("Ex: 123456");
        verificationCodeField.setWidth("200px");
        verificationCodeField.setMaxLength(6);
        verificationCodeField.setPattern("[0-9]*");

        Span errorMessage = new Span();
        errorMessage.getStyle()
                .set("color", "#e74c3c")
                .set("font-size", "0.8em")
                .set("min-height", "20px");

        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSpacing(true);

        Button verifyButton = new Button("Vérifier", VaadinIcon.CHECK.create());
        verifyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        verifyButton.addClickListener(e -> verifyEmailChangeCode(errorMessage));

        Button resendButton = new Button("Renvoyer", VaadinIcon.REFRESH.create());
        resendButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resendButton.addClickListener(e -> resendVerificationCode(errorMessage));

        Button cancelButton = new Button("Annuler", VaadinIcon.CLOSE.create());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancelButton.addClickListener(e -> {
            if (countdownThread != null) {
                countdownThread.interrupt();
            }
            verificationDialog.close();
        });

        buttonsLayout.add(verifyButton, resendButton, cancelButton);

        dialogLayout.add(emailIcon, dialogTitle, instructions,
                emailDisplay, countdownTimer, verificationCodeField,
                errorMessage, buttonsLayout);

        verificationDialog.add(dialogLayout);

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
                            countdownTimer.getStyle().set("color", "#e74c3c");
                        } else if (finalI < 120) {
                            countdownTimer.getStyle().set("color", "#f39c12");
                        } else {
                            countdownTimer.getStyle().set("color", "#2ecc71");
                        }

                        if (finalI == 0) {
                            verificationCodeField.setEnabled(false);
                            countdownTimer.setText("Code expiré !");
                            countdownTimer.getStyle().set("color", "#e74c3c");
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

    private void verifyEmailChangeCode(Span errorMessage) {
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
            if (!enteredCode.equals(emailVerificationCode)) {
                errorMessage.setText("Code de vérification incorrect");
                verificationCodeField.focus();
                return;
            }

            applyProfileChanges(userWithPendingChanges);

            if (countdownThread != null) {
                countdownThread.interrupt();
            }

            verificationDialog.close();

            Notification successNotification = Notification.show(
                    "✅ Email modifié avec succès !\n" +
                            "Votre adresse email a été mise à jour.",
                    5000, Notification.Position.MIDDLE
            );
            successNotification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            VaadinSession.getCurrent().setAttribute(User.class, currentUser);
            binder.readBean(currentUser);

        } catch (Exception e) {
            errorMessage.setText("Erreur lors de la vérification: " + e.getMessage());
        }
    }

    private void resendVerificationCode(Span errorMessage) {
        try {
            emailVerificationCode = generateVerificationCode();

            boolean emailSent = emailService.sendEmailChangeVerification(
                    pendingNewEmail,
                    emailVerificationCode
            );

            if (emailSent) {
                errorMessage.setText("");
                verificationCodeField.clear();
                verificationCodeField.focus();

                startCountdownTimer(10);

                errorMessage.getStyle().set("color", "#2ecc71");
                errorMessage.setText("✅ Nouveau code envoyé");

                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        getUI().ifPresent(ui -> ui.access(() -> {
                            errorMessage.setText("");
                            errorMessage.getStyle().set("color", "#e74c3c");
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
        }
    }

    private void applyProfileChanges(User editedUser) {
        if (!currentUser.getNom().equals(editedUser.getNom())) {
            currentUser.setNom(editedUser.getNom());
        }

        if (!currentUser.getPrenom().equals(editedUser.getPrenom())) {
            currentUser.setPrenom(editedUser.getPrenom());
        }

        if (!currentUser.getEmail().equals(editedUser.getEmail())) {
            currentUser.setEmail(editedUser.getEmail());
        }

        if ((currentUser.getTelephone() == null && editedUser.getTelephone() != null) ||
                (currentUser.getTelephone() != null && !currentUser.getTelephone().equals(editedUser.getTelephone()))) {
            currentUser.setTelephone(editedUser.getTelephone());
        }

        userService.updateProfile(currentUser.getId(), currentUser);
        VaadinSession.getCurrent().setAttribute(User.class, currentUser);
    }

    private void openChangePasswordDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Changer le mot de passe");
        dialog.setWidth("400px");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        PasswordField currentPasswordField = new PasswordField("Mot de passe actuel");
        currentPasswordField.setWidthFull();
        currentPasswordField.setRequired(true);

        PasswordField newPasswordField = new PasswordField("Nouveau mot de passe");
        newPasswordField.setWidthFull();
        newPasswordField.setRequired(true);
        newPasswordField.setHelperText("Minimum 8 caractères, avec au moins 1 majuscule, 1 chiffre et 1 caractère '@'");

        PasswordField confirmPasswordField = new PasswordField("Confirmer le mot de passe");
        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequired(true);

        dialogLayout.add(currentPasswordField, newPasswordField, confirmPasswordField);

        Button saveButton = new Button("Enregistrer", new Icon(VaadinIcon.CHECK));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> {
            String currentPassword = currentPasswordField.getValue();
            String newPassword = newPasswordField.getValue();
            String confirmPassword = confirmPasswordField.getValue();

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Notification.show("Tous les champs sont requis", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (newPassword.length() < 8) {
                Notification.show("Le mot de passe doit contenir au moins 8 caractères", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (!newPassword.matches("^(?=.*[A-Z])(?=.*\\d)(?=.*@).+$")) {
                Notification.show("Le mot de passe doit contenir au moins une majuscule, un chiffre et le caractère '@'",
                                5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Notification.show("Les mots de passe ne correspondent pas", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                userService.changePassword(currentUser.getId(), currentPassword, newPassword);

                Notification.show("Mot de passe modifié avec succès", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();

            } catch (Exception ex) {
                Notification.show("Erreur: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelButton = new Button("Annuler", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(dialogLayout);
        dialog.open();
    }

    private void confirmDeactivateAccount() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Désactiver le compte");
        dialog.setText("Êtes-vous sûr de vouloir désactiver votre compte ? Cette action peut être annulée en contactant l'administrateur.");

        dialog.setCancelable(true);
        dialog.setCancelText("Annuler");

        dialog.setConfirmText("Désactiver");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> deactivateAccount());

        dialog.open();
    }

    private void deactivateAccount() {
        try {
            userService.setActive(currentUser.getId(), false);

            Notification.show("Compte désactivé avec succès. Vous allez être déconnecté.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            VaadinSession.getCurrent().getSession().invalidate();
            SecurityContextHolder.clearContext();

            getUI().ifPresent(ui -> ui.navigate("login"));

        } catch (Exception e) {
            Notification.show("Erreur lors de la désactivation: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void showLoginRequired() {
        removeAll();

        VerticalLayout loginLayout = new VerticalLayout();
        loginLayout.setSizeFull();
        loginLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        loginLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon warningIcon = new Icon(VaadinIcon.EXCLAMATION_CIRCLE_O);
        warningIcon.setSize("64px");
        warningIcon.setColor("#f59e0b");

        H2 warningTitle = new H2("Connexion requise");
        warningTitle.getStyle()
                .set("color", "var(--lumo-error-text-color)");

        Span warningMessage = new Span("Vous devez être connecté pour accéder à votre profil");

        Button loginButton = new Button("Se connecter", e -> getUI().ifPresent(ui -> ui.navigate("login")));
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        loginLayout.add(warningIcon, warningTitle, warningMessage, loginButton);
        add(loginLayout);
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
                return userService.findByEmail(username);
            }

        } catch (Exception e) {
            System.out.println("Error getting current user: " + e.getMessage());
        }

        return null;
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dateTime.format(formatter);
    }
}