package com.example.myproject.WebSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;



import jakarta.annotation.PreDestroy;
import tools.jackson.databind.ObjectMapper;
@Component
public class SocketConnectionHandler extends TextWebSocketHandler {
    private final ConcurrentMap<String, ClientConnection> connections =
        new ConcurrentHashMap<>();
    private static final Logger log =
        LoggerFactory.getLogger(SocketConnectionHandler.class);
    private final AtomicBoolean broadcastScheduled = new AtomicBoolean();

    private final PresenceService presence;
    private final ObjectMapper objectMapper;
    private final ExecutorService sendExecutor;
    private final WebSocketBroadcastScheduler scheduler;

    public SocketConnectionHandler(
        PresenceService presence,
        ObjectMapper objectMapper,
        ExecutorService webSocketSendExecutor,
        WebSocketBroadcastScheduler webSocketBroadcastScheduler
    ) {
        this.presence = presence;
        this.objectMapper = objectMapper;
        this.sendExecutor = webSocketSendExecutor;
        this.scheduler = webSocketBroadcastScheduler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
        throws Exception {

        String email = String.valueOf(session.getAttributes().get("email"));

        if (email.isBlank() || "null".equals(email)) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        WebSocketSession safeSession =
            new ConcurrentWebSocketSessionDecorator(
                session,
                5_000,
                64 * 1024
            );

        ClientConnection connection = new ClientConnection(
            email,
            safeSession,
            sendExecutor,
            () -> removeConnection(session.getId())
        );

        connections.put(session.getId(), connection);
        presence.heartbeatOnline(email);
        scheduleBroadcast();
    }

    @Override
    protected void handleTextMessage(
        WebSocketSession session,
        TextMessage message
    ) {
        ClientConnection connection = connections.get(session.getId());
        if (connection == null) return;

        switch (message.getPayload()) {
            case "heartbeat" ->
                presence.heartbeatOnline(connection.email());

            case "writing" ->
                presence.heartbeatWriting(connection.email());

            default -> {
                return;
            }
        }

        scheduleBroadcast();
    }

    @Override
    public void afterConnectionClosed(
        WebSocketSession session,
        CloseStatus status
    ) {
        removeConnection(session.getId());
    }

    @Override
    public void handleTransportError(
        WebSocketSession session,
        Throwable exception
    ) {
        removeConnection(session.getId());
    }

    private void removeConnection(String sessionId) {
        ClientConnection connection = connections.remove(sessionId);

        if (connection != null) {
            connection.close();
            scheduleBroadcast();
        }
    }

    private void scheduleBroadcast() {
        if (!broadcastScheduled.compareAndSet(false, true)) return;

        scheduler.schedule(() -> {
            broadcastScheduled.set(false);
            broadcast();
        }, 200, TimeUnit.MILLISECONDS);
    }

    private void broadcast() {
        try {
            PresenceService.Snapshot snapshot = presence.snapshot();

            String json = objectMapper.writeValueAsString(Map.of(
                "usersOnline", snapshot.usersOnline(),
                "usersWritingLetter", snapshot.usersWritingLetter()
            ));

            TextMessage message = new TextMessage(json);
            connections.values().forEach(c -> c.offer(message));
        } catch (Exception exception) {
            log.error("Failed to broadcast WebSocket presence update", exception);
        }
    }

    @PreDestroy
    public void shutdown() {
        connections.values().forEach(ClientConnection::close);
        connections.clear();
    }
}