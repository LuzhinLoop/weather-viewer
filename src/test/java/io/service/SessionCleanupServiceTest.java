package io.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionCleanupServiceTest {

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private SessionCleanupService cleanupService;

    @Test
    void should_LogStartAndFinish_when_CleanupRuns() {
        when(sessionService.cleanUpExpiredSession()).thenReturn(7);

        cleanupService.cleanup();

        verify(sessionService, times(1)).cleanUpExpiredSession();
    }

}
