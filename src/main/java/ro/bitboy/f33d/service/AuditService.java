package ro.bitboy.f33d.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AuditService {

    private static final int MAX_AUDIT = 100;

    private final List<FailedAttempt> attempts = new CopyOnWriteArrayList<>();

    public void recordFailedAttempt(String ip, String path, String userAgent) {
        attempts.add(0, new FailedAttempt(ip, Instant.now(), path, userAgent != null ? userAgent : ""));
        if (attempts.size() > MAX_AUDIT) {
            attempts.subList(MAX_AUDIT, attempts.size()).clear();
        }
    }

    public List<FailedAttempt> getRecentAttempts() {
        return List.copyOf(attempts);
    }

    public record FailedAttempt(String ip, Instant timestamp, String path, String userAgent) {}
}
