package org.example.reservation_event.ui.Admin;

import org.example.reservation_event.Enums.EventStatut;
import org.example.reservation_event.classes.Event;
import org.example.reservation_event.repositories.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class EventDebugController {

    @Autowired
    private EventRepository eventRepository;

    /**
     * Check all events and their status
     */
    @GetMapping("/admin/debug/events")
    public String debugEvents() {
        LocalDateTime now = LocalDateTime.now();
        StringBuilder html = new StringBuilder();

        html.append("<html><head><title>Event Debug</title></head><body>");
        html.append("<h1>Event Status Debug</h1>");
        html.append("<p>Current time: ").append(now).append("</p>");

        // Get all events
        List<Event> allEvents = eventRepository.findAll();
        html.append("<p>Total events: ").append(allEvents.size()).append("</p>");

        html.append("<h2>All Events:</h2>");
        html.append("<table border='1' cellpadding='5'>");
        html.append("<tr><th>ID</th><th>Title</th><th>Status</th><th>Start</th><th>End</th><th>Should be TERMINE?</th></tr>");

        for (Event event : allEvents) {
            boolean shouldBeTermine = event.getStatut() == EventStatut.PUBLIE
                    && event.getDateFin() != null
                    && event.getDateFin().isBefore(now);

            String rowColor = shouldBeTermine ? "style='background-color: #ffcccc'" : "";

            html.append("<tr ").append(rowColor).append(">");
            html.append("<td>").append(event.getId()).append("</td>");
            html.append("<td><strong>").append(event.getTitre()).append("</strong></td>");
            html.append("<td>").append(event.getStatut()).append("</td>");
            html.append("<td>").append(event.getDateDebut()).append("</td>");
            html.append("<td>").append(event.getDateFin()).append("</td>");
            html.append("<td>").append(shouldBeTermine ? "YES ⚠️" : "No").append("</td>");
            html.append("</tr>");
        }

        html.append("</table>");

        // Summary
        long publiéCount = allEvents.stream().filter(e -> e.getStatut() == EventStatut.PUBLIE).count();
        long termineCount = allEvents.stream().filter(e -> e.getStatut() == EventStatut.TERMINE).count();
        long annuleCount = allEvents.stream().filter(e -> e.getStatut() == EventStatut.ANNULE).count();
        long brouillonCount = allEvents.stream().filter(e -> e.getStatut() == EventStatut.BROUILLON).count();

        html.append("<h2>Summary:</h2>");
        html.append("<ul>");
        html.append("<li>PUBLIE: ").append(publiéCount).append("</li>");
        html.append("<li>TERMINE: ").append(termineCount).append("</li>");
        html.append("<li>ANNULE: ").append(annuleCount).append("</li>");
        html.append("<li>BROUILLON: ").append(brouillonCount).append("</li>");
        html.append("</ul>");

        html.append("<br><a href='/api/admin/debug/fix-events'>Fix Events Now</a>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Manually fix all events that should be TERMINE
     */
    @GetMapping("/api/admin/debug/fix-events")
    public String fixEventsNow() {
        LocalDateTime now = LocalDateTime.now();
        StringBuilder result = new StringBuilder();

        result.append("<html><body>");
        result.append("<h1>Manually Fixing Events</h1>");
        result.append("<p>Current time: ").append(now).append("</p>");

        // Get all PUBLIE events
        List<Event> publishedEvents = eventRepository.findByStatut(EventStatut.PUBLIE);
        result.append("<p>Found ").append(publishedEvents.size()).append(" PUBLIE events</p>");

        int fixedCount = 0;
        result.append("<ul>");

        for (Event event : publishedEvents) {
            if (event.getDateFin() != null && event.getDateFin().isBefore(now)) {
                try {
                    event.setStatut(EventStatut.TERMINE);
                    eventRepository.save(event);
                    fixedCount++;
                    result.append("<li style='color: green'>✅ Fixed: ").append(event.getTitre())
                            .append(" (ended ").append(event.getDateFin()).append(")</li>");
                } catch (Exception e) {
                    result.append("<li style='color: red'>❌ Error fixing: ").append(event.getTitre())
                            .append(" - ").append(e.getMessage()).append("</li>");
                }
            }
        }

        result.append("</ul>");
        result.append("<h3>Result: Fixed ").append(fixedCount).append(" event(s)</h3>");
        result.append("<br><a href='/api/admin/debug/events'>Back to Event List</a>");
        result.append("</body></html>");

        return result.toString();
    }

    /**
     * Manually trigger the scheduler
     */
    @GetMapping("/api/admin/debug/trigger-scheduler")
    public String triggerScheduler() {
        return "<html><body>" +
                "<h1>Trigger Event Status Scheduler</h1>" +
                "<p>Scheduler will run manually. Check server logs for results.</p>" +
                "<p><a href='/api/admin/debug/events'>View Events</a></p>" +
                "</body></html>";
    }
}