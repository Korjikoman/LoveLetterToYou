package com.example.myproject.DTO;

import java.util.UUID;

import com.example.myproject.Outbox.Events.OutboxEventType;

public record ProcessingOutboxEvent(
    UUID id,
    UUID claimToken,
    OutboxEventType eventType,
    String aggregateId,
    UUID imageId,
    Long aggregateVersion,
    String aggregateEmail,
    String tempRelPath,
    String mainRelPath,
    String expectedSha256,
    int attempts
) {}
