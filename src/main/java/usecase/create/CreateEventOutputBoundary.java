package usecase.create;

import entity.EventResponse;

/**
 * Output Boundary for Create Event use case.
 * Presenter implements this to receive results.
 */
public interface CreateEventOutputBoundary {

    /**
     * Present successful event creation
     * @param response The response containing created event
     */
    void presentSuccess(EventResponse response);

    /**
     * Present failure
     * @param response The response containing error details
     */
    void presentFailure(EventResponse response);
}
