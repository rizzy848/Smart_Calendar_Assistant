package usecase.create;

import entity.Event;
import entity.EventRequest;
import entity.EventResponse;
import usecase.CalendarException;
import usecase.CalendarGateway;

/**
 * Interactor for Create Event use case.
 * Contains the business logic for creating calendar events.
 */
public class CreateEventInteractor implements CreateEventInputBoundary {

    private final CalendarGateway calendarGateway;
    private final CreateEventOutputBoundary outputBoundary;

    /**
     * Constructor with dependency injection
     * @param calendarGateway Gateway to interact with calendar service
     * @param outputBoundary Presenter to send results to
     */
    public CreateEventInteractor(CalendarGateway calendarGateway,
                                 CreateEventOutputBoundary outputBoundary) {
        this.calendarGateway = calendarGateway;
        this.outputBoundary = outputBoundary;
    }

    @Override
    public void execute(EventRequest request) {
        // Step 1: Validate the request
        if (!request.isValid()) {
            EventResponse errorResponse = EventResponse.error(
                    "Invalid event request: " + request.getErrorMessage(),
                    "INVALID_REQUEST"
            );
            outputBoundary.presentFailure(errorResponse);
            return;
        }

        // Step 2: Check if calendar service is available
        if (!calendarGateway.isAvailable()) {
            EventResponse errorResponse = EventResponse.error(
                    "Calendar service is not available. Please check your connection.",
                    "SERVICE_UNAVAILABLE"
            );
            outputBoundary.presentFailure(errorResponse);
            return;
        }

        try {
            // Step 3: Convert request to Event entity
            Event event = request.toEvent();

            // Step 4: Create event via gateway
            Event createdEvent = calendarGateway.createEvent(event);

            // Step 5: Build success response
            EventResponse successResponse = EventResponse.createSuccess(createdEvent);

            // Step 6: Present success to user
            outputBoundary.presentSuccess(successResponse);

        } catch (CalendarException e) {
            // Step 7: Handle any calendar errors
            EventResponse errorResponse = EventResponse.error(
                    "Failed to create event: " + e.getMessage(),
                    e.getErrorCode()
            );
            outputBoundary.presentFailure(errorResponse);
        }
    }
}