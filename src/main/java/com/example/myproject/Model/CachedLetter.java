package com.example.myproject.Model;

import java.time.Instant;
import java.util.List;

import com.example.myproject.DTO.FontData;
import com.example.myproject.Images.DTO.ImageView;

public record CachedLetter(
    String publicToken,
    String securityKey,
    String authorEmail,
    String authorName,
    String title,
    String text,
    Instant expiresAt,
    Long version,
    Boolean burnAfterOpening,
    List<ImageView> images,
    List<String> reactions,
    FontData fontSettings
) {
    public CachedLetter {
        images = images == null ? List.of() : List.copyOf(images);
        reactions = reactions == null ? List.of() : List.copyOf(reactions);
    }
}
