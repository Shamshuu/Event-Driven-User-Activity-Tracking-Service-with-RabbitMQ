package com.example.producer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class UserActivityEvent {

    @NotNull(message = "user_id is required")
    @JsonProperty("user_id")
    private Integer userId;

    @NotBlank(message = "event_type is required")
    @Size(max = 50, message = "event_type must be at most 50 characters")
    @JsonProperty("event_type")
    private String eventType;

    @NotBlank(message = "timestamp is required")
    @Pattern(
        regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})$",
        message = "timestamp must be in ISO 8601 format (e.g., 2023-10-27T10:00:00Z)"
    )
    private String timestamp;

    private Map<String, Object> metadata;

    public UserActivityEvent() {
    }

    public UserActivityEvent(Integer userId, String eventType, String timestamp, Map<String, Object> metadata) {
        this.userId = userId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
