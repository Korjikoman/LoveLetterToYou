package com.example.myproject.DTO;

import java.time.Instant;
import java.util.List;

import com.example.myproject.Model.Image;

public record LetterView (
    String publicToken,
    String authorName,
    String title,
    String text,
    Instant expiresAt,
    Boolean burnAfterOpening,
    List<Image> images,
    List<String> reactions,
    FontData font
) {
    public LetterView {
        images = images == null ? List.of() : List.copyOf(images);
        reactions = reactions == null ? List.of() : List.copyOf(reactions);
    }
    
}
