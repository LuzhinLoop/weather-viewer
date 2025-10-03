package io.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionCleanupService {

    private final SessionService sessionService;

    @Scheduled(fixedRate = 360000)
    public void cleanup() {
        log.info("Starting expired sessions cleanup...");
        int deletedCount = sessionService.cleanUpExpiredSession();
        log.info("Finished expired sessions cleanup. Deleted sessions: {}", deletedCount);
    }
}
