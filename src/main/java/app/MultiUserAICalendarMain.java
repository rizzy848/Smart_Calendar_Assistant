package app;

import Framework.AIEventParser;
import Framework.MultiUserGoogleCalendarGateway;
import Framework.UserManager;
import entity.ActionType;
import entity.EventRequest;
import entity.User;
import interface_adapter.presenter.ConsolePresenter;
import usecase.CalendarGateway;
import usecase.create.CreateEventInputBoundary;
import usecase.create.CreateEventInteractor;
import usecase.create.CreateEventOutputBoundary;

import java.util.List;
import java.util.Scanner;

/**
 * Multi-user AI-powered calendar assistant.
 * Features:
 * - Multiple users can register and login
 * - Each user has their own Google Calendar
 * - Natural language AI parsing
 * - Interactive console interface
 */
public class MultiUserAICalendarMain {

    private static UserManager userManager;
    private static AIEventParser aiParser;
    private static Scanner scanner;

    public static void main(String[] args) {
        printWelcomeBanner();

        // Initialize
        userManager = new UserManager();
        aiParser = new AIEventParser();
        scanner = new Scanner(System.in);

        // Check AI availability
        if (!aiParser.isAvailable()) {
            System.out.println("⚠️  AI Parser not available. Some features may be limited.");
        }

        // Main menu loop
        while (true) {
            if (!userManager.hasCurrentUser()) {
                // No user logged in - show login menu
                showLoginMenu();
            } else {
                // User logged in - show calendar menu
                showCalendarMenu();
            }
        }
    }

    private static void printWelcomeBanner() {
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║                                                       ║");
        System.out.println("║     🤖 MULTI-USER AI SMART CALENDAR ASSISTANT        ║");
        System.out.println("║                                                       ║");
        System.out.println("║  • Multiple users with separate calendars             ║");
        System.out.println("║  • Natural language AI processing                     ║");
        System.out.println("║  • Google Calendar integration                        ║");
        System.out.println("║                                                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝\n");
    }

    private static void showLoginMenu() {
        System.out.println("\n┌─────────────────────────────────┐");
        System.out.println("│      LOGIN / REGISTER MENU      │");
        System.out.println("├─────────────────────────────────┤");
        System.out.println("│  1. Register new user           │");
        System.out.println("│  2. Login existing user         │");
        System.out.println("│  3. List all users              │");
        System.out.println("│  4. Exit                        │");
        System.out.println("└─────────────────────────────────┘");
        System.out.print("\nChoose option: ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                registerNewUser();
                break;
            case "2":
                loginExistingUser();
                break;
            case "3":
                userManager.displayAllUsers();
                break;
            case "4":
                System.out.println("\n👋 Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("❌ Invalid option. Try again.");
        }
    }

    private static void registerNewUser() {
        System.out.println("\n=== USER REGISTRATION ===");

        System.out.print("Enter your name: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter your Google email: ");
        String email = scanner.nextLine().trim();

        if (username.isEmpty() || email.isEmpty()) {
            System.out.println("❌ Name and email cannot be empty!");
            return;
        }

        User user = userManager.registerUser(username, email);

        System.out.println("\n✅ Registration successful!");
        System.out.println("💡 You can now login with your email.");
    }

    private static void loginExistingUser() {
        List<User> users = userManager.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("\n❌ No users registered yet. Please register first.");
            return;
        }

        System.out.println("\n=== SELECT USER ===");
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            System.out.printf("%d. %s (%s)\n", i + 1, u.getDisplayName(), u.getEmail());
        }

        System.out.print("\nEnter number or email: ");
        String input = scanner.nextLine().trim();

        User selectedUser = null;

        // Try parsing as number first
        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < users.size()) {
                selectedUser = users.get(index);
            }
        } catch (NumberFormatException e) {
            // Input is email, not number
            selectedUser = userManager.loginUser(input);
        }

        if (selectedUser != null) {
            userManager.loginUser(selectedUser.getEmail());
        } else {
            System.out.println("❌ User not found.");
        }
    }

    private static void showCalendarMenu() {
        User currentUser = userManager.getCurrentUser();

        System.out.println("\n╔═══════════════════════════════════════════════════════╗");
        System.out.printf("║  📅 Welcome, %-42s ║\n", currentUser.getDisplayName());
        System.out.println("╚═══════════════════════════════════════════════════════╝");

        System.out.println("\n┌─────────────────────────────────────────┐");
        System.out.println("│            CALENDAR MENU                │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  1. Create event (AI natural language)  │");
        System.out.println("│  2. View schedule                       │");
        System.out.println("│  3. Delete event                        │");
        System.out.println("│  4. Update event                        │");
        System.out.println("│  5. Logout                              │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.print("\nChoose option: ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                createEventWithAI(currentUser);
                break;
            case "2":
                System.out.println("\n📋 View schedule feature coming soon!");
                break;
            case "3":
                System.out.println("\n🗑️  Delete event feature coming soon!");
                break;
            case "4":
                System.out.println("\n✏️  Update event feature coming soon!");
                break;
            case "5":
                userManager.logoutCurrentUser();
                break;
            default:
                System.out.println("❌ Invalid option. Try again.");
        }
    }

    private static void createEventWithAI(User currentUser) {
        System.out.println("\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║            AI EVENT CREATION                          ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");

        System.out.println("\n💡 Examples:");
        System.out.println("   • \"Schedule team meeting tomorrow at 2 PM\"");
        System.out.println("   • \"Add dentist appointment next Monday at 10:30 AM\"");
        System.out.println("   • \"Create workout session today at 6 PM for 1 hour\"");

        System.out.print("\n🎤 What would you like to schedule? (or 'back' to return): ");
        String userInput = scanner.nextLine().trim();

        if (userInput.equalsIgnoreCase("back") || userInput.isEmpty()) {
            return;
        }

        // Parse with AI
        System.out.println("🤔 Understanding your request...");
        EventRequest request = aiParser.parseNaturalLanguage(userInput);

        if (!request.isSuccessful()) {
            System.out.println("❌ Sorry, I couldn't understand that.");
            System.out.println("Error: " + request.getErrorMessage());
            System.out.println("💡 Try being more specific about the date and time.");
            return;
        }

        // Display what was understood
        System.out.println("\n✅ I understood:");
        displayParsedRequest(request);

        // Ask for confirmation
        System.out.print("\n❓ Proceed with creating this event? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();

        if (!confirmation.equals("yes") && !confirmation.equals("y")) {
            System.out.println("❌ Cancelled.");
            return;
        }

        // Initialize Google Calendar for this user
        System.out.println("\n🔐 Connecting to Google Calendar...");
        CalendarGateway calendarGateway = new MultiUserGoogleCalendarGateway(currentUser);

        if (!calendarGateway.isAvailable()) {
            System.out.println("❌ Could not connect to Google Calendar.");
            System.out.println("💡 Check your internet connection and credentials.");
            return;
        }

        // Create the event
        CreateEventOutputBoundary presenter = new ConsolePresenter();
        CreateEventInputBoundary createEventUseCase = new CreateEventInteractor(calendarGateway, presenter);

        System.out.println("\n📅 Creating event...");
        createEventUseCase.execute(request);

        System.out.println("\n✅ Event created successfully in your Google Calendar!");
        System.out.print("\n Press Enter to continue...");
        scanner.nextLine();
    }

    private static void displayParsedRequest(EventRequest request) {
        System.out.println("┌─────────────────────────────────────────────┐");
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

        System.out.println("└─────────────────────────────────────────────┘");
    }
}
