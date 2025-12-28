package org.example.reservation_event.ServicesImplimentation;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.example.reservation_event.classes.Reservation;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfTicketService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH'h'mm", new Locale("fr", "FR"));

    private static final DateTimeFormatter SHORT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Génère un billet PDF pour une réservation
     */
    public byte[] generateTicketPdf(Reservation reservation) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        try {
            // Fonts
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Colors
            DeviceRgb primaryColor = new DeviceRgb(79, 70, 229); // #4f46e5
            DeviceRgb darkColor = new DeviceRgb(30, 41, 59); // #1e293b
            DeviceRgb grayColor = new DeviceRgb(100, 116, 139); // #64748b

            // ===== HEADER SECTION =====
            addHeader(document, boldFont, primaryColor);

            document.add(new Paragraph("\n"));

            // ===== TITLE =====
            Paragraph title = new Paragraph("BILLET D'ÉVÉNEMENT")
                    .setFont(boldFont)
                    .setFontSize(24)
                    .setFontColor(darkColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            document.add(title);

            // ===== RESERVATION CODE (BIG) =====
            Paragraph code = new Paragraph(reservation.getCodeReservation())
                    .setFont(boldFont)
                    .setFontSize(32)
                    .setFontColor(primaryColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(code);

            // ===== EVENT INFO BOX =====
            Table eventTable = new Table(UnitValue.createPercentArray(new float[]{1}));
            eventTable.setWidth(UnitValue.createPercentValue(100));
            eventTable.setBorder(new SolidBorder(primaryColor, 2f));
            eventTable.setMarginBottom(20);

            // Event Title
            Cell titleCell = new Cell()
                    .add(new Paragraph(reservation.getEvenement().getTitre())
                            .setFont(boldFont)
                            .setFontSize(18)
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(primaryColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(10)
                    .setBorder(Border.NO_BORDER);
            eventTable.addCell(titleCell);

            // Event Details
            Cell detailsCell = new Cell()
                    .setPadding(15)
                    .setBorder(Border.NO_BORDER);

            detailsCell.add(createDetailRow("📅 Date",
                    DATE_FORMATTER.format(reservation.getEvenement().getDateDebut()),
                    boldFont, regularFont, darkColor, grayColor));

            detailsCell.add(createDetailRow("📍 Lieu",
                    reservation.getEvenement().getLieu() + ", " + reservation.getEvenement().getVille(),
                    boldFont, regularFont, darkColor, grayColor));

            detailsCell.add(createDetailRow("🎫 Catégorie",
                    reservation.getEvenement().getCategorie().getLabel(),
                    boldFont, regularFont, darkColor, grayColor));

            eventTable.addCell(detailsCell);
            document.add(eventTable);

            // ===== RESERVATION DETAILS =====
            Table reservationTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
            reservationTable.setWidth(UnitValue.createPercentValue(100));
            reservationTable.setMarginBottom(20);

            // Left column
            Cell leftCell = new Cell()
                    .setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 1f))
                    .setPadding(15)
                    .setBackgroundColor(new DeviceRgb(248, 250, 252));

            leftCell.add(createInfoBlock("Titulaire",
                    reservation.getUtilisateur().getPrenom() + " " + reservation.getUtilisateur().getNom(),
                    boldFont, regularFont, darkColor, grayColor));

            leftCell.add(createInfoBlock("Email",
                    reservation.getUtilisateur().getEmail(),
                    boldFont, regularFont, darkColor, grayColor));

            if (reservation.getUtilisateur().getTelephone() != null) {
                leftCell.add(createInfoBlock("Téléphone",
                        reservation.getUtilisateur().getTelephone(),
                        boldFont, regularFont, darkColor, grayColor));
            }

            reservationTable.addCell(leftCell);

            // Right column
            Cell rightCell = new Cell()
                    .setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 1f))
                    .setPadding(15)
                    .setBackgroundColor(new DeviceRgb(248, 250, 252));

            rightCell.add(createInfoBlock("Nombre de places",
                    String.valueOf(reservation.getNombrePlaces()),
                    boldFont, regularFont, darkColor, grayColor));

            rightCell.add(createInfoBlock("Prix unitaire",
                    String.format("%.2f DH", reservation.getEvenement().getPrixUnitaire()),
                    boldFont, regularFont, darkColor, grayColor));

            rightCell.add(createInfoBlock("Montant total",
                    String.format("%.2f DH", reservation.getMontantTotal()),
                    boldFont, regularFont, darkColor, new DeviceRgb(5, 150, 105)));

            rightCell.add(createInfoBlock("Date de réservation",
                    SHORT_DATE_FORMATTER.format(reservation.getDateReservation()),
                    boldFont, regularFont, darkColor, grayColor));

            reservationTable.addCell(rightCell);
            document.add(reservationTable);

            // ===== STATUS BADGE =====
            Paragraph status = new Paragraph("✓ " + reservation.getStatut().getLabel())
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setFontColor(ColorConstants.WHITE)
                    .setBackgroundColor(getStatusColor(reservation.getStatut().name()))
                    .setPadding(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(status);

            // ===== ORGANIZER INFO =====
            if (reservation.getEvenement().getOrganisateur() != null) {
                Paragraph organizerTitle = new Paragraph("Organisateur")
                        .setFont(boldFont)
                        .setFontSize(12)
                        .setFontColor(grayColor)
                        .setMarginBottom(5);
                document.add(organizerTitle);

                Paragraph organizer = new Paragraph(
                        reservation.getEvenement().getOrganisateur().getPrenom() + " " +
                                reservation.getEvenement().getOrganisateur().getNom())
                        .setFont(regularFont)
                        .setFontSize(11)
                        .setFontColor(darkColor)
                        .setMarginBottom(15);
                document.add(organizer);
            }

            // ===== COMMENT (if exists) =====
            if (reservation.getCommentaire() != null && !reservation.getCommentaire().isEmpty()) {
                Paragraph commentTitle = new Paragraph("Commentaire")
                        .setFont(boldFont)
                        .setFontSize(12)
                        .setFontColor(grayColor)
                        .setMarginBottom(5);
                document.add(commentTitle);

                Paragraph comment = new Paragraph(reservation.getCommentaire())
                        .setFont(regularFont)
                        .setFontSize(10)
                        .setFontColor(darkColor)
                        .setItalic()
                        .setMarginBottom(15);
                document.add(comment);
            }

            // ===== FOOTER =====
            addFooter(document, regularFont, grayColor);

        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private void addHeader(Document document, PdfFont font, DeviceRgb color) {
        Paragraph header = new Paragraph("EventBooking")
                .setFont(font)
                .setFontSize(20)
                .setFontColor(color)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(header);

        Paragraph subtitle = new Paragraph("Système de Gestion de Réservations d'Événements")
                .setFontSize(10)
                .setFontColor(new DeviceRgb(100, 116, 139))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(subtitle);

        // Horizontal line - CORRECTION ICI
        SolidLine solidLine = new SolidLine(2f);
        solidLine.setColor(color);
        LineSeparator line = new LineSeparator(solidLine);
        document.add(line);
    }

    private void addFooter(Document document, PdfFont font, DeviceRgb color) {
        // Horizontal line - CORRECTION ICI
        SolidLine solidLine = new SolidLine(1f);
        solidLine.setColor(new DeviceRgb(226, 232, 240));
        LineSeparator line = new LineSeparator(solidLine);
        document.add(line);

        Paragraph footer = new Paragraph(
                "⚠️ Conditions importantes\n" +
                        "• Ce billet est personnel et non transférable\n" +
                        "• Présentez ce billet (imprimé ou numérique) à l'entrée de l'événement\n" +
                        "• Les réservations peuvent être annulées jusqu'à 48h avant l'événement\n" +
                        "• Conservez précieusement votre code de réservation\n" +
                        "• En cas de problème, contactez l'organisateur\n\n" +
                        "Généré le " + java.time.LocalDateTime.now().format(SHORT_DATE_FORMATTER))
                .setFont(font)
                .setFontSize(8)
                .setFontColor(color)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginTop(10);
        document.add(footer);

        Paragraph copyright = new Paragraph("© 2025 EventBooking - Tous droits réservés")
                .setFont(font)
                .setFontSize(8)
                .setFontColor(color)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(5);
        document.add(copyright);
    }

    private Paragraph createDetailRow(String label, String value,
                                      PdfFont boldFont, PdfFont regularFont,
                                      DeviceRgb darkColor, DeviceRgb grayColor) {
        Paragraph p = new Paragraph();
        p.add(new Text(label + ": ")
                .setFont(boldFont)
                .setFontSize(11)
                .setFontColor(darkColor));
        p.add(new Text(value)
                .setFont(regularFont)
                .setFontSize(11)
                .setFontColor(grayColor));
        p.setMarginBottom(8);
        return p;
    }

    private Div createInfoBlock(String label, String value,
                                PdfFont boldFont, PdfFont regularFont,
                                DeviceRgb darkColor, DeviceRgb valueColor) {
        Div div = new Div();

        Paragraph labelP = new Paragraph(label)
                .setFont(boldFont)
                .setFontSize(10)
                .setFontColor(darkColor)
                .setMarginBottom(3);
        div.add(labelP);

        Paragraph valueP = new Paragraph(value)
                .setFont(regularFont)
                .setFontSize(12)
                .setFontColor(valueColor)
                .setMarginBottom(10);
        div.add(valueP);

        return div;
    }

    private DeviceRgb getStatusColor(String status) {
        switch (status) {
            case "CONFIRMEE":
                return new DeviceRgb(5, 150, 105); // Green
            case "EN_ATTENTE":
                return new DeviceRgb(245, 158, 11); // Orange
            case "ANNULEE":
                return new DeviceRgb(220, 38, 38); // Red
            default:
                return new DeviceRgb(107, 114, 128); // Gray
        }
    }
}