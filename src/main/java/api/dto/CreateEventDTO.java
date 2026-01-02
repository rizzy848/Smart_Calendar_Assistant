package api.dto;

import entity.ActionType;
import entity.EventRequest;
import java.time.LocalDate;
import java.time.LocalTime;

public class CreateEventDTO {
    private String title;
    private String date;
    private String startTime;
    private String endTime;
    private String location;

    public EventRequest toEventRequest() {
        EventRequest request = new EventRequest();
        request.setActionType(ActionType.CREATE);
        request.setTitle(this.title);
        request.setDate(LocalDate.parse(this.date));
        request.setStartTime(LocalTime.parse(this.startTime));
        if (this.endTime != null && !this.endTime.isEmpty()) {
            request.setEndTime(LocalTime.parse(this.endTime));
        }
        request.setLocation(this.location);
        request.setSuccessful(true);
        return request;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
