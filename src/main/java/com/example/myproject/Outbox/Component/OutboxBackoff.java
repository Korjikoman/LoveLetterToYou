package com.example.myproject.Outbox.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class OutboxBackoff {

    public static Duration calculate(int failedAttempt) {
        int limit = Math.min(
            Math.max(failedAttempt - 1, 0),
            8
        );

        long baseSeconds = Math.min(
            300,
            1L << limit
        );

        long half = baseSeconds / 2;

        // Случайный разброс не даёт всем обработчикам повторять работу одновременно.
        long seconds = ThreadLocalRandom.current().nextLong(half, baseSeconds + 1);
        return Duration.ofSeconds(seconds);
    }
}
