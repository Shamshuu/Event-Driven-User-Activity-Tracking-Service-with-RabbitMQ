package com.example.consumer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class UserActivityEvent {

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("event_type")
    private String eventType;

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
