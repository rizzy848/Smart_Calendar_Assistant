package usecase;

/**
 * Custom exception for calendar operations.
 * Wraps any external API errors in a clean way.
 */
public class CalendarException extends Exception {

    private final String errorCode;

    public CalendarException(String message) {
        super(message);
        this.errorCode = "CALENDAR_ERROR";
    }

    public CalendarException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public CalendarException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "CALENDAR_ERROR";
    }

    public CalendarException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}