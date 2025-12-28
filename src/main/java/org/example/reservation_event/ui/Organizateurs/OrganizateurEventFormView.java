package org.example.reservation_event.ui.Organizateurs;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.DoubleRangeValidator;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.RolesAllowed;
import org.example.reservation_event.Enums.EventCategorie;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.Enums.UserRole;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.User;
import org.example.reservation_event.services.EventService;
import org.example.reservation_event.services.UserService;
import org.example.reservation_event.ui.MainLayout;
import org.example.reservation_event.utils.ImageUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Base64;

@Route(value = "organizer/event", layout = MainLayout.class)
@PageTitle("Gestion d'Événement")
public class OrganizateurEventFormView extends VerticalLayout implements HasUrlParameter<String> {

    private static final String UPLOAD_DIR = "src/main/java/org/example/reservation_event/uploads/";
    private final UserService userService;
    private final EventService eventService;
    private User currentUser;
    private Event event;
    private boolean isEditMode = false;

    // Form fields
    private TextField titreField;
    private TextArea descriptionField;
    private ComboBox<EventCategorie> categorieCombo;
    private DateTimePicker dateDebutPicker;
    private DateTimePicker dateFinPicker;
    private TextField lieuField;
    private TextField villeField;
    private NumberField capaciteMaxField;
    private NumberField prixUnitaireField;

    // Image upload fields
    private Image currentImage;
    private Image previewImage;
    private Div previewContainer;
    private Span currentImageLabel;
    private Button removeImageButton;
    private ByteArrayOutputStream uploadedFileData;
    private String[] uploadedFileName;
    private String[] uploadedFileType;

    private Binder<Event> binder;
    private VerticalLayout previewSection;

    public OrganizateurEventFormView(UserService userService, EventService eventService) {
        this.userService = userService;
        this.eventService = eventService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("overflow-y", "auto");

        currentUser = getCurrentUser();
        if (currentUser == null) {
            showLoginRequired();
            return;
        }

        if (!isOrganizer(currentUser)) {
            showAccessDenied();
            return;
        }
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String parameter) {
        if (parameter != null) {
            if (parameter.equals("new")) {
                isEditMode = false;
                event = new Event();
                event.setOrganisateur(currentUser);
                event.setStatut(EventStatut.BROUILLON);
            } else if (parameter.startsWith("edit/")) {
                isEditMode = true;
                String eventId = parameter.substring(5);
                try {
                    Long id = Long.parseLong(eventId);
                    Event foundEvent = eventService.getEventById(id);
                    if (foundEvent != null) {
                        event = foundEvent;
                        if (!canEditEvent(event)) {
                            showAccessDenied();
                            return;
                        }
                    } else {
                        showEventNotFound();
                        return;
                    }
                } catch (NumberFormatException e) {
                    showEventNotFound();
                    return;
                } catch (Exception e) {
                    showEventNotFound();
                    return;
                }
            }
        } else {
            isEditMode = false;
            event = new Event();
            event.setOrganisateur(currentUser);
            event.setStatut(EventStatut.BROUILLON);
        }

        initializeLayout();
    }

    private void initializeLayout() {
        removeAll();

        // Container principal avec padding
        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setSizeFull();
        mainContainer.setPadding(true);
        mainContainer.setSpacing(true);

        mainContainer.add(
                createHeader(),
                createFormSection(),
                createPreviewSection()
        );

        add(mainContainer);
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
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("margin-bottom", "var(--lumo-space-m)");

        // Icône et titre
        Icon headerIcon = new Icon(isEditMode ? VaadinIcon.EDIT : VaadinIcon.PLUS_CIRCLE);
        headerIcon.setSize("32px");
        headerIcon.getStyle().set("color", "var(--lumo-primary-color)");

        H1 title = new H1(isEditMode ? "Modifier l'événement" : "Créer un événement");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        // Back button
        Button backBtn = new Button("Retour", new Icon(VaadinIcon.ARROW_LEFT));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("organizer/events")));

        header.add(headerIcon, title);
        header.expand(title);
        header.add(backBtn);

        return header;
    }

    private Component createFormSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        H3 sectionTitle = new H3("📝 Informations de l'événement");
        sectionTitle.getStyle()
                .set("margin", "0 0 var(--lumo-space-m) 0")
                .set("color", "var(--lumo-primary-text-color)");

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        // Initialize form fields
        titreField = new TextField("Titre de l'événement");
        titreField.setPlaceholder("Ex: Concert de Jazz");
        titreField.setClearButtonVisible(true);
        titreField.setRequiredIndicatorVisible(true);

        descriptionField = new TextArea("Description");
        descriptionField.setPlaceholder("Décrivez votre événement en détail...");
        descriptionField.setMaxLength(1000);
        descriptionField.setHelperText("Maximum 1000 caractères");
        descriptionField.setRequiredIndicatorVisible(true);

        categorieCombo = new ComboBox<>("Catégorie");
        categorieCombo.setItems(EventCategorie.values());
        categorieCombo.setItemLabelGenerator(EventCategorie::getLabel);
        categorieCombo.setPlaceholder("Sélectionnez une catégorie");
        categorieCombo.setRequiredIndicatorVisible(true);

        dateDebutPicker = new DateTimePicker("Date et heure de début");
        dateDebutPicker.setRequiredIndicatorVisible(true);
        dateDebutPicker.setHelperText("L'événement doit être dans le futur");

        dateFinPicker = new DateTimePicker("Date et heure de fin");
        dateFinPicker.setRequiredIndicatorVisible(true);
        dateFinPicker.setHelperText("Doit être après la date de début");

        lieuField = new TextField("Lieu");
        lieuField.setPlaceholder("Ex: Théâtre Mohamed V");
        lieuField.setClearButtonVisible(true);
        lieuField.setRequiredIndicatorVisible(true);

        villeField = new TextField("Ville");
        villeField.setPlaceholder("Ex: Casablanca");
        villeField.setClearButtonVisible(true);
        villeField.setRequiredIndicatorVisible(true);

        capaciteMaxField = new NumberField("Capacité maximale");
        capaciteMaxField.setPlaceholder("Nombre de places");
        capaciteMaxField.setMin(1);
        capaciteMaxField.setStep(1);
        capaciteMaxField.setRequiredIndicatorVisible(true);
        capaciteMaxField.setHelperText("Nombre total de places disponibles");

        prixUnitaireField = new NumberField("Prix unitaire (DH)");
        prixUnitaireField.setPlaceholder("Prix par place");
        prixUnitaireField.setMin(0);
        prixUnitaireField.setStep(0.01);
        prixUnitaireField.setRequiredIndicatorVisible(true);
        prixUnitaireField.setPrefixComponent(new Span("DH"));

        // Setup binder with validation
        binder = new Binder<>(Event.class);

        binder.forField(titreField)
                .withValidator(new StringLengthValidator(
                        "Le titre doit contenir entre 5 et 100 caractères", 5, 100))
                .bind(Event::getTitre, Event::setTitre);

        binder.forField(descriptionField)
                .withValidator(new StringLengthValidator(
                        "La description ne doit pas dépasser 1000 caractères", 0, 1000))
                .bind(Event::getDescription, Event::setDescription);

        binder.forField(categorieCombo)
                .asRequired("La catégorie est obligatoire")
                .bind(Event::getCategorie, Event::setCategorie);

        binder.forField(dateDebutPicker)
                .asRequired("La date de début est obligatoire")
                .withValidator(date -> date == null || date.isAfter(LocalDateTime.now()),
                        "La date de début doit être dans le futur")
                .bind(Event::getDateDebut, Event::setDateDebut);

        binder.forField(dateFinPicker)
                .asRequired("La date de fin est obligatoire")
                .withValidator(date -> {
                    if (date == null || dateDebutPicker.getValue() == null) return true;
                    return date.isAfter(dateDebutPicker.getValue());
                }, "La date de fin doit être après la date de début")
                .bind(Event::getDateFin, Event::setDateFin);

        binder.forField(lieuField)
                .asRequired("Le lieu est obligatoire")
                .bind(Event::getLieu, Event::setLieu);

        binder.forField(villeField)
                .asRequired("La ville est obligatoire")
                .bind(Event::getVille, Event::setVille);

        binder.forField(capaciteMaxField)
                .asRequired("La capacité maximale est obligatoire")
                .withConverter(
                        value -> value != null ? value.intValue() : null,
                        value -> value != null ? value.doubleValue() : null
                )
                .withValidator(new IntegerRangeValidator(
                        "La capacité doit être supérieure à 0", 1, Integer.MAX_VALUE))
                .bind(Event::getCapaciteMax, Event::setCapaciteMax);

        binder.forField(prixUnitaireField)
                .asRequired("Le prix unitaire est obligatoire")
                .withValidator(new DoubleRangeValidator(
                        "Le prix doit être supérieur ou égal à 0", 0.0, Double.MAX_VALUE))
                .bind(Event::getPrixUnitaire, Event::setPrixUnitaire);

        // Load event data if editing
        if (event != null) {
            binder.readBean(event);
        }

        // Real-time preview update
        titreField.addValueChangeListener(e -> updatePreview());
        descriptionField.addValueChangeListener(e -> updatePreview());
        categorieCombo.addValueChangeListener(e -> updatePreview());
        dateDebutPicker.addValueChangeListener(e -> updatePreview());
        dateFinPicker.addValueChangeListener(e -> updatePreview());
        lieuField.addValueChangeListener(e -> updatePreview());
        villeField.addValueChangeListener(e -> updatePreview());
        capaciteMaxField.addValueChangeListener(e -> updatePreview());
        prixUnitaireField.addValueChangeListener(e -> updatePreview());

        formLayout.add(titreField, categorieCombo);
        formLayout.add(descriptionField, 2);
        formLayout.add(dateDebutPicker, dateFinPicker);
        formLayout.add(lieuField, villeField);
        formLayout.add(capaciteMaxField, prixUnitaireField);

        Component imageUploadSection = createImageUploadSection();
        HorizontalLayout buttonsLayout = createButtonsLayout();

        section.add(sectionTitle, formLayout, imageUploadSection, buttonsLayout);
        return section;
    }

    private Component createImageUploadSection() {
        VerticalLayout imageSection = new VerticalLayout();
        imageSection.setWidthFull();
        imageSection.setPadding(true);
        imageSection.setSpacing(true);
        imageSection.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-top", "var(--lumo-space-m)")
                .set("background", "var(--lumo-contrast-5pct)");

        H4 imageTitle = new H4("📷 Image de l'événement");
        imageTitle.getStyle()
                .set("margin", "0 0 var(--lumo-space-m) 0")
                .set("color", "var(--lumo-primary-text-color)");

        // Current image preview container
        Div currentImageContainer = new Div();
        currentImageContainer.setWidthFull();
        currentImageContainer.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("margin-bottom", "var(--lumo-space-m)");

        // Image wrapper
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
                .set("margin-top", "var(--lumo-space-s)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        imageWrapper.add(currentImage);
        currentImageContainer.add(imageWrapper, currentImageLabel);

        // File upload component
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setWidthFull();
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp");
        upload.setMaxFileSize(5 * 1024 * 1024); // 5MB
        upload.setDropLabel(new Span("Glissez-déposez une image ici"));

        Button uploadButton = new Button("Parcourir les fichiers", new Icon(VaadinIcon.UPLOAD));
        uploadButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        upload.setUploadButton(uploadButton);

        Span uploadLabel = new Span("Téléchargez une image depuis votre ordinateur");
        uploadLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-xs)");

        Span uploadInfo = new Span("Formats acceptés: JPG, PNG, GIF, WebP (max 5MB)");
        uploadInfo.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-m)");

        // Preview for newly uploaded image
        previewContainer = new Div();
        previewContainer.setWidthFull();
        previewContainer.getStyle()
                .set("display", "none")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("margin-top", "var(--lumo-space-m)");

        previewImage = new Image();
        previewImage.setHeight("150px");
        previewImage.setWidth("150px");
        previewImage.getStyle()
                .set("object-fit", "cover")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "2px solid var(--lumo-success-color)");

        Span previewLabel = new Span("Nouvelle image");
        previewLabel.getStyle()
                .set("margin-top", "var(--lumo-space-s)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-success-text-color)")
                .set("font-weight", "500");

        previewContainer.add(previewImage, previewLabel);

        uploadedFileData = new ByteArrayOutputStream();
        uploadedFileName = new String[1];
        uploadedFileType = new String[1];

        upload.addSucceededListener(e -> {
            try {
                uploadedFileName[0] = e.getFileName();
                uploadedFileType[0] = e.getMIMEType();

                InputStream fileData = buffer.getInputStream();
                byte[] bytes = fileData.readAllBytes();
                uploadedFileData.reset();
                uploadedFileData.write(bytes);

                String base64Image = Base64.getEncoder().encodeToString(bytes);
                String dataUrl = "data:" + uploadedFileType[0] + ";base64," + base64Image;

                previewImage.setSrc(dataUrl);
                previewImage.setAlt("Nouvelle image: " + uploadedFileName[0]);
                previewContainer.getStyle().set("display", "flex");

                updatePreview();
                showSuccessNotification("✅ Image téléchargée: " + uploadedFileName[0]);

            } catch (Exception ex) {
                showErrorNotification("Erreur lors du traitement de l'image");
            }
        });

        upload.addFileRejectedListener(e -> {
            showErrorNotification("Erreur: " + e.getErrorMessage());
        });

        // Remove image button - MUST BE CREATED BEFORE loadCurrentImage()
        removeImageButton = new Button("Supprimer l'image actuelle", new Icon(VaadinIcon.TRASH));
        removeImageButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        removeImageButton.addClickListener(e -> {
            event.setImageUrl(null);
            loadCurrentImage();
            previewContainer.getStyle().set("display", "none");
            uploadedFileData.reset();
            uploadedFileName[0] = null;
            uploadedFileType[0] = null;
            removeImageButton.setVisible(false);
            showSuccessNotification("Image supprimée");
        });

        // NOW call loadCurrentImage() AFTER removeImageButton is created
        loadCurrentImage();

        imageSection.add(imageTitle, currentImageContainer, removeImageButton,
                uploadLabel, uploadInfo, upload, previewContainer);
        return imageSection;
    }

    private void loadCurrentImage() {
        if (event != null && event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            // Use ImageUtils to create the image
            Image image = ImageUtils.createEventImage(event.getImageUrl(), "150px");
            currentImage.setSrc(image.getSrc());
            currentImage.setAlt("Image actuelle");
            currentImageLabel.setText("Image actuelle");
            if (removeImageButton != null) {
                removeImageButton.setVisible(true);
            }
        } else {
            setPlaceholderImage("Aucune image définie");
            if (removeImageButton != null) {
                removeImageButton.setVisible(false);
            }
        }
    }
    private void setPlaceholderImage(String label) {
        currentImage.setSrc("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='150' height='150' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='1'%3E%3Crect x='3' y='3' width='18' height='18' rx='2' ry='2'%3E%3C/rect%3E%3Ccircle cx='8.5' cy='8.5' r='1.5'%3E%3C/circle%3E%3Cpolyline points='21 15 16 10 5 21'%3E%3C/polyline%3E%3C/svg%3E");
        currentImage.setAlt(label);
        currentImageLabel.setText(label);
    }

    private HorizontalLayout createButtonsLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        layout.setSpacing(true);
        layout.getStyle().set("margin-top", "var(--lumo-space-m)");

        Button cancelBtn = new Button("Annuler", new Icon(VaadinIcon.CLOSE));
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("organizer/events")));

        Button saveDraftBtn = new Button("Sauvegarder en brouillon", new Icon(VaadinIcon.FILE));
        saveDraftBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        saveDraftBtn.addClickListener(e -> saveEvent(EventStatut.BROUILLON));

        Button publishBtn = new Button("Publier", new Icon(VaadinIcon.CHECK_CIRCLE));
        publishBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        publishBtn.addClickListener(e -> saveEvent(EventStatut.PUBLIE));

        layout.add(cancelBtn, saveDraftBtn, publishBtn);
        return layout;
    }

    private Component createPreviewSection() {
        previewSection = new VerticalLayout();
        previewSection.setWidthFull();
        previewSection.setPadding(true);
        previewSection.setSpacing(true);
        previewSection.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        H3 previewTitle = new H3("👁️ Aperçu");
        previewTitle.getStyle()
                .set("margin", "0 0 var(--lumo-space-m) 0")
                .set("color", "var(--lumo-primary-text-color)");

        previewSection.add(previewTitle);
        updatePreview();

        return previewSection;
    }

    private void updatePreview() {
        if (previewSection == null) return;

        while (previewSection.getComponentCount() > 1) {
            previewSection.remove(previewSection.getComponentAt(1));
        }

        VerticalLayout preview = new VerticalLayout();
        preview.setWidthFull();
        preview.setPadding(true);
        preview.setSpacing(true);
        preview.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "2px dashed var(--lumo-contrast-30pct)");

        // Image preview
        if (uploadedFileData != null && uploadedFileData.size() > 0 && uploadedFileName[0] != null) {
            try {
                String base64Image = Base64.getEncoder().encodeToString(uploadedFileData.toByteArray());
                String dataUrl = "data:" + uploadedFileType[0] + ";base64," + base64Image;
                Image previewImg = new Image(dataUrl, "New Event image");
                previewImg.setWidth("100%");
                previewImg.setMaxHeight("300px");
                previewImg.getStyle()
                        .set("object-fit", "cover")
                        .set("border-radius", "var(--lumo-border-radius-m)");
                preview.add(previewImg);
            } catch (Exception e) {
                addImagePlaceholder(preview);
            }
        } else if (event != null && event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            try {
                String imagePath = event.getImageUrl();
                String fileName = imagePath.contains("/")
                        ? imagePath.substring(imagePath.lastIndexOf("/") + 1)
                        : imagePath;
                File imageFile = new File(UPLOAD_DIR + fileName);

                if (imageFile.exists() && imageFile.canRead()) {
                    StreamResource streamResource = new StreamResource(
                            fileName,
                            () -> {
                                try {
                                    return new FileInputStream(imageFile);
                                } catch (FileNotFoundException e) {
                                    return new ByteArrayInputStream(new byte[0]);
                                }
                            }
                    );
                    Image previewImg = new Image(streamResource, "Event image");
                    previewImg.setWidth("100%");
                    previewImg.setMaxHeight("300px");
                    previewImg.getStyle()
                            .set("object-fit", "cover")
                            .set("border-radius", "var(--lumo-border-radius-m)");
                    preview.add(previewImg);
                } else {
                    addImagePlaceholder(preview);
                }
            } catch (Exception e) {
                addImagePlaceholder(preview);
            }
        } else {
            addImagePlaceholder(preview);
        }

        // Title
        H2 title = new H2(titreField.getValue() != null && !titreField.getValue().isEmpty()
                ? titreField.getValue() : "Titre de l'événement");
        title.getStyle()
                .set("margin", "var(--lumo-space-s) 0")
                .set("color", "var(--lumo-primary-text-color)");

        // Category badge
        if (categorieCombo.getValue() != null) {
            Span categoryBadge = new Span(categorieCombo.getValue().getLabel());
            categoryBadge.getStyle()
                    .set("background", "var(--lumo-primary-color)")
                    .set("color", "var(--lumo-primary-contrast-color)")
                    .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("display", "inline-block")
                    .set("margin-bottom", "var(--lumo-space-s)");
            preview.add(categoryBadge);
        }

        preview.add(title);

        // Description
        Span description = new Span(descriptionField.getValue() != null && !descriptionField.getValue().isEmpty()
                ? descriptionField.getValue() : "Description de l'événement...");
        description.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "var(--lumo-space-m)");
        preview.add(description);

        // Details grid
        HorizontalLayout detailsGrid = new HorizontalLayout();
        detailsGrid.setWidthFull();
        detailsGrid.setSpacing(true);
        detailsGrid.getStyle().set("flex-wrap", "wrap");

        if (dateDebutPicker.getValue() != null) {
            detailsGrid.add(createDetailItem(VaadinIcon.CALENDAR, "Date",
                    formatDateTime(dateDebutPicker.getValue())));
        }

        if (lieuField.getValue() != null && !lieuField.getValue().isEmpty()) {
            detailsGrid.add(createDetailItem(VaadinIcon.MAP_MARKER, "Lieu",
                    lieuField.getValue() + (villeField.getValue() != null ? ", " + villeField.getValue() : "")));
        }

        if (capaciteMaxField.getValue() != null) {
            detailsGrid.add(createDetailItem(VaadinIcon.USERS, "Capacité",
                    capaciteMaxField.getValue().intValue() + " places"));
        }

        if (prixUnitaireField.getValue() != null) {
            detailsGrid.add(createDetailItem(VaadinIcon.MONEY, "Prix",
                    String.format("%.2f DH", prixUnitaireField.getValue())));
        }

        preview.add(detailsGrid);
        previewSection.add(preview);
    }

    private void addImagePlaceholder(VerticalLayout preview) {
        Div imagePlaceholder = new Div();
        imagePlaceholder.getStyle()
                .set("background", "var(--lumo-contrast-10pct)")
                .set("height", "200px")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center");

        Icon imageIcon = new Icon(VaadinIcon.PICTURE);
        imageIcon.setSize("48px");
        imageIcon.setColor("var(--lumo-contrast-50pct)");
        imagePlaceholder.add(imageIcon);

        preview.add(imagePlaceholder);
    }

    private Component createDetailItem(VaadinIcon icon, String label, String value) {
        HorizontalLayout item = new HorizontalLayout();
        item.setSpacing(true);
        item.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon itemIcon = new Icon(icon);
        itemIcon.setSize("16px");
        itemIcon.setColor("var(--lumo-primary-color)");

        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-weight", "500")
                .set("color", "var(--lumo-primary-text-color)");

        textLayout.add(labelSpan, valueSpan);
        item.add(itemIcon, textLayout);

        return item;
    }

    private void saveEvent(EventStatut statut) {
        try {
            binder.writeBean(event);

            if (statut == EventStatut.PUBLIE) {
                if (!validateEventForPublishing()) {
                    return;
                }
            }

            if (uploadedFileData != null && uploadedFileData.size() > 0 && uploadedFileName[0] != null) {
                try {
                    String imageUrl = saveUploadedImage(event, uploadedFileData.toByteArray(),
                            uploadedFileName[0], uploadedFileType[0]);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        event.setImageUrl(imageUrl);
                    }
                } catch (Exception ex) {
                    showErrorNotification("Erreur lors de la sauvegarde de l'image: " + ex.getMessage());
                    return;
                }
            }

            event.setStatut(statut);

            if (isEditMode) {
                event.setDateModification(LocalDateTime.now());
                eventService.updateEvent(event);
                showSuccessNotification(statut == EventStatut.PUBLIE
                        ? "Événement mis à jour et publié avec succès!"
                        : "Événement mis à jour avec succès!");
            } else {
                event.setDateCreation(LocalDateTime.now());
                event.setDateModification(LocalDateTime.now());
                eventService.createEvent(event, getCurrentUser());
                showSuccessNotification(statut == EventStatut.PUBLIE
                        ? "Événement créé et publié avec succès!"
                        : "Événement sauvegardé en brouillon!");
            }

            getUI().ifPresent(ui -> ui.navigate("organizer/events"));

        } catch (ValidationException e) {
            showErrorNotification("Veuillez corriger les erreurs dans le formulaire");
        } catch (Exception e) {
            showErrorNotification("Erreur lors de l'enregistrement: " + e.getMessage());
        }
    }

    private String saveUploadedImage(Event event, byte[] imageData, String fileName, String fileType) {
        try {
            String extension = getFileExtension(fileName);
            String uniqueFileName = "event_" +
                    (event.getId() != null ? event.getId() : System.currentTimeMillis()) +
                    "_" + System.currentTimeMillis() + extension;

            // Get project directory to make relative path absolute
            String projectDir = System.getProperty("user.dir");
            String absoluteUploadDir = projectDir + "/" + UPLOAD_DIR;

            File directory = new File(absoluteUploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File outputFile = new File(absoluteUploadDir + uniqueFileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(imageData);
            }

            // Return just the filename (ImageUtils will handle the path)
            return uniqueFileName;

        } catch (Exception e) {
            throw new RuntimeException("Image saving failed", e);
        }
    }
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex);
    }

    private boolean validateEventForPublishing() {
        if (titreField.isEmpty() || descriptionField.isEmpty() ||
                categorieCombo.isEmpty() || dateDebutPicker.isEmpty() ||
                dateFinPicker.isEmpty() || lieuField.isEmpty() ||
                villeField.isEmpty() || capaciteMaxField.isEmpty() ||
                prixUnitaireField.isEmpty()) {

            showErrorNotification("Tous les champs obligatoires doivent être remplis pour publier l'événement");
            return false;
        }

        if (dateDebutPicker.getValue().isBefore(LocalDateTime.now())) {
            showErrorNotification("La date de début doit être dans le futur");
            return false;
        }

        if (dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
            showErrorNotification("La date de fin doit être après la date de début");
            return false;
        }

        return true;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        return dateTime.format(formatter);
    }

    private void showSuccessNotification(String message) {
        Notification notification = new Notification(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.open();
    }

    private void showErrorNotification(String message) {
        Notification notification = new Notification(message, 4000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.open();
    }

    private boolean canEditEvent(Event event) {
        return event.getOrganisateur().getId().equals(currentUser.getId())
                || currentUser.getRole() == UserRole.ADMIN;
    }

    private void showLoginRequired() {
        removeAll();

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon icon = new Icon(VaadinIcon.EXCLAMATION_CIRCLE_O);
        icon.setSize("64px");
        icon.setColor("var(--lumo-warning-color)");

        H2 title = new H2("Connexion requise");
        title.getStyle().set("color", "var(--lumo-warning-text-color)");

        Span message = new Span("Vous devez être connecté pour accéder à cette page");
        message.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button loginBtn = new Button("Se connecter", e ->
                getUI().ifPresent(ui -> ui.navigate("login")));
        loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(icon, title, message, loginBtn);
        add(layout);
    }

    private void showAccessDenied() {
        removeAll();

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon icon = new Icon(VaadinIcon.BAN);
        icon.setSize("64px");
        icon.setColor("var(--lumo-error-color)");

        H2 title = new H2("Accès refusé");
        title.getStyle().set("color", "var(--lumo-error-text-color)");

        Span message = new Span("Vous n'avez pas l'autorisation de modifier cet événement");
        message.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button homeBtn = new Button("Retour", e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/events")));
        homeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(icon, title, message, homeBtn);
        add(layout);
    }

    private void showEventNotFound() {
        removeAll();

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon icon = new Icon(VaadinIcon.WARNING);
        icon.setSize("64px");
        icon.setColor("var(--lumo-warning-color)");

        H2 title = new H2("Événement introuvable");
        title.getStyle().set("color", "var(--lumo-warning-text-color)");

        Span message = new Span("L'événement que vous cherchez n'existe pas");
        message.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button backBtn = new Button("Retour aux événements", e ->
                getUI().ifPresent(ui -> ui.navigate("organizer/events")));
        backBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(icon, title, message, backBtn);
        add(layout);
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
        return user.getRole() == UserRole.ORGANIZER || user.getRole() == UserRole.ADMIN;
    }
}