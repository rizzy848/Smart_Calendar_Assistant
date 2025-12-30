package usecase.create;

import entity.EventRequest;

/**
 * Input Boundary for Create Event use case.
 * Controller calls this to create an event.
 */
public interface CreateEventInputBoundary {

    /**
     * Execute the create event use case
     * @param request The parsed event request from AI
     */
    void execute(EventRequest request);
}