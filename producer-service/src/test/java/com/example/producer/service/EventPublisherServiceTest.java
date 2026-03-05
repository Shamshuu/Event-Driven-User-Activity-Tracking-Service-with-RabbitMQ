package com.example.producer.service;

import com.example.producer.config.RabbitMQConfig;
import com.example.producer.model.UserActivityEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventPublisherServiceTest {

    @Mock
    private AmqpTemplate amqpTemplate;

    @InjectMocks
    private EventPublisherService publisherService;

    @Test
    void publish_sendsMessageToCorrectExchangeAndRoutingKey() {
        UserActivityEvent event = new UserActivityEvent(
                123, "login", "2023-10-27T10:00:00Z", Map.of("ip", "1.2.3.4")
        );

        publisherService.publish(event);

        verify(amqpTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY),
                eq(event)
        );
    }

    @Test
    void publish_rabbitMQDown_throwsAmqpException() {
        UserActivityEvent event = new UserActivityEvent(
                456, "page_view", "2023-10-27T11:00:00Z", null
        );

        doThrow(new AmqpException("Connection refused"))
                .when(amqpTemplate)
                .convertAndSend(
                        eq(RabbitMQConfig.EXCHANGE_NAME),
                        eq(RabbitMQConfig.ROUTING_KEY),
                        eq(event)
                );

        assertThrows(AmqpException.class, () -> publisherService.publish(event));
    }

    @Test
    void publish_multipleEvents_allPublished() {
        UserActivityEvent event1 = new UserActivityEvent(1, "login", "2023-10-27T10:00:00Z", null);
        UserActivityEvent event2 = new UserActivityEvent(2, "logout", "2023-10-27T11:00:00Z", null);

        assertDoesNotThrow(() -> {
            publisherService.publish(event1);
            publisherService.publish(event2);
        });

        verify(amqpTemplate, times(2)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY),
                any(UserActivityEvent.class)
        );
    }
}
