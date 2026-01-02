package api.controller;

import api.dto.*;
import entity.*;
import Framework.*;
import interface_adapter.presenter.ApiPresenter;
import usecase.*;
import usecase.create.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:3000")
public class EventController {

    private final AIEventParser aiParser;
    private final UserManager userManager;

    public EventController() {
        this.aiParser = new AIEventParser();
        this.userManager = new UserManager();
        System.out.println("✅ EventController initialized");
    }

    @PostMapping(value = "/parse",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ParsedEventDTO> parseNaturalLanguage(
            @RequestBody NaturalLanguageRequestDTO request) {

        System.out.println("📥 Received parse request: " + request.getText());

        try {
            EventRequest parsedRequest = aiParser.parseNaturalLanguage(request.getText());

            if (!parsedRequest.isSuccessful()) {
                System.out.println("❌ Parse failed: " + parsedRequest.getErrorMessage());
                ParsedEventDTO errorDto = ParsedEventDTO.fromError(parsedRequest.getErrorMessage());
                return ResponseEntity.badRequest().body(errorDto);
            }

            System.out.println("✅ Parse successful: " + parsedRequest.getTitle());
            ParsedEventDTO successDto = ParsedEventDTO.fromEventRequest(parsedRequest);
            return ResponseEntity.ok(successDto);

        } catch (Exception e) {
            System.err.println("❌ Exception during parse: " + e.getMessage());
            e.printStackTrace();
            ParsedEventDTO errorDto = ParsedEventDTO.fromError("Server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
        }
    }

    @PostMapping(value = "/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventResponseDTO> createEvent(
            @RequestBody CreateEventDTO dto,
            @RequestHeader("User-Id") String userId) {

        System.out.println("📥 Received create event request for user: " + userId);
        System.out.println("📅 Event: " + dto.getTitle() + " on " + dto.getDate() + " at " + dto.getStartTime());

        try {
            // Get user from UserManager
            User user = userManager.getUserById(userId);
            if (user == null) {
                System.out.println("❌ User not found: " + userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(EventResponseDTO.error("User not found. Please login again."));
            }

            System.out.println("✅ User found: " + user.getEmail());

            // Initialize Google Calendar for this user
            System.out.println("🔐 Connecting to Google Calendar...");
            CalendarGateway gateway = new MultiUserGoogleCalendarGateway(user);

            if (!gateway.isAvailable()) {
                System.out.println("❌ Calendar gateway not available");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(EventResponseDTO.error("Could not connect to Google Calendar. Please authenticate."));
            }

            System.out.println("✅ Connected to Google Calendar");

            // Create the use case
            ApiPresenter presenter = new ApiPresenter();
            CreateEventInputBoundary useCase = new CreateEventInteractor(gateway, presenter);

            // Convert DTO to EventRequest
            EventRequest request = dto.toEventRequest();

            // Execute use case
            System.out.println("📝 Creating event in Google Calendar...");
            useCase.execute(request);

            // Get response from presenter
            EventResponse response = presenter.getResponse();

            if (response.isSuccess()) {
                System.out.println("✅ Event created successfully!");
                return ResponseEntity.ok(EventResponseDTO.fromEventResponse(response));
            } else {
                System.out.println("❌ Failed to create event: " + response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(EventResponseDTO.fromEventResponse(response));
            }

        } catch (Exception e) {
            System.err.println("❌ Exception during event creation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(EventResponseDTO.error("Server error: " + e.getMessage()));
        }
    }
}