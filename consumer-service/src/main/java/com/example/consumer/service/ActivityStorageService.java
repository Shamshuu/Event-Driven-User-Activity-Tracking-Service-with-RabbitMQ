package com.example.consumer.service;

import com.example.consumer.model.UserActivity;
import com.example.consumer.model.UserActivityEvent;
import com.example.consumer.repository.UserActivityRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Service
public class ActivityStorageService {

    private static final Logger log = LoggerFactory.getLogger(ActivityStorageService.class);

    private final UserActivityRepository repository;
    private final ObjectMapper objectMapper;

    public ActivityStorageService(UserActivityRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UserActivity store(UserActivityEvent event) {
        log.info("Storing event for user_id={} event_type={}", event.getUserId(), event.getEventType());

        LocalDateTime eventTimestamp = parseTimestamp(event.getTimestamp());
        String metadataJson = serializeMetadata(event);

        UserActivity activity = new UserActivity(
                event.getUserId(),
                event.getEventType(),
                eventTimestamp,
                metadataJson
        );

        UserActivity saved = repository.save(activity);
        log.info("Stored activity id={} for user_id={}", saved.getId(), saved.getUserId());
        return saved;
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            OffsetDateTime odt = OffsetDateTime.parse(timestamp);
            return odt.toLocalDateTime();
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(timestamp);
            } catch (DateTimeParseException e2) {
                log.error("Cannot parse timestamp '{}': {}", timestamp, e2.getMessage());
                throw new IllegalArgumentException("Invalid timestamp format: " + timestamp, e2);
            }
        }
    }

    private String serializeMetadata(UserActivityEvent event) {
        if (event.getMetadata() == null || event.getMetadata().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(event.getMetadata());
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata for user_id={}: {}", event.getUserId(), e.getMessage());
            return null;
        }
    }
}
