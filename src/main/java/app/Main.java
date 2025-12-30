package app;

import entity.ActionType;
import entity.EventRequest;
import Framework.GoogleCalendarGateway;
import interface_adapter.presenter.ConsolePresenter;
import usecase.CalendarGateway;
import usecase.create.CreateEventInputBoundary;
import usecase.create.CreateEventInteractor;
import usecase.create.CreateEventOutputBoundary;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Scanner;

/**
 * Main application entry point.
 * Tests the Create Event use case.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("🚀 Smart Calendar Assistant - Create Event Test\n");

        // Initialize dependencies
        CalendarGateway calendarGateway = new GoogleCalendarGateway();
        CreateEventOutputBoundary presenter = new ConsolePresenter();
        CreateEventInputBoundary createEventUseCase = new CreateEventInteractor(calendarGateway, presenter);

        // Check if calendar is available
        if (!calendarGateway.isAvailable()) {
            System.out.println("❌ Google Calendar is not available. Please check your credentials.");
            return;
        }

        Scanner scanner = new Scanner(System.in);

        // Get event details from user
        System.out.print("Enter event title: ");
        String title = scanner.nextLine();

        System.out.print("Enter event date (YYYY-MM-DD): ");
        String dateStr = scanner.nextLine();
        LocalDate date = LocalDate.parse(dateStr);

        System.out.print("Enter start time (HH:MM, 24-hour format): ");
        String startStr = scanner.nextLine();
        LocalTime startTime = LocalTime.parse(startStr);

        System.out.print("Enter end time (HH:MM, 24-hour format): ");
        String endStr = scanner.nextLine();
        LocalTime endTime = LocalTime.parse(endStr);

        System.out.print("Enter location (optional, press Enter to skip): ");
        String location = scanner.nextLine();
        if (location.isEmpty()) {
            location = null;
        }

        // Create event request
        EventRequest request = new EventRequest();
        request.setActionType(ActionType.CREATE);
        request.setTitle(title);
        request.setDate(date);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setLocation(location);
        request.setSuccessful(true);

        // Execute use case
        System.out.println("\n📝 Creating event...");
        createEventUseCase.execute(request);

        scanner.close();
    }
}