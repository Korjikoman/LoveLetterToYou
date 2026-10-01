package com.example.myproject.WebSocket;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

final class ClientConnection {
    private final String email;
    private final WebSocketSession session;
    private final Executor executor;
    private final Runnable failureHandler;

    private final AtomicReference<TextMessage> pending = new AtomicReference<>();
    private final AtomicBoolean sending = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    ClientConnection(
        String email,
        WebSocketSession session,
        Executor executor,
        Runnable failureHandler
    ) {
        this.email = email;
        this.session = session;
        this.executor = executor;
        this.failureHandler = failureHandler;
    }

    String email() {
        return email;
    }

    void offer(TextMessage message) {
        if (closed.get()) return;

        pending.set(message);
        schedule();
    }

    void close() {
        if (!closed.compareAndSet(false, true)) return;

        pending.set(null);
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception ignored) {
        }
    }

    private void schedule() {
        if (closed.get() || !sending.compareAndSet(false, true)) return;

        try {
            executor.execute(this::drain);
        } catch (RejectedExecutionException exception) {
            sending.set(false);
            failureHandler.run();
        }
    }

    private void drain() {
        try {
            TextMessage message;

            while (!closed.get()
                && (message = pending.getAndSet(null)) != null) {
                session.sendMessage(message);
            }
        } catch (Exception exception) {
            failureHandler.run();
        } finally {
            sending.set(false);

            if (!closed.get() && pending.get() != null) {
                schedule();
            }
        }
    }
}