package org.example.reservation_event.utils;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;

public class ImageUtils {

    // Chemin relatif au projet au lieu d'un chemin absolu fixe
    private static final String UPLOAD_RELATIVE_PATH = "src/main/java/org/example/reservation_event/uploads/";

    public static Image createEventImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return createPlaceholderImage();
        }

        try {
            // Extraire le nom de fichier de l'URL
            String fileName = extractFileName(imageUrl);

            // Obtenir le chemin absolu basé sur le répertoire du projet
            String projectDir = System.getProperty("user.dir");
            String uploadDir = Paths.get(projectDir, UPLOAD_RELATIVE_PATH).toString();

            File imageFile = new File(uploadDir, fileName);

            // Debug: afficher les chemins pour vérification
            System.out.println("Project directory: " + projectDir);
            System.out.println("Upload directory: " + uploadDir);
            System.out.println("Image file path: " + imageFile.getAbsolutePath());
            System.out.println("File exists: " + imageFile.exists());
            System.out.println("Can read: " + imageFile.canRead());

            if (imageFile.exists() && imageFile.canRead()) {
                StreamResource resource = new StreamResource(
                        fileName,
                        () -> {
                            try {
                                return new FileInputStream(imageFile);
                            } catch (FileNotFoundException e) {
                                System.err.println("File not found during stream: " + imageFile.getAbsolutePath());
                                return new ByteArrayInputStream(new byte[0]);
                            }
                        }
                );

                Image image = new Image(resource, "Event Image");
                image.setWidthFull();
                image.setHeight("200px");
                image.getStyle()
                        .set("object-fit", "cover")
                        .set("border-radius", "8px")
                        .set("background-color", "#f8fafc");
                return image;
            } else {
                System.err.println("Image file not found or not readable: " + imageFile.getAbsolutePath());
                return createErrorImage("Image non disponible");
            }
        } catch (Exception e) {
            System.err.println("Error creating image: " + e.getMessage());
            e.printStackTrace();
            return createErrorImage("Erreur de chargement");
        }
    }

    // Méthode avec hauteur personnalisable
    public static Image createEventImage(String imageUrl, String height) {
        Image image = createEventImage(imageUrl);
        if (height != null && !height.isEmpty()) {
            image.setHeight(height);
        }
        return image;
    }

    // Méthode avec hauteur et largeur personnalisables
    public static Image createEventImage(String imageUrl, String width, String height) {
        Image image = createEventImage(imageUrl);
        if (width != null && !width.isEmpty()) {
            image.setWidth(width);
        }
        if (height != null && !height.isEmpty()) {
            image.setHeight(height);
        }
        return image;
    }

    private static String extractFileName(String imageUrl) {
        if (imageUrl == null) return "";

        // Supprimer les paramètres de requête s'il y en a
        if (imageUrl.contains("?")) {
            imageUrl = imageUrl.substring(0, imageUrl.indexOf("?"));
        }

        // Extraire le nom de fichier
        if (imageUrl.contains("/")) {
            return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        }
        if (imageUrl.contains("\\")) {
            return imageUrl.substring(imageUrl.lastIndexOf("\\") + 1);
        }
        return imageUrl;
    }

    private static Image createPlaceholderImage() {
        String svg = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='200' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='1'%3E%3Crect x='3' y='3' width='18' height='18' rx='2' ry='2'%3E%3C/rect%3E%3Ccircle cx='8.5' cy='8.5' r='1.5'%3E%3C/circle%3E%3Cpolyline points='21 15 16 10 5 21'%3E%3C/polyline%3E%3C/svg%3E";
        Image placeholder = new Image(svg, "Aucune image");
        placeholder.setWidthFull();
        placeholder.setHeight("200px");
        placeholder.getStyle()
                .set("object-fit", "contain")
                .set("background-color", "#f8fafc")
                .set("border", "2px dashed #e2e8f0")
                .set("border-radius", "8px")
                .set("padding", "20px");
        return placeholder;
    }

    private static Image createErrorImage(String errorText) {
        String escapedText = errorText.replace(" ", "%20").replace("'", "%27");
        String svg = String.format(
                "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='200' viewBox='0 0 200 200'%3E" +
                        "%3Crect width='200' height='200' fill='%23fef2f2' rx='8'/%3E" +
                        "%3Ccircle cx='100' cy='80' r='40' fill='%23fecaca'/%3E" +
                        "%3Cline x1='100' y1='60' x2='100' y2='80' stroke='%23dc2626' stroke-width='3'/%3E" +
                        "%3Ccircle cx='100' cy='90' r='3' fill='%23dc2626'/%3E" +
                        "%3Ctext x='100' y='140' text-anchor='middle' font-family='Arial' font-size='12' fill='%23991b1b'%3E%s%3C/text%3E" +
                        "%3C/svg%3E",
                escapedText
        );
        Image errorImage = new Image(svg, errorText);
        errorImage.setWidthFull();
        errorImage.setHeight("200px");
        errorImage.getStyle()
                .set("object-fit", "contain")
                .set("border-radius", "8px");
        return errorImage;
    }

    // Méthode utilitaire pour vérifier si un fichier image existe
    public static boolean eventImageExists(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return false;
        }

        try {
            String fileName = extractFileName(imageUrl);
            String projectDir = System.getProperty("user.dir");
            String uploadDir = Paths.get(projectDir, UPLOAD_RELATIVE_PATH).toString();
            File imageFile = new File(uploadDir, fileName);

            return imageFile.exists() && imageFile.canRead();
        } catch (Exception e) {
            System.err.println("Error checking image existence: " + e.getMessage());
            return false;
        }
    }
}