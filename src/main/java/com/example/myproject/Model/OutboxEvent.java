package com.example.myproject.Model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Id;

import com.example.myproject.Events.OutboxEventType;
import com.example.myproject.Events.OutboxStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "outbox_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {
    @Id
    private UUID id;

    @Column(name="claim_token")
    private UUID claim_token;
    
    @Column(name = "locked_until")
    private Instant locked_until;

    @Column(name="aggregate_type", length = 50, nullable=false, updatable = false)
    private String aggregate_type;

    @Column(name="aggregate_id",length = 255,  nullable=false, updatable = false)
    private String aggregate_id;

    @Column(name="aggregate_version",nullable=false)
    private Long aggregate_version;

    @Column(name="aggregate_email",length = 255,  nullable=false, updatable = false)
    private String aggregate_email;

    @Enumerated(EnumType.STRING)
    @Column(name="event_type", length = 50, nullable = false, updatable = false)
    private OutboxEventType event_type;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false,length = 50)
    private OutboxStatus status;
    
    @Column(nullable = false)
    private Integer attempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant created_at;
    @Column(name = "next_attempt_at", nullable = false)
    private Instant next_attempt_at;
    @Column(name = "processed_at")
    private Instant processed_at;
    @Column(name = "dead_at")
    private Instant dead_at;

    @Column(name="last_error", length = 2000)
    private String last_error;

    @Column(name= "paths_to_delete")
    private List<String> paths_to_delete;

    @Column(name = "old_images_paths")
    private List<String> old_images_paths;

    @Column(name = "new_images")
    private List<String> new_images_paths;

    public static OutboxEvent evictLetter(
        String publicToken,
        String email,
        Long version,
        Instant now
    ) {
        OutboxEvent event = new OutboxEvent();
        event.id = UUID.randomUUID();
        event.claim_token = UUID.randomUUID();
        event.aggregate_type = "LETTER";
        event.aggregate_email = email;
        event.aggregate_id = publicToken;
        event.event_type = OutboxEventType.LETTER_CACHE_EVICT;
        event.status = OutboxStatus.PENDING;
        event.attempts  = 0;
        event.created_at = now;
        event.next_attempt_at = now;
        event.aggregate_version = version;
        return event;
    }

    public static OutboxEvent deleteImages(String publicToken, String email, List<String> pathsToRemove, Instant now, Long version) {
        OutboxEvent event = new OutboxEvent();
        event.id = UUID.randomUUID();
        event.claim_token = UUID.randomUUID();
        event.aggregate_type = "IMAGES";
        event.aggregate_email = email;
        event.aggregate_id = publicToken;
        event.event_type = OutboxEventType.DELETE_LETTER_IMAGES;
        event.status = OutboxStatus.PENDING;
        
        event.paths_to_delete = pathsToRemove;

        event.attempts  = 0;
        event.created_at = now;
        event.next_attempt_at = now;
        event.aggregate_version = version;
        return event;
    }

    public static OutboxEvent updateImages(String publicToken,  String email, List<String> paths_to_delete, List<String> oldImagesPaths, List<String> newImagesPaths, Instant now, Long version) {
        OutboxEvent event = new OutboxEvent();
        event.id = UUID.randomUUID();
        event.claim_token = UUID.randomUUID();
        event.aggregate_type = "IMAGES";
        event.aggregate_email = email;
        event.aggregate_id = publicToken;
        event.event_type = OutboxEventType.UPDATE_LETTER_IMAGES;
        event.status = OutboxStatus.PENDING;
        
        event.paths_to_delete = paths_to_delete;
        event.old_images_paths = oldImagesPaths;
        event.new_images_paths = newImagesPaths;

        event.attempts  = 0;
        event.created_at = now;
        event.next_attempt_at = now;
        event.aggregate_version = version;
        return event;
    }

    public void eventProcessed(
        Instant now
    ) {
        status = OutboxStatus.PROCESSED;
        processed_at = now;
        last_error = null;
    }

    public void eventFailed(
        Instant now, Duration retry_delay, Integer maxAttempts,
        Throwable exception
    ) {
        attempts++;
        status = OutboxStatus.PENDING;
        
        String except = exception.getMessage();
        if (except == null) {
            except = exception.getClass().getSimpleName();
        }

        last_error = except.substring(0, Math.min(except.length(), 2000));

        if (attempts >= maxAttempts) {
            status = OutboxStatus.DEAD;
            dead_at = now;
            return;
        }

        next_attempt_at = now.plus( retry_delay);

    }
    
    public void dead(Instant deadTime) {
        status = OutboxStatus.DEAD;
        dead_at = deadTime;
        return;
    }

    public void claim(UUID token, Instant timeLimit) {
        status = OutboxStatus.PROCESSING;
        claim_token = token;
        locked_until = timeLimit;
        attempts++; 
        return;
    }

}   
