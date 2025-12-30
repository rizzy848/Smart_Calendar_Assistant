package entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the response sent back to the user after processing their request.
 * Contains success/failure status, message, and any retrieved events.
 */
public class EventResponse {

    private boolean success;            // Did the operation succeed?
    private String message;             // "Event created successfully!" or error message
    private ActionType actionPerformed; // What action was done
    private Event createdEvent;         // For CREATE - the event that was created
    private List<Event> events;         // For VIEW - list of events retrieved
    private String errorCode;           // For errors - specific error code

    // ===== CONSTRUCTORS =====

    public EventResponse() {
        this.events = new ArrayList<>();
    }

    /**
     * Quick constructor for success response
     */
    public EventResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.events = new ArrayList<>();
    }

    // ===== FACTORY METHODS (Clean way to create responses) =====

    /**
     * Create a success response for CREATE action
     */
    public static EventResponse createSuccess(Event event) {
        EventResponse response = new EventResponse();
        response.success = true;
        response.actionPerformed = ActionType.CREATE;
        response.createdEvent = event;
        response.message = String.format("✅ Event '%s' created for %s at %s",
                event.getTitle(), event.getDate(), event.getStartTime());
        return response;
    }

    /**
     * Create a success response for VIEW action
     */
    public static EventResponse viewSuccess(List<Event> events, String dateInfo) {
        EventResponse response = new EventResponse();
        response.success = true;
        response.actionPerformed = ActionType.VIEW;
        response.events = events;

        if (events.isEmpty()) {
            response.message = String.format("📅 No events found for %s", dateInfo);
        } else {
            response.message = String.format("📅 Found %d event(s) for %s",
                    events.size(), dateInfo);
        }
        return response;
    }

    /**
     * Create a success response for DELETE action
     */
    public static EventResponse deleteSuccess(Event deletedEvent) {
        EventResponse response = new EventResponse();
        response.success = true;
        response.actionPerformed = ActionType.DELETE;
        response.message = String.format("🗑️ Event '%s' deleted successfully",
                deletedEvent.getTitle());
        return response;
    }

    /**
     * Create a success response for UPDATE action
     */
    public static EventResponse updateSuccess(Event updatedEvent) {
        EventResponse response = new EventResponse();
        response.success = true;
        response.actionPerformed = ActionType.UPDATE;
        response.createdEvent = updatedEvent;
        response.message = String.format("✏️ Event '%s' updated successfully",
                updatedEvent.getTitle());
        return response;
    }

    /**
     * Create an error response
     */
    public static EventResponse error(String errorMessage, String errorCode) {
        EventResponse response = new EventResponse();
        response.success = false;
        response.message = "❌ " + errorMessage;
        response.errorCode = errorCode;
        return response;
    }

    /**
     * Create an error response (simple version)
     */
    public static EventResponse error(String errorMessage) {
        return error(errorMessage, "UNKNOWN_ERROR");
    }

    // ===== BUSINESS LOGIC =====

    /**
     * Get formatted list of events for display
     */
    public String getFormattedEventList() {
        if (events == null || events.isEmpty()) {
            return "No events to display.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");

        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            sb.append(String.format("%d. %s\n", i + 1, e.getTitle()));
            sb.append(String.format("   📅 %s | ⏰ %s - %s\n",
                    e.getDate(), e.getStartTime(), e.getEndTime()));
            if (e.getLocation() != null && !e.getLocation().isEmpty()) {
                sb.append(String.format("   📍 %s\n", e.getLocation()));
            }
            sb.append("───────────────────────────────────────\n");
        }

        return sb.toString();
    }

    /**
     * Check if there are any events in the response
     */
    public boolean hasEvents() {
        return events != null && !events.isEmpty();
    }

    /**
     * Get count of events
     */
    public int getEventCount() {
        return events != null ? events.size() : 0;
    }

    // ===== GETTERS AND SETTERS =====

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ActionType getActionPerformed() {
        return actionPerformed;
    }

    public void setActionPerformed(ActionType actionPerformed) {
        this.actionPerformed = actionPerformed;
    }

    public Event getCreatedEvent() {
        return createdEvent;
    }

    public void setCreatedEvent(Event createdEvent) {
        this.createdEvent = createdEvent;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return "EventResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", actionPerformed=" + actionPerformed +
                ", eventCount=" + getEventCount() +
                '}';
    }
}