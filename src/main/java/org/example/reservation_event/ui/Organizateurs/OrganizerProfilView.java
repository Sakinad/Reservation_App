package org.example.reservation_event.ui.Organizateurs;

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
import org.example.reservation_event.services.EventService;
import org.example.reservation_event.services.UserService;
import org.example.reservation_event.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

@Route(value = "organizer/profile", layout = MainLayout.class)
@PageTitle("Mon Profil - Organisateur")
public class OrganizerProfilView extends VerticalLayout {

    private final UserService userService;
    private final EmailService emailService;
    private final EventService eventService;
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
    public OrganizerProfilView(UserService userService, EmailService emailService, EventService eventService) {
        this.userService = userService;
        this.emailService = emailService;
        this.eventService = eventService;

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
                createProfileInfoSection(),
                createStatisticsSection(),
                createActionsSection()
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
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.05)")
                .set("border-radius", "12px");

        VerticalLayout titleSection = new VerticalLayout();
        titleSection.setSpacing(false);
        titleSection.setPadding(false);

        H1 title = new H1("Mon Profil");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Span subtitle = new Span("Gestion de votre compte organisateur");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        titleSection.add(title, subtitle);
        header.add(titleSection);
        header.setFlexGrow(1, titleSection);

        return header;
    }

    private Component createProfileInfoSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);

        // Style adaptatif
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        H2 sectionTitle = new H2("Informations personnelles");
        sectionTitle.getStyle()
                .set("margin", "0 0 20px 0")
                .set("color", "var(--lumo-primary-text-color)");

        // Profile form
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        nomField = new TextField("Nom *");
        nomField.setWidthFull();

        prenomField = new TextField("Prénom *");
        prenomField.setWidthFull();

        emailField = new EmailField("Email *");
        emailField.setWidthFull();

        telephoneField = new TextField("Téléphone");
        telephoneField.setWidthFull();

        // Role field (read-only)
        TextField roleField = new TextField("Rôle");
        roleField.setValue(currentUser.getRole().getLabel());
        roleField.setReadOnly(true);
        roleField.setWidthFull();

        // Member since (read-only)
        TextField memberSinceField = new TextField("Membre depuis");
        memberSinceField.setValue(formatDate(currentUser.getDateInscription()));
        memberSinceField.setReadOnly(true);
        memberSinceField.setWidthFull();

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

        section.add(sectionTitle, formLayout, buttonsLayout);
        return section;
    }

    private Component createStatisticsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);

        // Style adaptatif
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        H2 sectionTitle = new H2("Statistiques de l'organisateur");
        sectionTitle.getStyle()
                .set("margin", "0 0 20px 0")
                .set("color", "var(--lumo-primary-text-color)");

        // Get statistics
        Map<String, Object> stats = eventService.getOrganizerStats(currentUser);

        int totalEvents = ((Number) stats.getOrDefault("totalEvents", 0)).intValue();
        int totalReservations = ((Number) stats.getOrDefault("totalReservations", 0)).intValue();
        double totalRevenue = ((Number) stats.getOrDefault("totalRevenue", 0.0)).doubleValue();
        int publishedEvents = ((Number) stats.getOrDefault("publishedEvents", 0)).intValue();

        // Statistics grid
        HorizontalLayout statsGrid = new HorizontalLayout();
        statsGrid.setWidthFull();
        statsGrid.setSpacing(true);
        statsGrid.getStyle().set("flex-wrap", "wrap");

        statsGrid.add(
                createStatCard("Événements créés", String.valueOf(totalEvents), VaadinIcon.CALENDAR, "#4f46e5"),
                createStatCard("Réservations totales", String.valueOf(totalReservations), VaadinIcon.TICKET, "#10b981"),
                createStatCard("Revenus totaux", String.format("%.2f DH", totalRevenue), VaadinIcon.MONEY, "#f59e0b"),
                createStatCard("Événements publiés", String.valueOf(publishedEvents), VaadinIcon.ROCKET, "#8b5cf6")
        );

        section.add(sectionTitle, statsGrid);
        return section;
    }

    private Component createStatCard(String label, String value, VaadinIcon icon, String color) {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);

        // Style adaptatif
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.1)")
                .set("min-width", "180px");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "500");

        Icon cardIcon = new Icon(icon);
        cardIcon.setSize("20px");
        cardIcon.setColor(color);
        cardIcon.getStyle()
                .set("padding", "8px")
                .set("background", color + "20")
                .set("border-radius", "8px");

        header.add(labelSpan, cardIcon);

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "700")
                .set("color", "var(--lumo-primary-text-color)")
                .set("margin-top", "10px");

        card.add(header, valueSpan);
        return card;
    }

    private Component createActionsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);

        // Style adaptatif
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");

        H2 sectionTitle = new H2("Actions du compte");
        sectionTitle.getStyle()
                .set("margin", "0 0 20px 0")
                .set("color", "var(--lumo-primary-text-color)");

        VerticalLayout actionsLayout = new VerticalLayout();
        actionsLayout.setWidthFull();
        actionsLayout.setSpacing(true);

        // Change password button
        Button changePasswordButton = new Button("Changer le mot de passe", new Icon(VaadinIcon.LOCK));
        changePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changePasswordButton.addClickListener(e -> openChangePasswordDialog());

        // Email info
        HorizontalLayout emailStatus = new HorizontalLayout();
        emailStatus.setSpacing(true);
        emailStatus.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon emailIcon = new Icon(VaadinIcon.ENVELOPE);
        emailIcon.setColor("#10b981");

        Span emailText = new Span("Email : " + currentUser.getEmail());
        emailText.getStyle().set("color", "var(--lumo-secondary-text-color)");

        emailStatus.add(emailIcon, emailText);

        // Deactivate account button
        Button deactivateButton = new Button("Désactiver mon compte", new Icon(VaadinIcon.BAN));
        deactivateButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deactivateButton.addClickListener(e -> confirmDeactivateAccount());

        actionsLayout.add(changePasswordButton, emailStatus, deactivateButton);
        section.add(sectionTitle, actionsLayout);
        return section;
    }

    private void saveProfile() {
        try {
            User editedUser = new User();
            binder.writeBean(editedUser);

            boolean emailChanged = !editedUser.getEmail().equals(currentUser.getEmail());

            if (!emailChanged) {
                applyProfileChanges(editedUser);
                Notification.show("Profil mis à jour avec succès",
                                3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                return;
            }

            pendingNewEmail = editedUser.getEmail();

            User existing = userService.findByEmailOrNull(pendingNewEmail);
            if (existing != null && !existing.getId().equals(currentUser.getId())) {
                Notification.show("Cet email est déjà utilisé par un autre compte",
                                3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            userWithPendingChanges = editedUser;

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
        dialogLayout.setAlignItems(Alignment.CENTER);

        Icon emailIcon = VaadinIcon.ENVELOPE.create();
        emailIcon.setSize("48px");
        emailIcon.setColor("#4CAF50");

        H3 dialogTitle = new H3("Vérification de l'email");
        dialogTitle.getStyle().set("margin", "0").set("text-align", "center");

        Paragraph instructions = new Paragraph(
                "Un code de vérification à 6 chiffres a été envoyé à votre nouvelle adresse email:"
        );
        instructions.getStyle().set("text-align", "center").set("font-size", "0.9em");

        Span emailDisplay = new Span(pendingNewEmail);
        emailDisplay.getStyle().set("font-weight", "bold").set("margin", "10px 0");

        countdownTimer = new Span("Le code expire dans: 10:00");
        countdownTimer.getStyle().set("color", "#e74c3c").set("font-weight", "bold");

        verificationCodeField = new TextField("Code de vérification");
        verificationCodeField.setPlaceholder("Ex: 123456");
        verificationCodeField.setWidth("200px");
        verificationCodeField.setMaxLength(6);

        Span errorMessage = new Span();
        errorMessage.getStyle().set("color", "#e74c3c").set("font-size", "0.8em").set("min-height", "20px");

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
            if (countdownThread != null) countdownThread.interrupt();
            verificationDialog.close();
        });

        buttonsLayout.add(verifyButton, resendButton, cancelButton);

        dialogLayout.add(emailIcon, dialogTitle, instructions, emailDisplay,
                countdownTimer, verificationCodeField, errorMessage, buttonsLayout);

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
                    final String timeText = String.format("Le code expire dans: %02d:%02d", minutesLeft, secondsLeft);
                    int finalI = i;

                    getUI().ifPresent(ui -> ui.access(() -> {
                        countdownTimer.setText(timeText);
                        if (finalI == 0) {
                            verificationCodeField.setEnabled(false);
                            countdownTimer.setText("Code expiré !");
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

        if (!enteredCode.equals(emailVerificationCode)) {
            errorMessage.setText("Code de vérification incorrect");
            verificationCodeField.focus();
            return;
        }

        if (userWithPendingChanges != null) {
            applyProfileChanges(userWithPendingChanges);
        }

        if (countdownThread != null) countdownThread.interrupt();
        verificationDialog.close();

        Notification.show("✅ Email modifié avec succès !", 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void resendVerificationCode(Span errorMessage) {
        emailVerificationCode = generateVerificationCode();
        boolean sent = emailService.sendEmailChangeVerification(pendingNewEmail, emailVerificationCode);

        if (sent) {
            verificationCodeField.clear();
            verificationCodeField.focus();
            startCountdownTimer(10);
            errorMessage.setText("✅ Nouveau code envoyé");
        } else {
            errorMessage.setText("❌ Échec d'envoi du nouveau code");
        }
    }

    private void applyProfileChanges(User editedUser) {
        try {
            User userToUpdate = userService.getUserById(currentUser.getId());
            userToUpdate.setNom(editedUser.getNom());
            userToUpdate.setPrenom(editedUser.getPrenom());
            userToUpdate.setEmail(editedUser.getEmail());
            userToUpdate.setTelephone(editedUser.getTelephone());

            User updatedUser = userService.updateProfile(userToUpdate.getId(), userToUpdate);

            if (updatedUser != null) {
                currentUser = updatedUser;
                VaadinSession.getCurrent().setAttribute(User.class, currentUser);
                binder.readBean(currentUser);
                userWithPendingChanges = null;
                pendingNewEmail = null;

                Notification.show("Profil mis à jour avec succès", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        } catch (Exception e) {
            Notification.show("Erreur: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void openChangePasswordDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Changer le mot de passe");
        dialog.setWidth("400px");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        PasswordField currentPasswordField = new PasswordField("Mot de passe actuel");
        PasswordField newPasswordField = new PasswordField("Nouveau mot de passe");
        PasswordField confirmPasswordField = new PasswordField("Confirmer le mot de passe");

        dialogLayout.add(currentPasswordField, newPasswordField, confirmPasswordField);

        Button saveButton = new Button("Enregistrer", e -> {
            String current = currentPasswordField.getValue();
            String newPwd = newPasswordField.getValue();
            String confirm = confirmPasswordField.getValue();

            if (newPwd.length() < 8 || !newPwd.equals(confirm)) {
                Notification.show("Vérifiez vos mots de passe", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                userService.changePassword(currentUser.getId(), current, newPwd);
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
        dialog.setText("Êtes-vous sûr de vouloir désactiver votre compte ?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Désactiver");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> deactivateAccount());
        dialog.open();
    }

    private void deactivateAccount() {
        try {
            userService.setActive(currentUser.getId(), false);
            Notification.show("Compte désactivé", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            VaadinSession.getCurrent().getSession().invalidate();
            SecurityContextHolder.clearContext();
            getUI().ifPresent(ui -> ui.navigate("login"));
        } catch (Exception e) {
            Notification.show("Erreur: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
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

        H1 title = new H1("Accès refusé");
        Button backButton = new Button("Retour", e -> getUI().ifPresent(ui -> ui.navigate("dashboard")));

        layout.add(icon, title, backButton);
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

    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}