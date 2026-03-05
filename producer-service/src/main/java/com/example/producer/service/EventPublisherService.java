package com.example.producer.service;

import com.example.producer.config.RabbitMQConfig;
import com.example.producer.model.UserActivityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);

    private final AmqpTemplate amqpTemplate;

    public EventPublisherService(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    public void publish(UserActivityEvent event) {
        try {
            amqpTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    event
            );
            log.info("Published event for user_id={} event_type={}", event.getUserId(), event.getEventType());
        } catch (AmqpException e) {
            log.error("Failed to publish event for user_id={}: {}", event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }
}
