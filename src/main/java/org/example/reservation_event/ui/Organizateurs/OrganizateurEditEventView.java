package org.example.reservation_event.ui.Organizateurs;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.RolesAllowed;
import org.example.reservation_event.Enums.EventCategorie;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.services.EventService;
import org.example.reservation_event.services.UserService;
import org.example.reservation_event.ui.MainLayout;
import org.example.reservation_event.utils.ImageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;

@Route(value = "organizer/event/edit", layout = MainLayout.class)
@PageTitle("Modifier l'événement - Organisateur")

public class OrganizateurEditEventView extends VerticalLayout implements HasUrlParameter<Long> {

    private final EventService eventService;
    private final UserService userService;

    private User currentUser;
    private Event currentEvent;

    // Form fields
    private TextField titreField;
    private TextArea descriptionField;
    private ComboBox<EventCategorie> categorieComboBox;
    private TextField lieuField;
    private TextField villeField;
    private NumberField prixField;
    private IntegerField capaciteField;
    private DatePicker dateDebutPicker;
    private TimePicker heureDebutPicker;
    private DatePicker dateFinPicker;
    private TimePicker heureFinPicker;

    // Image components
    private Image currentImage;
    private Span currentImageLabel;
    private Image previewImage;
    private Div previewContainer;
    private Button removeImageButton;

    // Upload data storage
    private ByteArrayOutputStream uploadedFileData;
    private String uploadedFileName;
    private String uploadedFileType;

    @Autowired
    public OrganizateurEditEventView(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "var(--lumo-contrast-5pct)");

        uploadedFileData = new ByteArrayOutputStream();
    }

    @Override
    public void setParameter(BeforeEvent event, Long eventId) {
        currentUser = getCurrentUser();

        if (currentUser == null || !isOrganizer(currentUser)) {
            showAccessDenied();
            return;
        }

        if (eventId == null) {
            showEventNotFound();
            return;
        }

        try {
            this.currentEvent = eventService.getEventById(eventId);

            if (this.currentEvent == null) {
                showEventNotFound();
                return;
            }

            // Verify ownership
            if (!canManageEvent(this.currentEvent)) {
                showAccessDenied();
                return;
            }

            initializeLayout();
            populateForm();

        } catch (Exception e) {
            e.printStackTrace();
            showEventNotFound();
        }
    }

    private void initializeLayout() {
        removeAll();

        VerticalLayout contentArea = new VerticalLayout();
        contentArea.setSizeFull();
        contentArea.setPadding(true);
        contentArea.setSpacing(true);
        contentArea.getStyle()
                .set("max-width", "1200px")
                .set("margin", "0 auto");

        contentArea.add(
                createHeader(),
                createEditForm()
        );

        add(contentArea);
    }

    private Component createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-bottom", "var(--lumo-space-m)");

        VerticalLayout titleSection = new VerticalLayout();
        titleSection.setSpacing(false);
        titleSection.setPadding(false);

        H1 title = new H1("Modifier l'événement");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Span subtitle = new Span(currentEvent != null ? currentEvent.getTitre() : "");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        titleSection.add(title, subtitle);

        Button backButton = new Button("Retour aux événements", new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("organizer/events")));
        backButton.getStyle().set("flex-shrink", "0");

        header.add(titleSection, backButton);
        header.setFlexGrow(1, titleSection);
        header.setFlexGrow(0, backButton);

        return header;
    }

    private Component createEditForm() {
        VerticalLayout formSection = new VerticalLayout();
        formSection.setWidthFull();
        formSection.setPadding(true);
        formSection.setSpacing(true);
        formSection.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        // Titre
        titreField = new TextField("Titre *");
        titreField.setWidthFull();
        titreField.setRequired(true);
        titreField.setRequiredIndicatorVisible(true);

        // Description
        descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();
        descriptionField.setHeight("100px");

        // Catégorie
        categorieComboBox = new ComboBox<>("Catégorie *");
        categorieComboBox.setItems(EventCategorie.values());
        categorieComboBox.setItemLabelGenerator(EventCategorie::getLabel);
        categorieComboBox.setWidthFull();
        categorieComboBox.setRequired(true);
        categorieComboBox.setRequiredIndicatorVisible(true);

        // Lieu et Ville
        HorizontalLayout locationLayout = new HorizontalLayout();
        locationLayout.setWidthFull();
        locationLayout.setSpacing(true);

        lieuField = new TextField("Lieu *");
        lieuField.setWidthFull();
        lieuField.setRequired(true);
        lieuField.setRequiredIndicatorVisible(true);

        villeField = new TextField("Ville *");
        villeField.setWidthFull();
        villeField.setRequired(true);
        villeField.setRequiredIndicatorVisible(true);

        locationLayout.add(lieuField, villeField);

        // Prix et Capacité
        HorizontalLayout priceCapacityLayout = new HorizontalLayout();
        priceCapacityLayout.setWidthFull();
        priceCapacityLayout.setSpacing(true);

        prixField = new NumberField("Prix unitaire (DH) *");
        prixField.setWidthFull();
        prixField.setMin(0);
        prixField.setRequired(true);
        prixField.setRequiredIndicatorVisible(true);
        prixField.setPrefixComponent(new Span("DH"));

        capaciteField = new IntegerField("Capacité maximale *");
        capaciteField.setWidthFull();
        capaciteField.setMin(1);
        capaciteField.setRequired(true);
        capaciteField.setRequiredIndicatorVisible(true);

        priceCapacityLayout.add(prixField, capaciteField);

        // Dates section
        VerticalLayout datesSection = createDatesSection();

        // Image section
        VerticalLayout imageSection = createImageSection();

        // Buttons
        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setWidthFull();
        buttonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonsLayout.setSpacing(true);

        Button cancelButton = new Button("Annuler", new Icon(VaadinIcon.CLOSE));
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("organizer/events")));

        Button saveButton = new Button("Enregistrer", new Icon(VaadinIcon.CHECK));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveEvent());

        buttonsLayout.add(cancelButton, saveButton);

        formSection.add(
                titreField,
                descriptionField,
                categorieComboBox,
                locationLayout,
                priceCapacityLayout,
                datesSection,
                imageSection,
                buttonsLayout
        );

        return formSection;
    }

    private VerticalLayout createDatesSection() {
        VerticalLayout datesSection = new VerticalLayout();
        datesSection.setPadding(true);
        datesSection.setSpacing(true);
        datesSection.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background", "var(--lumo-contrast-5pct)");

        Span datesTitle = new Span("Dates de l'événement");
        datesTitle.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("color", "var(--lumo-primary-text-color)");

        // Date de début
        HorizontalLayout startDateLayout = new HorizontalLayout();
        startDateLayout.setWidthFull();
        startDateLayout.setSpacing(true);

        dateDebutPicker = new DatePicker("Date de début *");
        dateDebutPicker.setWidthFull();
        dateDebutPicker.setRequired(true);
        dateDebutPicker.setRequiredIndicatorVisible(true);

        heureDebutPicker = new TimePicker("Heure de début *");
        heureDebutPicker.setWidthFull();
        heureDebutPicker.setRequired(true);
        heureDebutPicker.setRequiredIndicatorVisible(true);

        startDateLayout.add(dateDebutPicker, heureDebutPicker);

        // Date de fin
        HorizontalLayout endDateLayout = new HorizontalLayout();
        endDateLayout.setWidthFull();
        endDateLayout.setSpacing(true);

        dateFinPicker = new DatePicker("Date de fin");
        dateFinPicker.setWidthFull();

        heureFinPicker = new TimePicker("Heure de fin");
        heureFinPicker.setWidthFull();

        endDateLayout.add(dateFinPicker, heureFinPicker);

        datesSection.add(datesTitle, startDateLayout, endDateLayout);
        return datesSection;
    }

    private VerticalLayout createImageSection() {
        VerticalLayout imageSection = new VerticalLayout();
        imageSection.setPadding(true);
        imageSection.setSpacing(true);
        imageSection.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background", "var(--lumo-contrast-5pct)");

        H4 imageTitle = new H4("Image de l'événement");
        imageTitle.getStyle()
                .set("margin", "0 0 10px 0")
                .set("color", "var(--lumo-primary-text-color)");

        // Current image preview
        Div currentImageContainer = new Div();
        currentImageContainer.setWidthFull();
        currentImageContainer.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("margin-bottom", "20px");

        Div imageWrapper = new Div();
        imageWrapper.setHeight("150px");
        imageWrapper.setWidth("150px");
        imageWrapper.getStyle()
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "2px solid var(--lumo-contrast-20pct)")
                .set("overflow", "hidden")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("background", "var(--lumo-contrast-10pct)");

        currentImage = new Image();
        currentImage.setHeight("100%");
        currentImage.setWidth("100%");
        currentImage.getStyle().set("object-fit", "cover");

        currentImageLabel = new Span();
        currentImageLabel.getStyle()
                .set("margin-top", "10px")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        imageWrapper.add(currentImage);
        currentImageContainer.add(imageWrapper, currentImageLabel);

        // Upload component
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setWidthFull();
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp");
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.setDropLabel(new Span("Glissez-déposez une image ici"));

        Button uploadButton = new Button("Parcourir les fichiers", new Icon(VaadinIcon.UPLOAD));
        uploadButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        upload.setUploadButton(uploadButton);

        Span uploadLabel = new Span("Téléchargez une nouvelle image");
        uploadLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("display", "block")
                .set("margin-bottom", "5px");

        Span uploadInfo = new Span("Formats acceptés: JPG, PNG, GIF, WebP (max 5MB)");
        uploadInfo.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("display", "block")
                .set("margin-bottom", "15px");

        // Preview for new image
        previewContainer = new Div();
        previewContainer.setWidthFull();
        previewContainer.getStyle()
                .set("display", "none")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("margin-top", "15px");

        previewImage = new Image();
        previewImage.setHeight("150px");
        previewImage.setWidth("150px");
        previewImage.getStyle()
                .set("object-fit", "cover")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "2px solid var(--lumo-success-color)");

        Span previewLabel = new Span("Nouvelle image");
        previewLabel.getStyle()
                .set("margin-top", "10px")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-success-color)")
                .set("font-weight", "500");

        previewContainer.add(previewImage, previewLabel);

        // Upload listeners
        upload.addSucceededListener(e -> {
            try {
                uploadedFileName = e.getFileName();
                uploadedFileType = e.getMIMEType();

                InputStream fileData = buffer.getInputStream();
                byte[] bytes = fileData.readAllBytes();
                uploadedFileData.reset();
                uploadedFileData.write(bytes);

                String base64Image = Base64.getEncoder().encodeToString(bytes);
                String dataUrl = "data:" + uploadedFileType + ";base64," + base64Image;

                previewImage.setSrc(dataUrl);
                previewImage.setAlt("Nouvelle image: " + uploadedFileName);
                previewContainer.getStyle().set("display", "flex");

                Notification.show("✓ Image téléchargée: " + uploadedFileName,
                                3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                Notification.show("Erreur lors du traitement de l'image",
                                3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        upload.addFileRejectedListener(e -> {
            Notification.show("Erreur: " + e.getErrorMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        // Remove button
        removeImageButton = new Button("Supprimer l'image actuelle", new Icon(VaadinIcon.TRASH));
        removeImageButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        removeImageButton.addClickListener(e -> removeCurrentImage());

        imageSection.add(imageTitle, currentImageContainer);
        imageSection.add(removeImageButton);
        imageSection.add(uploadLabel, uploadInfo, upload, previewContainer);

        return imageSection;
    }

    private void populateForm() {
        if (currentEvent == null) return;

        titreField.setValue(currentEvent.getTitre());
        descriptionField.setValue(currentEvent.getDescription() != null ? currentEvent.getDescription() : "");
        categorieComboBox.setValue(currentEvent.getCategorie());
        lieuField.setValue(currentEvent.getLieu() != null ? currentEvent.getLieu() : "");
        villeField.setValue(currentEvent.getVille() != null ? currentEvent.getVille() : "");
        prixField.setValue(currentEvent.getPrixUnitaire() != null ? currentEvent.getPrixUnitaire() : 0.0);
        capaciteField.setValue(currentEvent.getCapaciteMax() != null ? currentEvent.getCapaciteMax() : 0);

        if (currentEvent.getDateDebut() != null) {
            dateDebutPicker.setValue(currentEvent.getDateDebut().toLocalDate());
            heureDebutPicker.setValue(currentEvent.getDateDebut().toLocalTime());
        }

        if (currentEvent.getDateFin() != null) {
            dateFinPicker.setValue(currentEvent.getDateFin().toLocalDate());
            heureFinPicker.setValue(currentEvent.getDateFin().toLocalTime());
        }

        loadCurrentImage();
    }

    private void loadCurrentImage() {
        if (currentEvent.getImageUrl() != null && !currentEvent.getImageUrl().isEmpty()) {
            // Use ImageUtils to create the image
            Image image = ImageUtils.createEventImage(currentEvent.getImageUrl(), "150px");
            currentImage.setSrc(image.getSrc());
            currentImage.setAlt("Image actuelle");
            currentImageLabel.setText("Image actuelle");
            removeImageButton.setVisible(true);
        } else {
            setPlaceholderImage("Aucune image définie");
            removeImageButton.setVisible(false);
        }
    }
    private void setPlaceholderImage(String message) {
        currentImage.setSrc("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='150' height='150' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='1'%3E%3Crect x='3' y='3' width='18' height='18' rx='2' ry='2'%3E%3C/rect%3E%3Ccircle cx='8.5' cy='8.5' r='1.5'%3E%3C/circle%3E%3Cpolyline points='21 15 16 10 5 21'%3E%3C/polyline%3E%3C/svg%3E");
        currentImage.setAlt(message);
        currentImageLabel.setText(message);
    }
    private void removeCurrentImage() {
        currentEvent.setImageUrl(null);
        setPlaceholderImage("Image supprimée");
        removeImageButton.setVisible(false);
        previewContainer.getStyle().set("display", "none");
        uploadedFileData.reset();
        uploadedFileName = null;
        uploadedFileType = null;

        Notification.show("Image supprimée", 2000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (titreField.getValue() == null || titreField.getValue().trim().isEmpty()) {
            titreField.setErrorMessage("Le titre est obligatoire");
            titreField.setInvalid(true);
            isValid = false;
        } else {
            titreField.setInvalid(false);
        }

        if (capaciteField.getValue() == null || capaciteField.getValue() <= 0) {
            capaciteField.setErrorMessage("La capacité doit être supérieure à 0");
            capaciteField.setInvalid(true);
            isValid = false;
        } else {
            capaciteField.setInvalid(false);
        }

        if (prixField.getValue() == null || prixField.getValue() < 0) {
            prixField.setErrorMessage("Le prix doit être positif ou zéro");
            prixField.setInvalid(true);
            isValid = false;
        } else {
            prixField.setInvalid(false);
        }

        if (dateDebutPicker.getValue() == null) {
            dateDebutPicker.setErrorMessage("La date de début est obligatoire");
            dateDebutPicker.setInvalid(true);
            isValid = false;
        } else {
            dateDebutPicker.setInvalid(false);
        }

        if (heureDebutPicker.getValue() == null) {
            heureDebutPicker.setErrorMessage("L'heure de début est obligatoire");
            heureDebutPicker.setInvalid(true);
            isValid = false;
        } else {
            heureDebutPicker.setInvalid(false);
        }

        if (lieuField.getValue() == null || lieuField.getValue().trim().isEmpty()) {
            lieuField.setErrorMessage("Le lieu est obligatoire");
            lieuField.setInvalid(true);
            isValid = false;
        } else {
            lieuField.setInvalid(false);
        }

        if (villeField.getValue() == null || villeField.getValue().trim().isEmpty()) {
            villeField.setErrorMessage("La ville est obligatoire");
            villeField.setInvalid(true);
            isValid = false;
        } else {
            villeField.setInvalid(false);
        }

        if (categorieComboBox.getValue() == null) {
            categorieComboBox.setErrorMessage("La catégorie est obligatoire");
            categorieComboBox.setInvalid(true);
            isValid = false;
        } else {
            categorieComboBox.setInvalid(false);
        }

        return isValid;
    }

    private void saveEvent() {
        if (!validateForm()) {
            return;
        }

        try {
            if (uploadedFileData.size() > 0 && uploadedFileName != null) {
                String imageUrl = saveUploadedImage();
                if (imageUrl != null) {
                    currentEvent.setImageUrl(imageUrl);
                }
            }

            currentEvent.setTitre(titreField.getValue().trim());
            currentEvent.setDescription(descriptionField.getValue() != null ? descriptionField.getValue().trim() : null);
            currentEvent.setCategorie(categorieComboBox.getValue());
            currentEvent.setLieu(lieuField.getValue().trim());
            currentEvent.setVille(villeField.getValue().trim());
            currentEvent.setPrixUnitaire(prixField.getValue());
            currentEvent.setCapaciteMax(capaciteField.getValue());

            LocalDate debutDate = dateDebutPicker.getValue();
            LocalTime debutTime = heureDebutPicker.getValue();
            if (debutDate != null && debutTime != null) {
                currentEvent.setDateDebut(LocalDateTime.of(debutDate, debutTime));
            }

            LocalDate finDate = dateFinPicker.getValue();
            LocalTime finTime = heureFinPicker.getValue();
            if (finDate != null && finTime != null) {
                currentEvent.setDateFin(LocalDateTime.of(finDate, finTime));
            } else {
                currentEvent.setDateFin(null);
            }

            eventService.saveEvent(currentEvent);

            Notification.show("Événement modifié avec succès",
                            3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            getUI().ifPresent(ui -> ui.navigate("organizer/events"));

        } catch (Exception e) {
            Notification.show("Erreur lors de la modification: " + e.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private String saveUploadedImage() {
        try {
            String extension = getFileExtension(uploadedFileName);
            String uniqueFileName = "event_" + currentEvent.getId() + "_" +
                    System.currentTimeMillis() + extension;

            // Get project directory
            String projectDir = System.getProperty("user.dir");
            String uploadDir = projectDir + "/src/main/java/org/example/reservation_event/uploads/";

            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File outputFile = new File(uploadDir + uniqueFileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(uploadedFileData.toByteArray());
            }

            // Return relative path or just filename
            return uniqueFileName; // ImageUtils will construct the full path

        } catch (Exception e) {
            Notification.show("Erreur lors de la sauvegarde de l'image: " + e.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return null;
        }
    }
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
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

    private boolean isOrganizer(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String roleName = user.getRole().name();
        return roleName.equals("ORGANIZER") || roleName.equals("ADMIN");
    }

    private boolean canManageEvent(Event event) {
        return event != null && event.getOrganisateur() != null &&
                event.getOrganisateur().getId().equals(currentUser.getId());
    }

    private void showAccessDenied() {
        removeAll();
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.getStyle().set("background", "var(--lumo-contrast-5pct)");

        Icon icon = new Icon(VaadinIcon.BAN);
        icon.setSize("64px");
        icon.setColor("var(--lumo-error-color)");

        H1 title = new H1("Accès refusé");
        title.getStyle().set("color", "var(--lumo-error-text-color)");

        Paragraph message = new Paragraph("Vous n'avez pas les autorisations nécessaires pour accéder à cette page.");
        message.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button backButton = new Button("Retour aux événements", e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/events")));
        backButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(icon, title, message, backButton);
        add(layout);
    }

    private void showEventNotFound() {
        removeAll();
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.getStyle().set("background", "var(--lumo-contrast-5pct)");

        Icon icon = new Icon(VaadinIcon.WARNING);
        icon.setSize("64px");
        icon.setColor("var(--lumo-warning-color)");

        H1 title = new H1("Événement introuvable");
        title.getStyle().set("color", "var(--lumo-warning-text-color)");

        Paragraph message = new Paragraph("L'événement que vous recherchez n'existe pas ou a été supprimé.");
        message.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button backButton = new Button("Retour aux événements", e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/events")));
        backButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(icon, title, message, backButton);
        add(layout);
    }
}