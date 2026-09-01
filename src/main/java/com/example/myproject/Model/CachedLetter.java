package com.example.myproject.Model;

import java.time.Instant;
import java.util.List;

import com.example.myproject.DTO.FontData;

public record CachedLetter(
    String publicToken,
    String securityKey,
    String authorEmail,
    String authorName,
    String title,
    String text,
    String password,
    Instant expiresAt,
    Long version,
    Boolean burnAfterOpening,
    List<String> imagesPaths,
    List<String> reactions,
    FontData fontSettings
) {}