package com.example.producer.controller;

import com.example.producer.model.UserActivityEvent;
import com.example.producer.service.EventPublisherService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final EventPublisherService publisherService;

    public EventController(EventPublisherService publisherService) {
        this.publisherService = publisherService;
    }

    @PostMapping("/track")
    public ResponseEntity<Map<String, Object>> trackEvent(@Valid @RequestBody UserActivityEvent event) {
        log.info("Received track request for user_id={} event_type={}", event.getUserId(), event.getEventType());
        publisherService.publish(event);

        Map<String, Object> response = Map.of(
                "status", "accepted",
                "message", "Event published successfully",
                "user_id", event.getUserId(),
                "event_type", event.getEventType()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
