package com.example.myproject.Security;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestRateLimitFilter extends OncePerRequestFilter {
    private static final String POST_METHOD = "POST";
    private static final String IMAGE_UPLOAD_PATH = "/api/images";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String TOO_MANY_REQUESTS_BODY =
        "{\"error\":\"Too many requests\"}";
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final ConcurrentMap<ClientAction, Window> windows =
        new ConcurrentHashMap<>();
    private final AtomicLong nextCleanupNanos = new AtomicLong();
    private final Limit uploadLimit;
    private final long idleEntryTtlNanos;
    private final long cleanupIntervalNanos;

    public RequestRateLimitFilter(
        @Value("${app.rate-limit.upload.max-attempts}") int uploadMaxAttempts,
        @Value("${app.rate-limit.upload.window}") Duration uploadWindow,
        @Value("${app.rate-limit.idle-entry-ttl}") Duration idleEntryTtl,
        @Value("${app.rate-limit.cleanup-interval}") Duration cleanupInterval
    ) {
        uploadLimit = new Limit(Action.UPLOAD, uploadMaxAttempts, uploadWindow);
        idleEntryTtlNanos = idleEntryTtl.toNanos();
        cleanupIntervalNanos = cleanupInterval.toNanos();
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Limit limit = resolveLimit(request);
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.nanoTime();
        cleanupExpiredEntries(now);
        ClientAction key = new ClientAction(clientId(request), limit.action());
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now));
        long retryAfterSeconds = window.acquire(now, limit);

        if (retryAfterSeconds == 0L) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(JSON_CONTENT_TYPE);
        response.setHeader(
            HttpHeaders.RETRY_AFTER,
            Long.toString(retryAfterSeconds)
        );
        response.getWriter().write(TOO_MANY_REQUESTS_BODY);
    }

    private Limit resolveLimit(HttpServletRequest request) {
        if (!POST_METHOD.equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        return switch (request.getRequestURI()) {
            case IMAGE_UPLOAD_PATH -> uploadLimit;
            default -> null;
        };
    }

    private String clientId(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private void cleanupExpiredEntries(long now) {
        long scheduled = nextCleanupNanos.get();
        if (now < scheduled
            || !nextCleanupNanos.compareAndSet(
                scheduled,
                now + cleanupIntervalNanos
            )) {
            return;
        }

        windows.entrySet().removeIf(
            entry -> now - entry.getValue().lastSeenNanos()
                >= idleEntryTtlNanos
        );
    }

    private enum Action {
        UPLOAD
    }

    private record ClientAction(String clientId, Action action) {}

    private record Limit(Action action, int maxAttempts, Duration window) {
        private Limit {
            if (maxAttempts <= 0 || window == null || !window.isPositive()) {
                throw new IllegalArgumentException("Invalid rate limit");
            }
        }
    }

    private static final class Window {
        private long startedNanos;
        private long lastSeenNanos;
        private int attempts;

        private Window(long now) {
            startedNanos = now;
            lastSeenNanos = now;
        }

        private synchronized long acquire(long now, Limit limit) {
            lastSeenNanos = now;
            long windowNanos = limit.window().toNanos();
            if (now - startedNanos >= windowNanos) {
                startedNanos = now;
                attempts = 0;
            }
            if (attempts < limit.maxAttempts()) {
                attempts++;
                return 0L;
            }

            long remainingNanos = windowNanos - (now - startedNanos);
            return Math.max(
                1L,
                (remainingNanos + NANOS_PER_SECOND - 1L)
                    / NANOS_PER_SECOND
            );
        }

        private synchronized long lastSeenNanos() {
            return lastSeenNanos;
        }
    }
}
