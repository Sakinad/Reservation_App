package org.example.reservation_event.ui.Admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.example.reservation_event.Enums.EventCategorie;
import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.repositories.EventRepository;
import org.example.reservation_event.services.EventService;
import org.example.reservation_event.ui.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AllEventsManagementView - Gestion de tous les événements
 * Version corrigée avec Dialog de modification au lieu de redirection
 */
@Route(value = "admin/events", layout = MainLayout.class)
@PageTitle("Gestion des Événements | Event Manager")
@RolesAllowed("ADMIN")
public class AllEventsManagementView extends VerticalLayout {

    private final EventService eventService;
    private final EventRepository eventRepository;

    // Composants UI
    private Grid<Event> grid;
    private TextField searchField;
    private ComboBox<EventStatut> statusFilter;
    private TextField villeFilter;
    private DatePicker dateDebutFilter;
    private DatePicker dateFinFilter;
    private NumberField prixMinFilter;
    private NumberField prixMaxFilter;

    // Filtres actuels
    private String currentSearch = "";
    private EventStatut currentStatusFilter = null;
    private String currentVilleFilter = "";
    private LocalDateTime currentDateDebutFilter = null;
    private LocalDateTime currentDateFinFilter = null;
    private Double currentPrixMinFilter = null;
    private Double currentPrixMaxFilter = null;

    @Autowired
    public AllEventsManagementView(EventService eventService, EventRepository eventRepository) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createFilters();
        createGrid();

        loadEvents();
    }

    private void createHeader() {
        H1 title = new H1("Gestion des Événements");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "700");

        Span subtitle = new Span("Gérer tous les événements de la plateforme");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "14px");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setSpacing(false);
        header.setPadding(false);

        add(header);
    }

    private void createFilters() {
        // Première ligne de filtres
        searchField = new TextField();
        searchField.setPlaceholder("Rechercher...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidth("250px");
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            loadEvents();
        });

        statusFilter = new ComboBox<>("Statut");
        statusFilter.setItems(EventStatut.values());
        statusFilter.setItemLabelGenerator(EventStatut::getLabel);
        statusFilter.setPlaceholder("Tous les statuts");
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("180px");
        statusFilter.addValueChangeListener(e -> {
            currentStatusFilter = e.getValue();
            loadEvents();
        });

        villeFilter = new TextField("Ville");
        villeFilter.setPlaceholder("Toutes les villes");
        villeFilter.setClearButtonVisible(true);
        villeFilter.setWidth("180px");
        villeFilter.setValueChangeMode(ValueChangeMode.LAZY);
        villeFilter.addValueChangeListener(e -> {
            currentVilleFilter = e.getValue();
            loadEvents();
        });

        HorizontalLayout firstRow = new HorizontalLayout(searchField, statusFilter, villeFilter);
        firstRow.setAlignItems(Alignment.END);

        // Deuxième ligne de filtres
        dateDebutFilter = new DatePicker("Date début");
        dateDebutFilter.setPlaceholder("À partir de...");
        dateDebutFilter.setClearButtonVisible(true);
        dateDebutFilter.setWidth("180px");
        dateDebutFilter.addValueChangeListener(e -> {
            LocalDate date = e.getValue();
            currentDateDebutFilter = date != null ? date.atStartOfDay() : null;
            loadEvents();
        });

        dateFinFilter = new DatePicker("Date fin");
        dateFinFilter.setPlaceholder("Jusqu'à...");
        dateFinFilter.setClearButtonVisible(true);
        dateFinFilter.setWidth("180px");
        dateFinFilter.addValueChangeListener(e -> {
            LocalDate date = e.getValue();
            currentDateFinFilter = date != null ? date.atTime(LocalTime.MAX) : null;
            loadEvents();
        });

        prixMinFilter = new NumberField("Prix min");
        prixMinFilter.setPlaceholder("0");
        prixMinFilter.setSuffixComponent(new Span("DH"));
        prixMinFilter.setWidth("150px");
        prixMinFilter.setMin(0);
        prixMinFilter.addValueChangeListener(e -> {
            currentPrixMinFilter = e.getValue();
            loadEvents();
        });

        prixMaxFilter = new NumberField("Prix max");
        prixMaxFilter.setPlaceholder("∞");
        prixMaxFilter.setSuffixComponent(new Span("DH"));
        prixMaxFilter.setWidth("150px");
        prixMaxFilter.setMin(0);
        prixMaxFilter.addValueChangeListener(e -> {
            currentPrixMaxFilter = e.getValue();
            loadEvents();
        });

        Button resetButton = new Button("Réinitialiser", VaadinIcon.REFRESH.create());
        resetButton.addClickListener(e -> resetFilters());

        HorizontalLayout secondRow = new HorizontalLayout(
                dateDebutFilter, dateFinFilter, prixMinFilter, prixMaxFilter, resetButton
        );
        secondRow.setAlignItems(Alignment.END);

        VerticalLayout filters = new VerticalLayout(firstRow, secondRow);
        filters.setSpacing(true);
        filters.setPadding(false);
        filters.getStyle().set("margin-top", "20px");

        add(filters);
    }

    private void createGrid() {
        grid = new Grid<>(Event.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("600px");

        // Colonne Titre
        grid.addColumn(Event::getTitre)
                .setHeader("Titre")
                .setSortable(true)
                .setAutoWidth(true)
                .setFlexGrow(1);

        // Colonne Organisateur
        grid.addColumn(event -> event.getOrganisateur().getNomComplet())
                .setHeader("Organisateur")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Catégorie
        grid.addColumn(event -> event.getCategorie().getLabel())
                .setHeader("Catégorie")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Date
        grid.addColumn(event -> event.getDateDebut()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setHeader("Date")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Ville
        grid.addColumn(Event::getVille)
                .setHeader("Ville")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Prix
        grid.addColumn(event -> String.format("%.2f DH", event.getPrixUnitaire()))
                .setHeader("Prix")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Statut avec badge
        grid.addColumn(new ComponentRenderer<>(event -> {
                    Span badge = new Span(event.getStatut().getLabel());
                    badge.getElement().getThemeList().add("badge");

                    String theme = switch (event.getStatut()) {
                        case BROUILLON -> "contrast";
                        case PUBLIE -> "success";
                        case ANNULE -> "error";
                        case TERMINE -> "";
                    };

                    if (!theme.isEmpty()) {
                        badge.getElement().getThemeList().add(theme);
                    }

                    return badge;
                }))
                .setHeader("Statut")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Places
        grid.addColumn(new ComponentRenderer<>(event -> {
                    int disponibles = event.getPlacesDisponibles();
                    int total = event.getCapaciteMax();

                    Span places = new Span(disponibles + "/" + total);

                    double taux = (double) (total - disponibles) / total * 100;
                    if (taux >= 90) {
                        places.getStyle().set("color", "#F44336"); // Rouge
                    } else if (taux >= 70) {
                        places.getStyle().set("color", "#FF9800"); // Orange
                    } else {
                        places.getStyle().set("color", "#4CAF50"); // Vert
                    }

                    return places;
                }))
                .setHeader("Places")
                .setSortable(true)
                .setAutoWidth(true);

        // Colonne Actions
        grid.addColumn(new ComponentRenderer<>(this::createActionsLayout))
                .setHeader("Actions")
                .setAutoWidth(true)
                .setFlexGrow(0);

        add(grid);
    }

    private Component createActionsLayout(Event event) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        // Bouton Voir
        Button viewButton = new Button(VaadinIcon.EYE.create());
        viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        viewButton.getElement().setAttribute("title", "Voir les détails");
        viewButton.addClickListener(e -> showEventDetails(event));

        // Bouton Modifier - ✅ OUVERTURE D'UNE DIALOG
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        editButton.getElement().setAttribute("title", "Modifier");
        editButton.setEnabled(event.isModifiable());
        editButton.addClickListener(e -> openEditDialog(event));

        // Bouton Publier (si brouillon)
        if (event.getStatut() == EventStatut.BROUILLON) {
            Button publishButton = new Button(VaadinIcon.UPLOAD.create());
            publishButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
            publishButton.getElement().setAttribute("title", "Publier");
            publishButton.addClickListener(e -> publishEvent(event));
            actions.add(publishButton);
        }

        // Bouton Annuler
        if (event.getStatut() != EventStatut.ANNULE && event.getStatut() != EventStatut.TERMINE) {
            Button cancelButton = new Button(VaadinIcon.BAN.create());
            cancelButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            cancelButton.getElement().setAttribute("title", "Annuler");
            cancelButton.addClickListener(e -> cancelEvent(event));
            actions.add(cancelButton);
        }

        // Bouton Supprimer
        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        deleteButton.getElement().setAttribute("title", "Supprimer");
        deleteButton.setEnabled(event.getReservations().isEmpty());
        deleteButton.addClickListener(e -> deleteEvent(event));

        actions.add(viewButton, editButton, deleteButton);
        return actions;
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Dialog de modification d'événement
     */
    private void openEditDialog(Event event) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("✏️ Modifier l'événement");
        dialog.setWidth("800px");
        dialog.setHeight("90vh");

        // Formulaire
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        // Binder pour la validation
        Binder<Event> binder = new BeanValidationBinder<>(Event.class);

        // Champs
        TextField titreField = new TextField("Titre");
        titreField.setWidthFull();
        titreField.setRequired(true);
        binder.forField(titreField)
                .asRequired("Le titre est obligatoire")
                .bind(Event::getTitre, Event::setTitre);

        TextArea descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();
        descriptionField.setMaxLength(1000);
        descriptionField.setHelperText("Maximum 1000 caractères");
        binder.bind(descriptionField, Event::getDescription, Event::setDescription);

        ComboBox<EventCategorie> categorieField = new ComboBox<>("Catégorie");
        categorieField.setItems(EventCategorie.values());
        categorieField.setItemLabelGenerator(EventCategorie::getLabel);
        categorieField.setRequired(true);
        binder.forField(categorieField)
                .asRequired("La catégorie est obligatoire")
                .bind(Event::getCategorie, Event::setCategorie);

        DateTimePicker dateDebutField = new DateTimePicker("Date de début");
        dateDebutField.setRequiredIndicatorVisible(true);
        dateDebutField.setWidthFull();
        binder.forField(dateDebutField)
                .asRequired("La date de début est obligatoire")
                .bind(Event::getDateDebut, Event::setDateDebut);

        DateTimePicker dateFinField = new DateTimePicker("Date de fin");
        dateFinField.setRequiredIndicatorVisible(true);
        dateFinField.setWidthFull();
        binder.forField(dateFinField)
                .asRequired("La date de fin est obligatoire")
                .bind(Event::getDateFin, Event::setDateFin);

        TextField lieuField = new TextField("Lieu");
        lieuField.setWidthFull();
        lieuField.setRequired(true);
        binder.forField(lieuField)
                .asRequired("Le lieu est obligatoire")
                .bind(Event::getLieu, Event::setLieu);

        TextField villeField = new TextField("Ville");
        villeField.setWidthFull();
        villeField.setRequired(true);
        binder.forField(villeField)
                .asRequired("La ville est obligatoire")
                .bind(Event::getVille, Event::setVille);

        NumberField capaciteField = new NumberField("Capacité maximale");
        capaciteField.setWidthFull();
        capaciteField.setMin(1);
        capaciteField.setStep(1);
        capaciteField.setRequired(true);
        binder.forField(capaciteField)
                .asRequired("La capacité est obligatoire")
                .withConverter(Double::intValue, Integer::doubleValue)
                .bind(Event::getCapaciteMax, Event::setCapaciteMax);

        NumberField prixField = new NumberField("Prix unitaire");
        prixField.setWidthFull();
        prixField.setMin(0);
        prixField.setSuffixComponent(new Span("DH"));
        prixField.setRequired(true);
        binder.forField(prixField)
                .asRequired("Le prix est obligatoire")
                .bind(Event::getPrixUnitaire, Event::setPrixUnitaire);

        TextField imageUrlField = new TextField("URL de l'image");
        imageUrlField.setWidthFull();
        imageUrlField.setPlaceholder("https://...");
        binder.bind(imageUrlField, Event::getImageUrl, Event::setImageUrl);

        // Organisation du formulaire
        form.add(titreField, 2);
        form.add(descriptionField, 2);
        form.add(categorieField, 1);
        form.add(imageUrlField, 1);
        form.add(dateDebutField, 1);
        form.add(dateFinField, 1);
        form.add(lieuField, 1);
        form.add(villeField, 1);
        form.add(capaciteField, 1);
        form.add(prixField, 1);

        // Charger les données
        binder.readBean(event);

        // Conteneur scrollable
        VerticalLayout content = new VerticalLayout(form);
        content.setPadding(false);
        content.setSpacing(true);
        content.getStyle()
                .set("overflow-y", "auto")
                .set("max-height", "calc(90vh - 200px)");

        dialog.add(content);

        // Boutons
        Button cancelButton = new Button("Annuler", e -> dialog.close());

        Button saveButton = new Button("Enregistrer", e -> {
            try {
                if (binder.validate().isOk()) {
                    binder.writeBean(event);

                    // Validation dates
                    if (event.getDateFin().isBefore(event.getDateDebut())) {
                        showErrorNotification("La date de fin doit être après la date de début");
                        return;
                    }

                    // Sauvegarder
                    eventService.updateEvent(event);

                    showSuccessNotification("Événement modifié avec succès");
                    loadEvents();
                    dialog.close();
                } else {
                    showErrorNotification("Veuillez corriger les erreurs dans le formulaire");
                }
            } catch (Exception ex) {
                showErrorNotification("Erreur: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    /**
     * Afficher les détails d'un événement
     */
    private void showEventDetails(Event event) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("📋 Détails de l'événement");
        dialog.setWidth("600px");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);

        // Informations principales
        content.add(createDetailSection("Informations générales",
                "Titre: " + event.getTitre(),
                "Catégorie: " + event.getCategorie().getLabel(),
                "Statut: " + event.getStatut().getLabel(),
                "Organisateur: " + event.getOrganisateur().getNomComplet()
        ));

        // Dates et lieu
        content.add(createDetailSection("Dates et lieu",
                "Début: " + event.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                "Fin: " + event.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                "Lieu: " + event.getLieu(),
                "Ville: " + event.getVille()
        ));

        // Capacité et prix
        content.add(createDetailSection("Capacité et tarif",
                "Capacité totale: " + event.getCapaciteMax() + " places",
                "Places disponibles: " + event.getPlacesDisponibles() + " places",
                "Taux de remplissage: " + String.format("%.1f%%", event.getTauxRemplissage()),
                "Prix unitaire: " + String.format("%.2f DH", event.getPrixUnitaire()),
                "Revenu total: " + String.format("%.2f DH", event.getRevenuTotal())
        ));

        // Description
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            content.add(createDetailSection("Description", event.getDescription()));
        }

        dialog.add(content);

        Button closeButton = new Button("Fermer", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeButton);

        dialog.open();
    }

    private Div createDetailSection(String title, String... lines) {
        Div section = new Div();
        section.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "15px")
                .set("border-radius", "8px")
                .set("margin-bottom", "10px");

        H3 sectionTitle = new H3(title);
        sectionTitle.getStyle()
                .set("margin", "0 0 10px 0")
                .set("font-size", "16px")
                .set("color", "var(--lumo-primary-color)");

        section.add(sectionTitle);

        for (String line : lines) {
            Span lineSpan = new Span(line);
            lineSpan.getStyle()
                    .set("display", "block")
                    .set("margin-bottom", "5px")
                    .set("color", "var(--lumo-primary-text-color)");
            section.add(lineSpan);
        }

        return section;
    }

    private void loadEvents() {
        try {
            List<Event> events = eventService.searchEvents(
                    currentSearch,
                    currentVilleFilter,
                    currentDateDebutFilter,
                    currentDateFinFilter,
                    currentPrixMinFilter,
                    currentPrixMaxFilter,
                    currentStatusFilter
            );

            grid.setItems(events);
        } catch (Exception e) {
            showErrorNotification("Erreur lors du chargement: " + e.getMessage());
        }
    }

    private void resetFilters() {
        searchField.clear();
        statusFilter.clear();
        villeFilter.clear();
        dateDebutFilter.clear();
        dateFinFilter.clear();
        prixMinFilter.clear();
        prixMaxFilter.clear();

        currentSearch = "";
        currentStatusFilter = null;
        currentVilleFilter = "";
        currentDateDebutFilter = null;
        currentDateFinFilter = null;
        currentPrixMinFilter = null;
        currentPrixMaxFilter = null;

        loadEvents();
    }

    private void publishEvent(Event event) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Publier l'événement");
        dialog.setText("Voulez-vous vraiment publier cet événement ? Il sera visible par tous les utilisateurs.");

        dialog.setCancelable(true);
        dialog.setCancelText("Annuler");

        dialog.setConfirmText("Publier");
        dialog.setConfirmButtonTheme("success primary");

        dialog.addConfirmListener(e -> {
            try {
                eventService.publishEvent(event);
                showSuccessNotification("Événement publié avec succès");
                loadEvents();
            } catch (Exception ex) {
                showErrorNotification("Erreur: " + ex.getMessage());
            }
        });

        dialog.open();
    }

    private void cancelEvent(Event event) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Annuler l'événement");
        dialog.setText("Voulez-vous vraiment annuler cet événement ? Toutes les réservations seront également annulées.");

        dialog.setCancelable(true);
        dialog.setCancelText("Non");

        dialog.setConfirmText("Oui, annuler");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                eventService.cancelEvent(event);
                showSuccessNotification("Événement annulé avec succès");
                loadEvents();
            } catch (Exception ex) {
                showErrorNotification("Erreur: " + ex.getMessage());
            }
        });

        dialog.open();
    }

    private void deleteEvent(Event event) {
        if (!event.getReservations().isEmpty()) {
            showErrorNotification("Impossible de supprimer un événement avec des réservations");
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Supprimer l'événement");
        dialog.setText("Voulez-vous vraiment supprimer définitivement cet événement ? Cette action est irréversible.");

        dialog.setCancelable(true);
        dialog.setCancelText("Annuler");

        dialog.setConfirmText("Supprimer");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                eventService.deleteEvent(event);
                showSuccessNotification("Événement supprimé avec succès");
                loadEvents();
            } catch (Exception ex) {
                showErrorNotification("Erreur: " + ex.getMessage());
            }
        });

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