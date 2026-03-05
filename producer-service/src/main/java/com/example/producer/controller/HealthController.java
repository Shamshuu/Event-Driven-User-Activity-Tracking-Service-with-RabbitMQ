package com.example.producer.controller;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final ConnectionFactory connectionFactory;

    public HealthController(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean rabbitHealthy = checkRabbitMQ();

        if (rabbitHealthy) {
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "rabbitmq", "connected"
            ));
        } else {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "rabbitmq", "disconnected"
            ));
        }
    }

    private boolean checkRabbitMQ() {
        try {
            connectionFactory.createConnection().close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
