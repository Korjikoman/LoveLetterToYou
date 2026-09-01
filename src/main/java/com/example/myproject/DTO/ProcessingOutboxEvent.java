package com.example.myproject.DTO;

import java.util.UUID;

import com.example.myproject.Events.OutboxEventType;

public record ProcessingOutboxEvent (
    UUID id,
    UUID claimToken,
    OutboxEventType eventType,  // LETTER_CACHE_EVICT
    String aggregateId,   // ID письма
    String aggregateEmail, // Email того, кто запрашивает письмо
    int attempt
){}
