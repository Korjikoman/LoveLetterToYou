package com.example.myproject.DTO;

import java.time.Instant;

public record LetterSummaryView(
    String publicToken,
    long version,
    Instant expiresAt
) {}
