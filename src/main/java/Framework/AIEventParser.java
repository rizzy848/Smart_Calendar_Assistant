package Framework;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import entity.ActionType;
import entity.EventRequest;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uses Google Gemini AI to parse natural language into EventRequest objects.
 * Example: "Schedule meeting with John tomorrow at 2 PM"
 *       -> EventRequest with title, date, time extracted
 */
public class AIEventParser {

    private Client geminiClient;
    private String modelName;
    private static final String CONFIG_PATH = "src/main/resources/config/Gemini.properties";

    public AIEventParser() {
        try {
            initializeGemini();
        } catch (IOException e) {
            System.err.println("Failed to initialize Gemini AI: " + e.getMessage());
        }
    }

    private void initializeGemini() throws IOException {
        // Try environment variable first
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            // Fall back to properties file (local dev)
            Properties props = new Properties();
            props.load(new FileInputStream(CONFIG_PATH));
            apiKey = props.getProperty("gemini.api.key");
        }

        this.modelName = "gemini-2.5-flash";
        this.geminiClient = new Client.Builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Parse natural language into an EventRequest
     * @param userInput Natural language input like "Meeting tomorrow at 3pm"
     * @return EventRequest with extracted information
     */
    public EventRequest parseNaturalLanguage(String userInput) {
        if (geminiClient == null) {
            return EventRequest.failed("AI service not available", userInput);
        }

        try {
            // Create a detailed prompt for Gemini
            String prompt = createPrompt(userInput);

            // Call Gemini API (v1.0.0 syntax is simpler)
            GenerateContentResponse response =
                    geminiClient.models.generateContent("gemini-2.5-flash", prompt, null);
            String aiResponse = response.text();

            // Parse the structured response from AI
            return parseAIResponse(aiResponse, userInput);

        } catch (Exception e) {
            System.err.println("AI parsing failed: " + e.getMessage());
            e.printStackTrace();
            return EventRequest.failed("Failed to parse input: " + e.getMessage(), userInput);
        }
    }

    private String createPrompt(String userInput) {
        LocalDate today = LocalDate.now();

        return String.format("""
            You are a calendar assistant. Parse the following user input into calendar event details.
            Today's date is: %s
            
            User input: "%s"
            
            Extract the following information and respond ONLY in this exact format:
            ACTION: [CREATE/VIEW/DELETE/UPDATE]
            TITLE: [event title]
            DATE: [YYYY-MM-DD format]
            START_TIME: [HH:MM in 24-hour format]
            END_TIME: [HH:MM in 24-hour format, or leave empty]
            LOCATION: [location or leave empty]
            
            Rules:
            - If no end time specified, leave END_TIME empty (default will be 1 hour after start)
            - Convert relative dates like "tomorrow", "next Monday" to actual dates
            - Convert 12-hour time (2 PM) to 24-hour format (14:00)
            - If date is ambiguous, use the nearest future occurrence
            - If action is unclear, default to CREATE
            - Keep title concise but descriptive
            
            Example inputs and outputs:
            Input: "Meeting with John tomorrow at 2 PM"
            ACTION: CREATE
            TITLE: Meeting with John
            DATE: %s
            START_TIME: 14:00
            END_TIME: 
            LOCATION: 
            
            Input: "What's on my calendar next Monday?"
            ACTION: VIEW
            TITLE: 
            DATE: [next Monday's date]
            START_TIME: 
            END_TIME: 
            LOCATION: 
            
            Now parse the user input above.
            """,
                today,
                userInput,
                today.plusDays(1)
        );
    }

    private EventRequest parseAIResponse(String aiResponse, String originalInput) {
        try {
            EventRequest request = new EventRequest();
            request.setRawQuery(originalInput);

            // Parse each field using regex
            request.setActionType(extractAction(aiResponse));
            request.setTitle(extractField(aiResponse, "TITLE"));
            request.setDate(extractDate(aiResponse));
            request.setStartTime(extractTime(aiResponse, "START_TIME"));
            request.setEndTime(extractTime(aiResponse, "END_TIME"));
            request.setLocation(extractField(aiResponse, "LOCATION"));

            // Validate the parsed request
            if (request.getActionType() == ActionType.CREATE &&
                    (request.getTitle() == null || request.getTitle().isEmpty())) {
                return EventRequest.failed("Could not extract event title", originalInput);
            }

            if (request.getDate() == null &&
                    (request.getActionType() == ActionType.CREATE ||
                            request.getActionType() == ActionType.VIEW)) {
                return EventRequest.failed("Could not extract valid date", originalInput);
            }

            request.setSuccessful(true);
            return request;

        } catch (Exception e) {
            return EventRequest.failed("Error parsing AI response: " + e.getMessage(), originalInput);
        }
    }

    private ActionType extractAction(String response) {
        String actionLine = extractField(response, "ACTION");
        if (actionLine == null) return ActionType.CREATE;

        return switch (actionLine.toUpperCase()) {
            case "VIEW" -> ActionType.VIEW;
            case "DELETE" -> ActionType.DELETE;
            case "UPDATE" -> ActionType.UPDATE;
            default -> ActionType.CREATE;
        };
    }

    private String extractField(String response, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            String value = matcher.group(1).trim();
            return value.isEmpty() || value.equals("empty") ? null : value;
        }
        return null;
    }

    private LocalDate extractDate(String response) {
        String dateStr = extractField(response, "DATE");
        if (dateStr == null) return null;

        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            System.err.println("Failed to parse date: " + dateStr);
            return null;
        }
    }

    private LocalTime extractTime(String response, String fieldName) {
        String timeStr = extractField(response, fieldName);
        if (timeStr == null) return null;

        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            System.err.println("Failed to parse time: " + timeStr);
            return null;
        }
    }

    /**
     * Test if AI service is available
     */
    public boolean isAvailable() {
        return geminiClient != null;
    }
}
