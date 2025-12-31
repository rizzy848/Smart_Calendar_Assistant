package app;

import Framework.AIEventParser;
import Framework.GoogleCalendarGateway;
import entity.ActionType;
import entity.EventRequest;
import interface_adapter.presenter.ConsolePresenter;
import usecase.CalendarGateway;
import usecase.create.CreateEventInputBoundary;
import usecase.create.CreateEventInteractor;
import usecase.create.CreateEventOutputBoundary;

import java.util.Scanner;

/**
 * AI-powered calendar assistant with natural language processing.
 * Users can type commands like "Schedule meeting tomorrow at 2 PM"
 */
public class AICalendarMain {

    public static void main(String[] args) {
        printWelcomeBanner();

        // Initialize dependencies
        CalendarGateway calendarGateway = new GoogleCalendarGateway();
        AIEventParser aiParser = new AIEventParser();
        CreateEventOutputBoundary presenter = new ConsolePresenter();
        CreateEventInputBoundary createEventUseCase = new CreateEventInteractor(calendarGateway, presenter);

        // Check services availability
        if (!calendarGateway.isAvailable()) {
            System.out.println("❌ Google Calendar is not available. Please check your credentials.");
            return;
        }

        if (!aiParser.isAvailable()) {
            System.out.println("⚠️  AI Parser not available. Using manual input mode.");
            // Could fall back to manual mode here
        }

        Scanner scanner = new Scanner(System.in);

        System.out.println("\n🎤 AI Calendar Assistant is ready!");
        System.out.println("💡 Try commands like:");
        System.out.println("   • \"Schedule team meeting tomorrow at 2 PM\"");
        System.out.println("   • \"Add dentist appointment next Monday at 10:30 AM\"");
        System.out.println("   • \"Create workout session today at 6 PM for 1 hour\"");
        System.out.println("\n📝 Type 'exit' to quit\n");

        // Main interaction loop
        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
                System.out.println("\n👋 Goodbye! Have a productive day!");
                break;
            }

            if (userInput.isEmpty()) {
                continue;
            }

            // Parse natural language with AI
            System.out.println("🤔 Understanding your request...");
            EventRequest request = aiParser.parseNaturalLanguage(userInput);

            // Display what was understood
            if (request.isSuccessful()) {
                System.out.println("✅ I understood:");
                displayParsedRequest(request);

                // Ask for confirmation
                System.out.print("\n❓ Proceed with this? (yes/no): ");
                String confirmation = scanner.nextLine().trim().toLowerCase();

                if (confirmation.equals("yes") || confirmation.equals("y")) {
                    // Execute the appropriate action
                    executeAction(request, createEventUseCase);
                } else {
                    System.out.println("❌ Cancelled. Try rephrasing your request.\n");
                }
            } else {
                System.out.println("❌ Sorry, I couldn't understand that.");
                System.out.println("Error: " + request.getErrorMessage());
                System.out.println("💡 Try being more specific about the date and time.\n");
            }
        }

        scanner.close();
    }

    private static void printWelcomeBanner() {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║                                                  ║");
        System.out.println("║     🤖 AI-POWERED SMART CALENDAR ASSISTANT      ║");
        System.out.println("║                                                  ║");
        System.out.println("║  Just tell me what you want to schedule and     ║");
        System.out.println("║  I'll understand using natural language! 🎯      ║");
        System.out.println("║                                                  ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
    }

    private static void displayParsedRequest(EventRequest request) {
        System.out.println("┌─────────────────────────────────────┐");
        System.out.println("│  Action: " + request.getActionType());

        if (request.getTitle() != null) {
            System.out.println("│  Title: " + request.getTitle());
        }

        if (request.getDate() != null) {
            System.out.println("│  Date: " + request.getDate());
        }

        if (request.getStartTime() != null) {
            System.out.println("│  Start: " + request.getStartTime());
        }

        if (request.getEndTime() != null) {
            System.out.println("│  End: " + request.getEndTime());
        } else if (request.getStartTime() != null) {
            System.out.println("│  End: " + request.getStartTime().plusHours(1) + " (default)");
        }

        if (request.getLocation() != null) {
            System.out.println("│  Location: " + request.getLocation());
        }

        System.out.println("└─────────────────────────────────────┘");
    }

    private static void executeAction(EventRequest request, CreateEventInputBoundary createEventUseCase) {
        switch (request.getActionType()) {
            case CREATE:
                System.out.println("\n📅 Creating event...");
                createEventUseCase.execute(request);
                System.out.println(); // Add spacing
                break;

            case VIEW:
                System.out.println("\n📋 View schedule feature coming soon!");
                System.out.println("💡 This will show all events for the specified date.\n");
                break;

            case DELETE:
                System.out.println("\n🗑️  Delete event feature coming soon!");
                System.out.println("💡 This will remove the specified event.\n");
                break;

            case UPDATE:
                System.out.println("\n✏️  Update event feature coming soon!");
                System.out.println("💡 This will modify the specified event.\n");
                break;

            default:
                System.out.println("❌ Unknown action type.\n");
        }
    }
}
