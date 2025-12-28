package org.example.reservation_event.ServicesImplimentation;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.classes.Reservation;
import org.example.reservation_event.classes.User;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Service pour l'export PDF des réservations d'événements
 * Conforme au cahier des charges - Section 9: Fonctionnalités Bonus (Export de données)
 */
@Service
public class PdfReservationExportService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("fr", "FR"));

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm", new Locale("fr", "FR"));

    // Couleurs
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(79, 70, 229); // #4f46e5
    private static final DeviceRgb SUCCESS_COLOR = new DeviceRgb(5, 150, 105); // #059669
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(100, 116, 139); // #64748b

    /**
     * Génère un PDF contenant toutes les réservations d'un événement
     *
     * @param event L'événement concerné
     * @param reservations Liste des réservations à inclure dans le PDF
     * @param totalPlaces Nombre total de places réservées
     * @param totalRevenue Revenu total généré
     * @return Tableau d'octets contenant le PDF généré
     * @throws IOException En cas d'erreur lors de la génération
     */
    public byte[] generateReservationsPdf(
            Event event,
            List<Reservation> reservations,
            int totalPlaces,
            double totalRevenue
    ) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        try {
            // Fonts
            PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // ===== HEADER =====
            addHeader(document, titleFont, normalFont);

            // ===== EVENT INFO =====
            addEventInfo(document, event, titleFont, normalFont);

            // ===== STATISTICS SUMMARY =====
            addStatisticsSummary(document, reservations, totalPlaces, totalRevenue, normalFont);

            // ===== RESERVATIONS TABLE =====
            addReservationsTable(document, reservations, titleFont, normalFont);

            // ===== FOOTER =====
            addFooter(document, reservations.size(), totalPlaces, totalRevenue, normalFont);

        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    /**
     * Génère le nom de fichier pour l'export PDF
     */
    public String generateFileName(Event event) {
        String sanitizedTitle = event.getTitre().replaceAll("[^a-zA-Z0-9]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "reservations_" + sanitizedTitle + "_" + timestamp + ".pdf";
    }

    // ========================================
    // MÉTHODES PRIVÉES - CONSTRUCTION DU PDF
    // ========================================

    private void addHeader(Document document, PdfFont titleFont, PdfFont normalFont) throws IOException {
        // Logo/Brand
        Paragraph brand = new Paragraph("EventBooking")
                .setFont(titleFont)
                .setFontSize(16)
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(brand);

        // Sous-titre
        Paragraph subtitle = new Paragraph("Système de Gestion de Réservations d'Événements")
                .setFont(normalFont)
                .setFontSize(9)
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(subtitle);

        // Ligne de séparation
        document.add(createSeparatorLine());
    }

    private void addEventInfo(Document document, Event event, PdfFont titleFont, PdfFont normalFont) throws IOException {
        // Titre du document
        Paragraph title = new Paragraph("RAPPORT DE RÉSERVATIONS")
                .setFont(titleFont)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(title);

        // Nom de l'événement
        Paragraph eventTitle = new Paragraph(event.getTitre())
                .setFont(titleFont)
                .setFontSize(16)
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(eventTitle);

        // Détails de l'événement
        Paragraph eventDetails = new Paragraph(
                String.format("%s • %s • %s, %s",
                        event.getCategorie().getLabel(),
                        formatDate(event.getDateDebut()),
                        event.getLieu(),
                        event.getVille()
                )
        )
                .setFont(normalFont)
                .setFontSize(10)
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(eventDetails);

        // Organisateur
        if (event.getOrganisateur() != null) {
            User organizer = event.getOrganisateur();
            Paragraph organizerInfo = new Paragraph(
                    "Organisateur: " + organizer.getPrenom() + " " + organizer.getNom()
            )
                    .setFont(normalFont)
                    .setFontSize(9)
                    .setFontColor(SECONDARY_COLOR)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(organizerInfo);
        }
    }

    private void addStatisticsSummary(
            Document document,
            List<Reservation> reservations,
            int totalPlaces,
            double totalRevenue,
            PdfFont normalFont
    ) throws IOException {

        long confirmedCount = reservations.stream()
                .filter(r -> r.getStatut().name().equals("CONFIRMEE"))
                .count();

        long pendingCount = reservations.stream()
                .filter(r -> r.getStatut().name().equals("EN_ATTENTE"))
                .count();

        long canceledCount = reservations.stream()
                .filter(r -> r.getStatut().name().equals("ANNULEE"))
                .count();

        // Tableau de statistiques
        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}));
        statsTable.setWidth(UnitValue.createPercentValue(100));
        statsTable.setMarginBottom(20);

        addStatCell(statsTable, "Total Réservations", String.valueOf(reservations.size()), PRIMARY_COLOR);
        addStatCell(statsTable, "Places Réservées", String.valueOf(totalPlaces), SUCCESS_COLOR);
        addStatCell(statsTable, "Revenus", String.format("%.2f DH", totalRevenue), new DeviceRgb(245, 158, 11));
        addStatCell(statsTable, "Confirmées", String.valueOf(confirmedCount), new DeviceRgb(139, 92, 246));

        document.add(statsTable);
    }

    private void addStatCell(Table table, String label, String value, DeviceRgb color) {
        Cell cell = new Cell()
                .setBackgroundColor(new DeviceRgb(248, 250, 252))
                .setPadding(10)
                .setTextAlignment(TextAlignment.CENTER);

        Paragraph labelPara = new Paragraph(label)
                .setFontSize(9)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(5);

        Paragraph valuePara = new Paragraph(value)
                .setFontSize(14)
                .setBold()
                .setFontColor(color);

        cell.add(labelPara);
        cell.add(valuePara);
        table.addCell(cell);
    }

    private void addReservationsTable(
            Document document,
            List<Reservation> reservations,
            PdfFont titleFont,
            PdfFont normalFont
    ) throws IOException {

        // Titre de section
        Paragraph tableTitle = new Paragraph("Détails des Réservations")
                .setFont(titleFont)
                .setFontSize(14)
                .setMarginBottom(10);
        document.add(tableTitle);

        // Tableau
        Table table = new Table(UnitValue.createPercentArray(new float[]{2f, 3f, 4f, 1.5f, 2f, 3f, 2f}));
        table.setWidth(UnitValue.createPercentValue(100));

        // En-têtes
        String[] headers = {"Code", "Client", "Email", "Places", "Montant", "Date", "Statut"};

        for (String header : headers) {
            Cell cell = new Cell()
                    .add(new Paragraph(header)
                            .setFont(titleFont)
                            .setFontSize(10)
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(PRIMARY_COLOR)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8);
            table.addHeaderCell(cell);
        }

        // Données
        for (Reservation r : reservations) {
            User user = r.getUtilisateur();
            String clientName = user != null ? user.getPrenom() + " " + user.getNom() : "N/A";
            String email = user != null ? user.getEmail() : "N/A";

            addDataCell(table, r.getCodeReservation(), normalFont, false);
            addDataCell(table, clientName, normalFont, false);
            addDataCell(table, email, normalFont, false);
            addDataCell(table, String.valueOf(r.getNombrePlaces()), normalFont, true);
            addDataCell(table, String.format("%.2f DH", r.getMontantTotal()), normalFont, false);
            addDataCell(table, formatDateTime(r.getDateReservation()), normalFont, false);
            addStatusCell(table, r.getStatut().name());
        }

        document.add(table);
    }

    private void addDataCell(Table table, String content, PdfFont font, boolean center) {
        Cell cell = new Cell()
                .add(new Paragraph(content)
                        .setFont(font)
                        .setFontSize(9))
                .setPadding(5)
                .setTextAlignment(center ? TextAlignment.CENTER : TextAlignment.LEFT);
        table.addCell(cell);
    }

    private void addStatusCell(Table table, String status) {
        DeviceRgb bgColor;
        switch (status) {
            case "CONFIRMEE":
                bgColor = new DeviceRgb(5, 150, 105);
                break;
            case "EN_ATTENTE":
                bgColor = new DeviceRgb(245, 158, 11);
                break;
            case "ANNULEE":
                bgColor = new DeviceRgb(239, 68, 68);
                break;
            default:
                bgColor = SECONDARY_COLOR;
        }

        Cell cell = new Cell()
                .add(new Paragraph(status)
                        .setFontSize(9)
                        .setBold()
                        .setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(bgColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5);
        table.addCell(cell);
    }

    private void addFooter(
            Document document,
            int totalReservations,
            int totalPlaces,
            double totalRevenue,
            PdfFont normalFont
    ) throws IOException {

        document.add(new Paragraph("\n"));
        document.add(createSeparatorLine());

        // Résumé final
        Paragraph summary = new Paragraph(
                String.format("📊 Résumé: %d réservation(s) | %d place(s) | %.2f DH de revenus",
                        totalReservations, totalPlaces, totalRevenue)
        )
                .setFont(normalFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(10);
        document.add(summary);

        // Date de génération
        Paragraph generated = new Paragraph(
                "Document généré le " + LocalDateTime.now().format(DATETIME_FORMATTER)
        )
                .setFont(normalFont)
                .setFontSize(8)
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(generated);

        // Copyright
        Paragraph copyright = new Paragraph("© 2025 EventBooking - Tous droits réservés")
                .setFont(normalFont)
                .setFontSize(8)
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(copyright);
    }

    private Paragraph createSeparatorLine() {
        Paragraph line = new Paragraph("_".repeat(100))
                .setFontColor(new DeviceRgb(226, 232, 240))
                .setFontSize(8)
                .setMarginBottom(10);
        return line;
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DATE_FORMATTER);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DATETIME_FORMATTER);
    }
}