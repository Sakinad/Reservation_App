package org.example.reservation_event.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @PostConstruct
    public void init() {
        System.out.println("=== StaticResourceConfig Initialized ===");

        String basePath = "C:/Master/java_python/Reservation_event_sakina/uploads/events/";
        File dir = new File(basePath);

        System.out.println("Checking upload directory:");
        System.out.println("Path: " + dir.getAbsolutePath());
        System.out.println("Exists: " + dir.exists());
        System.out.println("Is directory: " + dir.isDirectory());
        System.out.println("Can read: " + dir.canRead());

        if (dir.exists()) {
            String[] files = dir.list();
            System.out.println("Files in directory (" + (files != null ? files.length : 0) + "):");
            if (files != null) {
                for (String file : files) {
                    System.out.println("  - " + file);
                }
            }
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String basePath = "C:/Master/java_python/Reservation_event_sakina/uploads/events/";

        System.out.println("Registering resource handler:");
        System.out.println("Pattern: /images/events/**");
        System.out.println("Location: file:" + basePath);

        registry.addResourceHandler("/images/events/**")
                .addResourceLocations("file:" + basePath)
                .setCachePeriod(3600);

        // Also try with forward slashes
        registry.addResourceHandler("/uploads/events/**")
                .addResourceLocations("file:" + basePath.replace("\\", "/"));
    }
}
