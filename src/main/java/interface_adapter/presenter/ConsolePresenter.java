package interface_adapter.presenter;

import entity.EventResponse;
import usecase.create.CreateEventOutputBoundary;

/**
 * Simple console presenter that prints results to the terminal.
 */
public class ConsolePresenter implements CreateEventOutputBoundary {

    @Override
    public void presentSuccess(EventResponse response) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("✅ SUCCESS!");
        System.out.println("=".repeat(50));
        System.out.println(response.getMessage());

        if (response.getCreatedEvent() != null) {
            System.out.println("\nEvent Details:");
            System.out.println("  Title: " + response.getCreatedEvent().getTitle());
            System.out.println("  Date: " + response.getCreatedEvent().getDate());
            System.out.println("  Time: " + response.getCreatedEvent().getStartTime()
                    + " - " + response.getCreatedEvent().getEndTime());
            if (response.getCreatedEvent().getLocation() != null) {
                System.out.println("  Location: " + response.getCreatedEvent().getLocation());
            }
        }
        System.out.println("=".repeat(50) + "\n");
    }

    @Override
    public void presentFailure(EventResponse response) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("❌ ERROR!");
        System.out.println("=".repeat(50));
        System.out.println(response.getMessage());
        System.out.println("Error Code: " + response.getErrorCode());
        System.out.println("=".repeat(50) + "\n");
    }
}