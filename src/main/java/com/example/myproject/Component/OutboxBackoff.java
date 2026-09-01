package com.example.myproject.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class OutboxBackoff {

    // рассчитывем время следующей попытки
    public static Duration calculate(int failedAttempt) {
        
        // максимально кол-во попыток = 8
        int limit = Math.min(
            Math.max(failedAttempt - 1, 0),
            8
        );

        // при сдвиге влево мы делаем 2^limit
        long baseSeconds = Math.min(
            300,
            1L << limit
        );

        long half = baseSeconds / 2;


        // adding jitter
        long seconds = ThreadLocalRandom.current().nextLong(baseSeconds - half + 1);
        return Duration.ofSeconds(seconds);
    }
}
