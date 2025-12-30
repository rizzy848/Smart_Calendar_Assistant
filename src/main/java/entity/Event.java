package entity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Represents a calendar event.
 * This is a core domain entity - no external dependencies.
 */
public class Event {

    private String id;              // Google Calendar event ID (null for new events)
    private String title;           // "Team Meeting"
    private String description;     // "Discuss Q4 goals"
    private LocalDate date;         // 2024-12-17
    private LocalTime startTime;    // 15:00
    private LocalTime endTime;      // 16:00
    private String location;        // "Room 101" or "Zoom"

    // ===== CONSTRUCTORS =====

    /**
     * Default constructor
     */
    public Event() {
    }

    /**
     * Constructor for creating a new event (no ID yet)
     */
    public Event(String title, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.title = title;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Full constructor
     */
    public Event(String id, String title, String description, LocalDate date,
                 LocalTime startTime, LocalTime endTime, String location) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
    }

    // ===== BUSINESS LOGIC =====

    /**
     * Calculate event duration in minutes
     */
    public int getDurationInMinutes() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return (int) java.time.Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Check if event is an all-day event (no specific time)
     */
    public boolean isAllDay() {
        return startTime == null && endTime == null;
    }

    /**
     * Check if this event overlaps with another event
     */
    public boolean overlapsWith(Event other) {
        if (other == null || !this.date.equals(other.date)) {
            return false;
        }
        // Event A overlaps with B if A starts before B ends AND A ends after B starts
        return this.startTime.isBefore(other.endTime) &&
                this.endTime.isAfter(other.startTime);
    }

    /**
     * Check if event is in the past
     */
    public boolean isPast() {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            return true;
        }
        if (date.equals(today) && startTime != null) {
            return startTime.isBefore(LocalTime.now());
        }
        return false;
    }

    // ===== GETTERS AND SETTERS =====

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getEventId(){
        return id;
    }

    // ===== OBJECT METHODS =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id) &&
                Objects.equals(title, event.title) &&
                Objects.equals(date, event.date) &&
                Objects.equals(startTime, event.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, date, startTime);
    }

    @Override
    public String toString() {
        return String.format("Event{title='%s', date=%s, time=%s-%s, location='%s'}",
                title, date, startTime, endTime, location);
    }
}