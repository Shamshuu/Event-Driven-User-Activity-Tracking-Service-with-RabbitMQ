package com.example.consumer.controller;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@RestController
public class HealthController {

    private final ConnectionFactory rabbitConnectionFactory;
    private final DataSource dataSource;

    public HealthController(ConnectionFactory rabbitConnectionFactory, DataSource dataSource) {
        this.rabbitConnectionFactory = rabbitConnectionFactory;
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean rabbitHealthy = checkRabbitMQ();
        boolean mysqlHealthy = checkMySQL();

        if (rabbitHealthy && mysqlHealthy) {
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "rabbitmq", "connected",
                    "mysql", "connected"
            ));
        } else {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "rabbitmq", rabbitHealthy ? "connected" : "disconnected",
                    "mysql", mysqlHealthy ? "connected" : "disconnected"
            ));
        }
    }

    private boolean checkRabbitMQ() {
        try {
            rabbitConnectionFactory.createConnection().close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkMySQL() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(3);
        } catch (Exception e) {
            return false;
        }
    }
}
