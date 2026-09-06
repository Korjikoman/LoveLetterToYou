package com.example.myproject.DTO;

import java.time.Instant;

public record LetterSummaryView(
    String publicToken,
    String title,
    String text,
    long version,
    Instant expiresAt
) {}
