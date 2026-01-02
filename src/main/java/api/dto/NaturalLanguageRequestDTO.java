package api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NaturalLanguageRequestDTO {

    @JsonProperty("text")
    private String text;

    // Default constructor for Jackson
    public NaturalLanguageRequestDTO() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}