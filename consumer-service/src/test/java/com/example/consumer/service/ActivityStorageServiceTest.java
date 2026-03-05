package com.example.consumer.service;

import com.example.consumer.model.UserActivity;
import com.example.consumer.model.UserActivityEvent;
import com.example.consumer.repository.UserActivityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityStorageServiceTest {

    @Mock
    private UserActivityRepository repository;

    private ActivityStorageService storageService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        storageService = new ActivityStorageService(repository, objectMapper);
    }

    @Test
    void store_validEvent_savesToRepository() {
        UserActivityEvent event = new UserActivityEvent(
                123, "page_view", "2023-10-27T10:00:00Z",
                Map.of("page_url", "/products/item-xyz")
        );

        UserActivity savedEntity = new UserActivity(
                123, "page_view", LocalDateTime.of(2023, 10, 27, 10, 0, 0),
                "{\"page_url\":\"/products/item-xyz\"}"
        );
        savedEntity.setId(1);
        when(repository.save(any(UserActivity.class))).thenReturn(savedEntity);

        UserActivity result = storageService.store(event);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(123, result.getUserId());

        ArgumentCaptor<UserActivity> captor = ArgumentCaptor.forClass(UserActivity.class);
        verify(repository, times(1)).save(captor.capture());

        UserActivity captured = captor.getValue();
        assertEquals(123, captured.getUserId());
        assertEquals("page_view", captured.getEventType());
        assertEquals(LocalDateTime.of(2023, 10, 27, 10, 0, 0), captured.getTimestamp());
        assertNotNull(captured.getMetadata());
    }

    @Test
    void store_eventWithNoMetadata_savesNullMetadata() {
        UserActivityEvent event = new UserActivityEvent(456, "logout", "2023-10-27T15:30:00Z", null);

        UserActivity savedEntity = new UserActivity(456, "logout",
                LocalDateTime.of(2023, 10, 27, 15, 30, 0), null);
        savedEntity.setId(2);
        when(repository.save(any(UserActivity.class))).thenReturn(savedEntity);

        UserActivity result = storageService.store(event);

        assertNotNull(result);
        ArgumentCaptor<UserActivity> captor = ArgumentCaptor.forClass(UserActivity.class);
        verify(repository).save(captor.capture());
        assertNull(captor.getValue().getMetadata());
    }

    @Test
    void store_eventWithOffsetTimestamp_parsesCorrectly() {
        UserActivityEvent event = new UserActivityEvent(
                789, "login", "2023-10-27T10:00:00+05:30", null
        );

        UserActivity savedEntity = new UserActivity(789, "login",
                LocalDateTime.of(2023, 10, 27, 10, 0, 0), null);
        savedEntity.setId(3);
        when(repository.save(any(UserActivity.class))).thenReturn(savedEntity);

        UserActivity result = storageService.store(event);
        assertNotNull(result);
        verify(repository, times(1)).save(any(UserActivity.class));
    }

    @Test
    void store_invalidTimestamp_throwsException() {
        UserActivityEvent event = new UserActivityEvent(
                123, "login", "not-a-timestamp", null
        );

        assertThrows(IllegalArgumentException.class, () -> storageService.store(event));
        verify(repository, never()).save(any());
    }

    @Test
    void store_databaseError_throwsException() {
        UserActivityEvent event = new UserActivityEvent(
                123, "page_view", "2023-10-27T10:00:00Z", null
        );

        when(repository.save(any(UserActivity.class))).thenThrow(new RuntimeException("DB unavailable"));

        assertThrows(RuntimeException.class, () -> storageService.store(event));
    }
}
