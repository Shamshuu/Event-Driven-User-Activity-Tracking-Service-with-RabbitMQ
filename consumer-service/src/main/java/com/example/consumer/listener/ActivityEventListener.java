package com.example.consumer.listener;

import com.example.consumer.config.RabbitMQConfig;
import com.example.consumer.model.UserActivityEvent;
import com.example.consumer.service.ActivityStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ActivityEventListener {

    private static final Logger log = LoggerFactory.getLogger(ActivityEventListener.class);

    private final ActivityStorageService storageService;

    public ActivityEventListener(ActivityStorageService storageService) {
        this.storageService = storageService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleMessage(UserActivityEvent event) {
        log.info("Received message from queue: user_id={} event_type={}",
                event.getUserId(), event.getEventType());

        try {
            validateEvent(event);
            storageService.store(event);
            log.info("Successfully processed event for user_id={}", event.getUserId());
        } catch (IllegalArgumentException e) {
            log.error("Invalid event received, rejecting: {}", e.getMessage());
            throw new RuntimeException("Invalid event: " + e.getMessage(), e);
        } catch (Exception e) {
            // Unexpected error (e.g., DB down) — log and reject (goes to DLQ)
            log.error("Failed to process event for user_id={}: {}",
                    event != null ? event.getUserId() : "null", e.getMessage(), e);
            throw new RuntimeException("Processing failed: " + e.getMessage(), e);
        }
    }

    private void validateEvent(UserActivityEvent event) {
        if (event.getUserId() == null) {
            throw new IllegalArgumentException("user_id is required");
        }
        if (event.getEventType() == null || event.getEventType().isBlank()) {
            throw new IllegalArgumentException("event_type is required");
        }
        if (event.getTimestamp() == null || event.getTimestamp().isBlank()) {
            throw new IllegalArgumentException("timestamp is required");
        }
    }
}
