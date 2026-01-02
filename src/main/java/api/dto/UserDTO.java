package api.dto;

import entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDTO {

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("authenticated")
    private boolean authenticated;

    public UserDTO() {
    }

    public static UserDTO fromUser(User user) {
        UserDTO dto = new UserDTO();
        dto.userId = user.getUserId();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        dto.authenticated = user.isAuthenticated();
        return dto;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
