package api.dto;

import entity.EventResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EventResponseDTO {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("errorCode")
    private String errorCode;

    public EventResponseDTO() {
    }

    public static EventResponseDTO fromEventResponse(EventResponse response) {
        EventResponseDTO dto = new EventResponseDTO();
        dto.success = response.isSuccess();
        dto.message = response.getMessage();
        dto.errorCode = response.getErrorCode();
        return dto;
    }

    public static EventResponseDTO error(String message) {
        EventResponseDTO dto = new EventResponseDTO();
        dto.success = false;
        dto.message = message;
        dto.errorCode = "ERROR";
        return dto;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}