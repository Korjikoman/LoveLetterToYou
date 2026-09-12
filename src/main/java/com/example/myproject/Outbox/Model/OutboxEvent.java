package com.example.myproject.Outbox.Model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.example.myproject.Images.Model.Image;
import com.example.myproject.Outbox.Events.OutboxEventType;
import com.example.myproject.Outbox.Events.OutboxStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "outbox_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {
    @Id
    private UUID id;

    @Column(name = "claim_token")
    private UUID claimToken;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "image_id")
    private UUID imageId;

    @Column(name = "temp_rel_path", length = 512)
    private String tempRelPath;

    @Column(name = "main_rel_path", length = 512)
    private String mainRelPath;

    @Column(name = "expected_sha256", length = 64)
    private String expectedSha256;

    @Column(name = "aggregate_type", length = 50, updatable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 255, updatable = false)
    private String aggregateId;

    @Column(name = "aggregate_version")
    private Long aggregateVersion;

    @Column(name = "aggregate_email", length = 255, updatable = false)
    private String aggregateEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50, nullable = false, updatable = false)
    private OutboxEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "dead_at")
    private Instant deadAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    public static OutboxEvent evictLetter(
        String publicToken,
        String email,
        long version,
        Instant now
    ) {
        OutboxEvent event = create(OutboxEventType.LETTER_CACHE_EVICT, now);
        event.aggregateType = "LETTER";
        event.aggregateId = Objects.requireNonNull(publicToken);
        event.aggregateEmail = Objects.requireNonNull(email);
        event.aggregateVersion = version;
        return event;
    }

    public static OutboxEvent promoteImage(Image image, Instant now) {
        OutboxEvent event = imageEvent(OutboxEventType.IMAGE_PROMOTE, image, now);
        event.expectedSha256 = Objects.requireNonNull(image.getSha256());
        return event;
    }

    public static OutboxEvent deleteImage(
        Image image,
        Instant now,
        Instant executeAt
    ) {
        OutboxEvent event = imageEvent(OutboxEventType.IMAGE_DELETE, image, now);
        event.nextAttemptAt = Objects.requireNonNull(executeAt);
        return event;
    }

    public static OutboxEvent deleteImage(
        UUID imageId,
        String tempRelPath,
        String mainRelPath,
        Instant now
    ) {
        OutboxEvent event = create(OutboxEventType.IMAGE_DELETE, now);
        event.imageId = Objects.requireNonNull(imageId);
        event.tempRelPath = tempRelPath;
        event.mainRelPath = mainRelPath;
        return event;
    }

    private static OutboxEvent imageEvent(
        OutboxEventType type,
        Image image,
        Instant now
    ) {
        OutboxEvent event = create(type, now);
        event.imageId = image.getId();
        event.tempRelPath = image.getTempRelPath();
        event.mainRelPath = image.getMainRelPath();
        return event;
    }

    private static OutboxEvent create(OutboxEventType type, Instant now) {
        OutboxEvent event = new OutboxEvent();
        event.id = UUID.randomUUID();
        event.eventType = Objects.requireNonNull(type);
        event.status = OutboxStatus.PENDING;
        event.createdAt = Objects.requireNonNull(now);
        event.nextAttemptAt = now;
        return event;
    }

    public void claim(UUID token, Instant deadline) {
        status = OutboxStatus.PROCESSING;
        claimToken = Objects.requireNonNull(token);
        lockedUntil = Objects.requireNonNull(deadline);
        attempts++;
    }

    public boolean isOwnedBy(UUID token) {
        return status == OutboxStatus.PROCESSING
            && Objects.equals(claimToken, token);
    }

    public void eventProcessed(Instant now) {
        status = OutboxStatus.PROCESSED;
        processedAt = Objects.requireNonNull(now);
        lastError = null;
        lockedUntil = null;
        claimToken = null;
    }

    public void dead(Instant now) {
        status = OutboxStatus.DEAD;
        deadAt = Objects.requireNonNull(now);
        lockedUntil = null;
        claimToken = null;
    }
}
