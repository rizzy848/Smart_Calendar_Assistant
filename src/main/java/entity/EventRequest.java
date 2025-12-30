package entity;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a parsed request from the AI.
 * When user says "Add meeting tomorrow at 3 PM",
 * AI extracts the data and returns it in this format.
 */
public class EventRequest {

    private ActionType actionType;      // CREATE, VIEW, DELETE, UPDATE
    private String title;               // "meeting"
    private String description;         // optional description
    private LocalDate date;             // tomorrow's date
    private LocalTime startTime;        // 15:00
    private LocalTime endTime;          // 16:00 (default 1 hour if not specified)
    private String location;            // optional location
    private String eventId;             // for DELETE/UPDATE - which event to modify
    private String rawQuery;            // original user input
    private boolean successful;         // did AI parse successfully?
    private String errorMessage;        // if parsing failed, why?

    // ===== CONSTRUCTORS =====

    public EventRequest() {
        this.successful = true;
    }

    /**
     * Quick constructor for successful CREATE requests
     */
    public EventRequest(ActionType actionType, String title, LocalDate date,
                        LocalTime startTime, LocalTime endTime) {
        this.actionType = actionType;
        this.title = title;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.successful = true;
    }

    /**
     * Constructor for failed parsing
     */
    public static EventRequest failed(String errorMessage, String rawQuery) {
        EventRequest request = new EventRequest();
        request.successful = false;
        request.errorMessage = errorMessage;
        request.rawQuery = rawQuery;
        request.actionType = ActionType.UNKNOWN;
        return request;
    }

    // ===== BUSINESS LOGIC =====

    /**
     * Check if this is a valid request that can be processed
     */
    public boolean isValid() {
        if (!successful) {
            return false;
        }

        if (actionType == null || actionType == ActionType.UNKNOWN) {
            return false;
        }

        // CREATE requires at least title and date
        if (actionType == ActionType.CREATE) {
            return title != null && !title.isEmpty() && date != null;
        }

        // VIEW requires at least a date
        if (actionType == ActionType.VIEW) {
            return date != null;
        }

        // DELETE/UPDATE requires event identifier
        if (actionType == ActionType.DELETE || actionType == ActionType.UPDATE) {
            return eventId != null || (title != null && date != null);
        }

        return false;
    }

    /**
     * Convert this request to an Event entity
     */
    public Event toEvent() {
        Event event = new Event();
        event.setTitle(this.title);
        event.setDescription(this.description);
        event.setDate(this.date);
        event.setStartTime(this.startTime);
        event.setEndTime(this.endTime != null ? this.endTime :
                (this.startTime != null ? this.startTime.plusHours(1) : null));
        event.setLocation(this.location);
        return event;
    }

    /**
     * Get a human-readable summary of what user wants
     */
    public String getSummary() {
        if (!successful) {
            return "Failed to understand: " + errorMessage;
        }

        switch (actionType) {
            case CREATE:
                return String.format("Create '%s' on %s at %s", title, date, startTime);
            case VIEW:
                return String.format("View schedule for %s", date);
            case DELETE:
                return String.format("Delete '%s' on %s", title, date);
            case UPDATE:
                return String.format("Update '%s' on %s", title, date);
            default:
                return "Unknown action";
        }
    }

    // ===== GETTERS AND SETTERS =====

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getRawQuery() {
        return rawQuery;
    }

    public void setRawQuery(String rawQuery) {
        this.rawQuery = rawQuery;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "EventRequest{" +
                "actionType=" + actionType +
                ", title='" + title + '\'' +
                ", date=" + date +
                ", startTime=" + startTime +
                ", successful=" + successful +
                '}';
    }
}