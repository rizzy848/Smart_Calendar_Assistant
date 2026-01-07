package api.controller;

import api.dto.*;
import entity.*;
import Framework.*;
import interface_adapter.presenter.ApiPresenter;
import usecase.*;
import usecase.create.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:3000")
public class EventController {

    private final AIEventParser aiParser;
    private final UserManager userManager;

    // Cache calendar gateways per user to avoid re-authentication
    private final Map<String, MultiUserGoogleCalendarGateway> userGateways;

    // Store OAuth URLs for users
    private final Map<String, String> pendingOAuthUrls;

    public EventController() {
        this.aiParser = new AIEventParser();
        this.userManager = new UserManager();
        this.userGateways = new ConcurrentHashMap<>();
        this.pendingOAuthUrls = new ConcurrentHashMap<>();
        System.out.println("✅ EventController initialized");
    }

    @PostMapping(value = "/parse",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ParsedEventDTO> parseNaturalLanguage(
            @RequestBody NaturalLanguageRequestDTO request) {

        System.out.println("\n📥 ============================================");
        System.out.println("📥 PARSE REQUEST");
        System.out.println("📥 Input: " + request.getText());
        System.out.println("📥 ============================================");

        try {
            EventRequest parsedRequest = aiParser.parseNaturalLanguage(request.getText());

            if (!parsedRequest.isSuccessful()) {
                System.out.println("❌ Parse failed: " + parsedRequest.getErrorMessage());
                ParsedEventDTO errorDto = ParsedEventDTO.fromError(parsedRequest.getErrorMessage());
                return ResponseEntity.badRequest().body(errorDto);
            }

            System.out.println("✅ Parse successful!");
            System.out.println("   Title: " + parsedRequest.getTitle());
            System.out.println("   Date: " + parsedRequest.getDate());
            System.out.println("   Time: " + parsedRequest.getStartTime());

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
        Map<String, Object> response = new HashMap<>();

        User user = userManager.getUserById(userId);
        if (user == null) {
            response.put("needsAuth", true);
            response.put("error", "User not found");
            return ResponseEntity.ok(response);
        }

        // Check if we have a cached gateway
        MultiUserGoogleCalendarGateway gateway = userGateways.get(userId);
        boolean needsAuth = (gateway == null || !gateway.isAvailable());

        response.put("needsAuth", needsAuth);
        response.put("userEmail", user.getEmail());

        System.out.println("🔍 Auth check for " + userId + ": needsAuth=" + needsAuth);
        return ResponseEntity.ok(response);
    }

    /**
     * Get OAuth URL for user to authenticate
     */
    @GetMapping("/auth/url/{userId}")
    public ResponseEntity<Map<String, String>> getOAuthUrl(@PathVariable String userId) {
        System.out.println("\n🔐 ============================================");
        System.out.println("🔐 OAUTH URL REQUEST");
        System.out.println("🔐 User ID: " + userId);

        try {
            User user = userManager.getUserById(userId);
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            System.out.println("🔐 User: " + user.getEmail());
            System.out.println("🔐 Generating OAuth URL...");

            // This will trigger OAuth flow and store the URL
            MultiUserGoogleCalendarGateway gateway = new MultiUserGoogleCalendarGateway(user);
            String oauthUrl = gateway.getAuthorizationUrl();

            if (oauthUrl != null) {
                pendingOAuthUrls.put(userId, oauthUrl);
                Map<String, String> response = new HashMap<>();
                response.put("authUrl", oauthUrl);
                response.put("message", "Please authenticate with Google");

                System.out.println("✅ OAuth URL generated");
                System.out.println("🔗 URL: " + oauthUrl.substring(0, Math.min(100, oauthUrl.length())) + "...");
                System.out.println("🔐 ============================================\n");

                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Failed to generate OAuth URL");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }

        } catch (Exception e) {
            System.err.println("❌ Error generating OAuth URL: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to initialize OAuth: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping(value = "/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventResponseDTO> createEvent(
            @RequestBody CreateEventDTO dto,
            @RequestHeader("User-Id") String userId) {

        System.out.println("\n📅 ============================================");
        System.out.println("📅 CREATE EVENT REQUEST");
        System.out.println("📅 User ID: " + userId);
        System.out.println("📅 Event: " + dto.getTitle());
        System.out.println("📅 Date: " + dto.getDate());
        System.out.println("📅 Time: " + dto.getStartTime());
        System.out.println("📅 ============================================");

        try {
            // Get user from UserManager
            User user = userManager.getUserById(userId);
            if (user == null) {
                System.out.println("❌ User not found: " + userId);
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
                    System.out.println("❌ Failed to initialize Google Calendar");
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body(EventResponseDTO.error(
                                    "Could not connect to Google Calendar. Please authenticate first."));
                }
            } else {
                System.out.println("✅ Using cached Google Calendar connection");
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
                System.out.println("✅ Event created successfully!");
                System.out.println("   Event ID: " + response.getCreatedEvent().getId());
                System.out.println("📅 ============================================\n");
                return ResponseEntity.ok(EventResponseDTO.fromEventResponse(response));
            } else {
                System.out.println("❌ Failed to create event: " + response.getMessage());
                System.out.println("📅 ============================================\n");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(EventResponseDTO.fromEventResponse(response));
            }

        } catch (Exception e) {
            System.err.println("❌ Exception during event creation:");
            System.err.println("   Message: " + e.getMessage());
            System.err.println("   Type: " + e.getClass().getName());
            e.printStackTrace();
            System.out.println("📅 ============================================\n");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(EventResponseDTO.error("Server error: " + e.getMessage()));
        }
    }

    /**
     * Clear cached gateway for a user (useful for forcing re-authentication)
     */
    @DeleteMapping("/cache/{userId}")
    public ResponseEntity<String> clearUserCache(@PathVariable String userId) {
        userGateways.remove(userId);
        pendingOAuthUrls.remove(userId);
        System.out.println("🗑️  Cleared calendar cache for user: " + userId);
        return ResponseEntity.ok("Cache cleared");
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("status", "OK");
        status.put("aiParserAvailable", aiParser.isAvailable());
        status.put("activeUsers", userGateways.size());
        return ResponseEntity.ok(status);
    }
}