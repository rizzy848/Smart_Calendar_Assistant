package api.controller;

import api.dto.*;
import entity.*;
import Framework.*;
import interface_adapter.presenter.ApiPresenter;
import usecase.*;
import usecase.create.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST Controller for calendar event operations.
 * Handles natural language parsing, event creation, and OAuth authentication.
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:3000")
public class EventController {

    private final AIEventParser aiParser;
    private final UserManager userManager;

    // Cache calendar gateways per user to avoid re-authentication
    private final Map<String, MultiUserGoogleCalendarGateway> userGateways;

    @Autowired
    public EventController(UserManager userManager) {
        this.aiParser = new AIEventParser();
        this.userManager = userManager;
        this.userGateways = new ConcurrentHashMap<>();

        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║     EVENT CONTROLLER INITIALIZED               ║");
        System.out.println("║     AI Parser: " + (aiParser.isAvailable() ? "✅ Ready" : "❌ Not available") + "                       ║");
        System.out.println("║     UserManager: ✅ Shared (Spring Bean)      ║");
        System.out.println("╚════════════════════════════════════════════════╝");
    }

    /**
     * Health check endpoint - verify backend is running
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "OK");
        status.put("aiParserAvailable", aiParser.isAvailable());
        status.put("activeUsers", userGateways.size());
        status.put("registeredUsers", userManager.getAllUsers().size());
        status.put("timestamp", System.currentTimeMillis());

        System.out.println("📊 Health check - AI: " + aiParser.isAvailable() +
                ", Active users: " + userGateways.size() +
                ", Registered: " + userManager.getAllUsers().size());

        return ResponseEntity.ok(status);
    }

    /**
     * Parse natural language input into structured event data
     */
    @PostMapping(value = "/parse",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ParsedEventDTO> parseNaturalLanguage(
            @RequestBody NaturalLanguageRequestDTO request) {

        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║            PARSE REQUEST                       ║");
        System.out.println("╠════════════════════════════════════════════════╣");
        System.out.println("  Input: " + request.getText());
        System.out.println("╚════════════════════════════════════════════════╝");

        try {
            // Validate input
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                System.out.println("❌ Empty input received");
                return ResponseEntity.badRequest()
                        .body(ParsedEventDTO.fromError("Please provide an event description"));
            }

            // Parse with AI
            EventRequest parsedRequest = aiParser.parseNaturalLanguage(request.getText());

            if (!parsedRequest.isSuccessful()) {
                System.out.println("❌ Parse failed: " + parsedRequest.getErrorMessage());
                ParsedEventDTO errorDto = ParsedEventDTO.fromError(parsedRequest.getErrorMessage());
                return ResponseEntity.badRequest().body(errorDto);
            }

            System.out.println("✅ Parse successful!");
            System.out.println("   Title: " + parsedRequest.getTitle());
            System.out.println("   Date: " + parsedRequest.getDate());
            System.out.println("   Time: " + parsedRequest.getStartTime() + " - " + parsedRequest.getEndTime());
            if (parsedRequest.getLocation() != null) {
                System.out.println("   Location: " + parsedRequest.getLocation());
            }

            ParsedEventDTO successDto = ParsedEventDTO.fromEventRequest(parsedRequest);
            return ResponseEntity.ok(successDto);

        } catch (Exception e) {
            System.err.println("❌ Exception during parse: " + e.getMessage());
            e.printStackTrace();
            ParsedEventDTO errorDto = ParsedEventDTO.fromError("Server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
        }
    }

    /**
     * Check if user needs OAuth authentication
     */
    @GetMapping("/auth/check/{userId}")
    public ResponseEntity<Map<String, Object>> checkAuthStatus(@PathVariable String userId) {
        System.out.println("\n🔍 Checking auth status for user: " + userId);

        Map<String, Object> response = new HashMap<>();

        try {
            User user = userManager.getUserById(userId);
            if (user == null) {
                System.out.println("❌ User not found: " + userId);
                System.out.println("   Available users: " + userManager.getAllUsers().size());
                response.put("needsAuth", true);
                response.put("error", "User not found");
                return ResponseEntity.ok(response);
            }

            System.out.println("✅ User found: " + user.getEmail());

            // Check if we have a cached gateway
            MultiUserGoogleCalendarGateway gateway = userGateways.get(userId);
            boolean needsAuth = (gateway == null || !gateway.isAvailable());

            response.put("needsAuth", needsAuth);
            response.put("userEmail", user.getEmail());
            response.put("authenticated", !needsAuth);

            System.out.println(needsAuth ? "⚠️  User needs authentication" : "✅ User is authenticated");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error checking auth status: " + e.getMessage());
            response.put("needsAuth", true);
            response.put("error", "Failed to check authentication status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get OAuth URL for user to authenticate with Google
     */
    @GetMapping("/auth/url/{userId}")
    public ResponseEntity<Map<String, String>> getOAuthUrl(@PathVariable String userId) {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║         OAUTH URL REQUEST                      ║");
        System.out.println("╠════════════════════════════════════════════════╣");
        System.out.println("  User ID: " + userId);

        try {
            User user = userManager.getUserById(userId);
            if (user == null) {
                System.out.println("❌ User not found: " + userId);
                System.out.println("   Available users: " + userManager.getAllUsers().size());
                System.out.println("╚════════════════════════════════════════════════╝");

                Map<String, String> error = new HashMap<>();
                error.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            System.out.println("  User: " + user.getEmail());
            System.out.println("  Generating OAuth URL...");

            // Create gateway and get OAuth URL
            MultiUserGoogleCalendarGateway gateway = new MultiUserGoogleCalendarGateway(user);
            String oauthUrl = gateway.getAuthorizationUrl();

            if (oauthUrl != null) {
                // Cache the gateway for later use
                userGateways.put(userId, gateway);

                Map<String, String> response = new HashMap<>();
                response.put("authUrl", oauthUrl);
                response.put("message", "Please authenticate with Google");

                System.out.println("✅ OAuth URL generated successfully");
                System.out.println("  URL: " + oauthUrl.substring(0, Math.min(80, oauthUrl.length())) + "...");
                System.out.println("╚════════════════════════════════════════════════╝");

                return ResponseEntity.ok(response);
            } else {
                System.out.println("❌ Failed to generate OAuth URL");
                System.out.println("╚════════════════════════════════════════════════╝");

                Map<String, String> error = new HashMap<>();
                error.put("error", "Failed to generate OAuth URL");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }

        } catch (Exception e) {
            System.err.println("❌ Error generating OAuth URL: " + e.getMessage());
            e.printStackTrace();
            System.out.println("╚════════════════════════════════════════════════╝");

            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to initialize OAuth: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Create a calendar event
     */
    @PostMapping(value = "/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventResponseDTO> createEvent(
            @RequestBody CreateEventDTO dto,
            @RequestHeader("User-Id") String userId) {

        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║         CREATE EVENT REQUEST                   ║");
        System.out.println("╠════════════════════════════════════════════════╣");
        System.out.println("  User ID: " + userId);
        System.out.println("  Event: " + dto.getTitle());
        System.out.println("  Date: " + dto.getDate());
        System.out.println("  Time: " + dto.getStartTime() + " - " + dto.getEndTime());
        if (dto.getLocation() != null) {
            System.out.println("  Location: " + dto.getLocation());
        }
        System.out.println("╚════════════════════════════════════════════════╝");

        try {
            // Validate user
            User user = userManager.getUserById(userId);
            if (user == null) {
                System.out.println("❌ User not found: " + userId);
                System.out.println("   Available users: " + userManager.getAllUsers().size());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(EventResponseDTO.error("User not found. Please login again."));
            }

            System.out.println("✅ User found: " + user.getEmail());

            // Get or create calendar gateway for this user
            MultiUserGoogleCalendarGateway gateway = userGateways.get(userId);

            if (gateway == null || !gateway.isAvailable()) {
                System.out.println("🔐 Initializing Google Calendar for " + user.getEmail() + "...");

                gateway = new MultiUserGoogleCalendarGateway(user);

                if (gateway.isAvailable()) {
                    // Cache the gateway for future requests
                    userGateways.put(userId, gateway);
                    System.out.println("✅ Google Calendar initialized and cached");
                } else {
                    System.out.println("❌ Google Calendar not available - authentication required");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(EventResponseDTO.error(
                                    "Google Calendar authentication required. Please authenticate first."));
                }
            } else {
                System.out.println("✅ Using cached Google Calendar connection");
            }

            // Validate event data
            if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(EventResponseDTO.error("Event title is required"));
            }
            if (dto.getDate() == null || dto.getStartTime() == null) {
                return ResponseEntity.badRequest()
                        .body(EventResponseDTO.error("Event date and start time are required"));
            }

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
                System.out.println("╔════════════════════════════════════════════════╗");
                System.out.println("║              ✅ SUCCESS                        ║");
                System.out.println("╠════════════════════════════════════════════════╣");
                System.out.println("  Event created successfully!");
                if (response.getCreatedEvent() != null) {
                    System.out.println("  Event ID: " + response.getCreatedEvent().getId());
                }
                System.out.println("╚════════════════════════════════════════════════╝");

                return ResponseEntity.ok(EventResponseDTO.fromEventResponse(response));
            } else {
                System.out.println("❌ Failed to create event: " + response.getMessage());
                System.out.println("   Error code: " + response.getErrorCode());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(EventResponseDTO.fromEventResponse(response));
            }

        } catch (Exception e) {
            System.err.println("╔════════════════════════════════════════════════╗");
            System.err.println("║              ❌ EXCEPTION                      ║");
            System.err.println("╠════════════════════════════════════════════════╣");
            System.err.println("  Message: " + e.getMessage());
            System.err.println("  Type: " + e.getClass().getName());
            System.err.println("╚════════════════════════════════════════════════╝");
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(EventResponseDTO.error("Server error: " + e.getMessage()));
        }
    }

    /**
     * Clear cached gateway for a user (force re-authentication)
     */
    @DeleteMapping("/cache/{userId}")
    public ResponseEntity<Map<String, String>> clearUserCache(@PathVariable String userId) {
        userGateways.remove(userId);
        System.out.println("🗑️  Cleared calendar cache for user: " + userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache cleared successfully");
        response.put("userId", userId);

        return ResponseEntity.ok(response);
    }
}