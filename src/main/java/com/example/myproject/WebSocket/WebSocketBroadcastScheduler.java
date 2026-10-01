package com.example.myproject.WebSocket;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class WebSocketBroadcastScheduler {
    private final ScheduledExecutorService executor =
        Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("ws-broadcast-", 0).factory()
        );

    public void schedule(Runnable task, long delay, TimeUnit unit) {
        executor.schedule(task, delay, unit);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}