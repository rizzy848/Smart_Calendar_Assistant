package api.dto;

import entity.EventRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ParsedEventDTO {

    @JsonProperty("actionType")
    private String actionType;

    @JsonProperty("title")
    private String title;

    @JsonProperty("date")
    private String date;

    @JsonProperty("startTime")
    private String startTime;

    @JsonProperty("endTime")
    private String endTime;

    @JsonProperty("location")
    private String location;

    @JsonProperty("successful")
    private boolean successful;

    @JsonProperty("errorMessage")
    private String errorMessage;

    // Default constructor for Jackson
    public ParsedEventDTO() {
    }

    public static ParsedEventDTO fromEventRequest(EventRequest request) {
        ParsedEventDTO dto = new ParsedEventDTO();
        dto.successful = request.isSuccessful();
        if (request.getActionType() != null) {
            dto.actionType = request.getActionType().toString();
        }
        dto.title = request.getTitle();
        dto.date = request.getDate() != null ? request.getDate().toString() : null;
        dto.startTime = request.getStartTime() != null ? request.getStartTime().toString() : null;
        dto.endTime = request.getEndTime() != null ? request.getEndTime().toString() : null;
        dto.location = request.getLocation();
        return dto;
    }

    public static ParsedEventDTO fromError(String error) {
        ParsedEventDTO dto = new ParsedEventDTO();
        dto.successful = false;
        dto.errorMessage = error;
        return dto;
    }

    // Getters and setters
    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}