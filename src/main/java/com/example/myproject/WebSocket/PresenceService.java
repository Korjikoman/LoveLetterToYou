package com.example.myproject.WebSocket;

import java.time.Clock;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {
    private static final String ONLINE = "presence:online-users";
    private static final String WRITING = "presence:writing-users";
    private static final long TIMEOUT_MS = 60_000;

    private final RedisTemplate<String, Object> redis;
    private final Clock clock;

    public PresenceService(RedisTemplate<String, Object> redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    public void heartbeatOnline(String email) {
        touch(ONLINE, email);
    }

    public void heartbeatWriting(String email) {
        touch(ONLINE, email);
        touch(WRITING, email);
    }

    public Snapshot snapshot() {
        long cutoff = clock.millis() - TIMEOUT_MS;

        redis.opsForZSet().removeRangeByScore(ONLINE, 0, cutoff);
        redis.opsForZSet().removeRangeByScore(WRITING, 0, cutoff);

        Long online = redis.opsForZSet().zCard(ONLINE);
        Long writing = redis.opsForZSet().zCard(WRITING);

        return new Snapshot(
            online == null ? 0 : online,
            writing == null ? 0 : writing
        );
    }

    private void touch(String key, String email) {
        if (email != null && !email.isBlank()) {
            redis.opsForZSet().add(key, email, clock.millis());
        }
    }

    public record Snapshot(long usersOnline, long usersWritingLetter) {}
}
