package com.example.consumer.listener;

import com.example.consumer.model.UserActivity;
import com.example.consumer.model.UserActivityEvent;
import com.example.consumer.service.ActivityStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityEventListenerTest {

    @Mock
    private ActivityStorageService storageService;

    @InjectMocks
    private ActivityEventListener listener;

    @Test
    void handleMessage_validEvent_storesSuccessfully() {
        UserActivityEvent event = new UserActivityEvent(
                123, "page_view", "2023-10-27T10:00:00Z",
                Map.of("page_url", "/home")
        );

        UserActivity saved = new UserActivity(123, "page_view",
                LocalDateTime.of(2023, 10, 27, 10, 0, 0), null);
        saved.setId(1);
        when(storageService.store(any(UserActivityEvent.class))).thenReturn(saved);

        assertDoesNotThrow(() -> listener.handleMessage(event));
        verify(storageService, times(1)).store(event);
    }

    @Test
    void handleMessage_nullUserId_throwsException() {
        UserActivityEvent event = new UserActivityEvent(null, "login", "2023-10-27T10:00:00Z", null);

        assertThrows(RuntimeException.class, () -> listener.handleMessage(event));
        verify(storageService, never()).store(any());
    }

    @Test
    void handleMessage_emptyEventType_throwsException() {
        UserActivityEvent event = new UserActivityEvent(123, "", "2023-10-27T10:00:00Z", null);

        assertThrows(RuntimeException.class, () -> listener.handleMessage(event));
        verify(storageService, never()).store(any());
    }

    @Test
    void handleMessage_nullTimestamp_throwsException() {
        UserActivityEvent event = new UserActivityEvent(123, "login", null, null);

        assertThrows(RuntimeException.class, () -> listener.handleMessage(event));
        verify(storageService, never()).store(any());
    }

    @Test
    void handleMessage_storageFailure_throwsException() {
        UserActivityEvent event = new UserActivityEvent(
                123, "login", "2023-10-27T10:00:00Z", null
        );

        when(storageService.store(any())).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> listener.handleMessage(event));
        verify(storageService, times(1)).store(event);
    }
}
