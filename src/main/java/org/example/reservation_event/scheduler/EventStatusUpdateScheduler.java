package org.example.reservation_event.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class EventStatusUpdateScheduler {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * FIXED: Uses correct column names (uppercase as in database)
     * Runs every 2 minutes
     */
    @Scheduled(cron = "0 */2 * * * ?")
    @Transactional
    public void updateEventStatuses() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("📅 EVENT STATUS UPDATE - EVERY 2 MINUTES");
        System.out.println("=".repeat(50));

        LocalDateTime now = LocalDateTime.now();
        System.out.println("Current time: " + now);

        try {
            // CORRECT: Use uppercase column names as they appear in database
            String updateSql = """
                UPDATE events 
                SET STATUT = 'TERMINE' 
                WHERE STATUT = 'PUBLIE' 
                AND DATE_FIN < ?
                AND STATUT != 'ANNULE'
                """;

            int updated = jdbcTemplate.update(updateSql, now);

            if (updated > 0) {
                System.out.println("✅ Updated " + updated + " event(s) to TERMINE");

                // Show which events were updated (using correct column names)
                String showUpdatedSql = """
                    SELECT ID, TITRE, DATE_FIN 
                    FROM events 
                    WHERE STATUT = 'TERMINE' 
                    AND DATE_MODIFICATION >= ?
                    ORDER BY DATE_FIN DESC
                    """;

                List<Map<String, Object>> updatedEvents = jdbcTemplate.queryForList(
                        showUpdatedSql, now.minusMinutes(3));

                if (!updatedEvents.isEmpty()) {
                    System.out.println("📋 Recently updated:");
                    for (Map<String, Object> event : updatedEvents) {
                        System.out.println("   • " + event.get("TITRE") +
                                " (ID: " + event.get("ID") +
                                ", ended: " + event.get("DATE_FIN") + ")");
                    }
                }
            } else {
                System.out.println("ℹ️ No events needed updating");
            }

            // Show statistics (using correct column names)
            String statsSql = """
                SELECT 
                    COUNT(CASE WHEN STATUT = 'PUBLIE' AND DATE_FIN < ? THEN 1 END) as should_be_termine,
                    COUNT(CASE WHEN STATUT = 'PUBLIE' THEN 1 END) as total_publie,
                    COUNT(CASE WHEN STATUT = 'TERMINE' THEN 1 END) as total_termine
                FROM events
                """;

            Map<String, Object> stats = jdbcTemplate.queryForMap(statsSql, now);

            System.out.println("\n📊 STATISTICS:");
            System.out.println("   PUBLIE events: " + stats.get("total_publie"));
            System.out.println("   TERMINE events: " + stats.get("total_termine"));
            System.out.println("   Should be TERMINE: " + stats.get("should_be_termine"));

            // Show events that still need fixing
            String problemEventsSql = """
                SELECT ID, TITRE, STATUT, DATE_FIN 
                FROM events 
                WHERE STATUT = 'PUBLIE' 
                AND DATE_FIN < ?
                ORDER BY DATE_FIN
                """;

            List<Map<String, Object>> problemEvents = jdbcTemplate.queryForList(
                    problemEventsSql, now);

            if (!problemEvents.isEmpty()) {
                System.out.println("\n⚠️ Events that should be TERMINE but aren't:");
                for (Map<String, Object> event : problemEvents) {
                    System.out.println("   • " + event.get("TITRE") +
                            " (ID: " + event.get("ID") +
                            ", ends: " + event.get("DATE_FIN") +
                            ", status: " + event.get("STATUT") + ")");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=".repeat(50) + "\n");
    }
}