package usecase;

import entity.Event;
import java.time.LocalDate;
import java.util.List;

/**
 * Gateway interface for calendar operations.
 * This abstraction allows us to swap calendar implementations
 * (Google Calendar, Outlook, etc.) without changing use case code.
 *
 * Follows Dependency Inversion Principle - use cases depend on
 * this abstraction, not on concrete Google Calendar implementation.
 */
public interface CalendarGateway {

    /**
     * Create a new event in the calendar
     * @param event The event to create
     * @return The created event with ID assigned
     * @throws CalendarException if creation fails
     */
    Event createEvent(Event event) throws CalendarException;

    /**
     * Get all events for a specific date
     * @param date The date to query
     * @return List of events (empty if none found)
     * @throws CalendarException if retrieval fails
     */
    List<Event> getEventsForDate(LocalDate date) throws CalendarException;

    /**
     * Get events within a date range
     * @param startDate Start of range (inclusive)
     * @param endDate End of range (inclusive)
     * @return List of events (empty if none found)
     * @throws CalendarException if retrieval fails
     */
    List<Event> getEventsInRange(LocalDate startDate, LocalDate endDate)
            throws CalendarException;

    /**
     * Delete an event by its ID
     * @param eventId The ID of the event to delete
     * @throws CalendarException if deletion fails or event not found
     */
    void deleteEvent(String eventId) throws CalendarException;

    /**
     * Update an existing event
     * @param event The event with updated information
     * @return The updated event
     * @throws CalendarException if update fails
     */
    Event updateEvent(Event event) throws CalendarException;

    /**
     * Check if calendar service is available/authenticated
     * @return true if ready to use, false otherwise
     */
    boolean isAvailable();
}
