package com.example.myproject.WebSocket;

import java.util.concurrent.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebSocketExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    ExecutorService webSocketSendExecutor() {
        return new ThreadPoolExecutor(
            8,
            16,
            30,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1000),
            Thread.ofPlatform().name("ws-send-", 0).factory(),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }

}