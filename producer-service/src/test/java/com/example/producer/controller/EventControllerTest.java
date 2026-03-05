package com.example.producer.controller;

import com.example.producer.exception.GlobalExceptionHandler;
import com.example.producer.model.UserActivityEvent;
import com.example.producer.service.EventPublisherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
 
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EventPublisherService publisherService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        EventController controller = new EventController(publisherService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void trackEvent_validPayload_returns202() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "user_id", 123,
                "event_type", "page_view",
                "timestamp", "2023-10-27T10:00:00Z",
                "metadata", Map.of("page_url", "/products/item-xyz", "session_id", "abc123")
        ));

        mockMvc.perform(post("/api/v1/events/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.message").value("Event published successfully"))
                .andExpect(jsonPath("$.user_id").value(123))
                .andExpect(jsonPath("$.event_type").value("page_view"));

        verify(publisherService, times(1)).publish(any());
    }

    @Test
    void trackEvent_missingUserId_returns400() throws Exception {
        String payload = """
                {
                    "event_type": "login",
                    "timestamp": "2023-10-27T10:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/events/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(publisherService, never()).publish(any());
    }

    @Test
    void trackEvent_missingEventType_returns400() throws Exception {
        String payload = """
                {
                    "user_id": 123,
                    "timestamp": "2023-10-27T10:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/events/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(publisherService, never()).publish(any());
    }

    @Test
    void trackEvent_missingTimestamp_returns400() throws Exception {
        String payload = """
                {
                    "user_id": 123,
                    "event_type": "login"
                }
                """;

        mockMvc.perform(post("/api/v1/events/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(publisherService, never()).publish(any());
    }

    @Test
    void trackEvent_invalidTimestampFormat_returns400() throws Exception {
        String payload = """
                {
                    "user_id": 123,
                    "event_type": "login",
                    "timestamp": "not-a-timestamp"
                }
                """;

        mockMvc.perform(post("/api/v1/events/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(publisherService, never()).publish(any());
    }

    @Test
    void trackEvent_malformedJson_returns400() throws Exception {
        String payload = "{ this is not valid json }";

        mockMvc.perform(post("/api/v1/events/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(publisherService, never()).publish(any());
    }

    @Test
    void trackEvent_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/events/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(publisherService, never()).publish(any());
    }

    @Test
    void trackEvent_validPayloadWithNullMetadata_returns202() throws Exception {
        String payload = """
                {
                    "user_id": 456,
                    "event_type": "logout",
                    "timestamp": "2023-10-27T15:30:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/events/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"));

        verify(publisherService, times(1)).publish(any(UserActivityEvent.class));
    }

    @Test
    void trackEvent_eventTypeTooLong_returns400() throws Exception {
        String longEventType = "a".repeat(51);
        String payload = String.format("""
                {
                    "user_id": 123,
                    "event_type": "%s",
                    "timestamp": "2023-10-27T10:00:00Z"
                }
                """, longEventType);

        mockMvc.perform(post("/api/v1/events/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(publisherService, never()).publish(any());
    }
}
