package com.example.myproject.Outbox.Events;

public record LetterCacheInvalidationEvent(
    String publicToken,
    String email,
    long version
) {}
