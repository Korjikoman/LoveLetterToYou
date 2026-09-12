package com.example.myproject.DTO;

import java.time.Instant;
import java.util.List;

import com.example.myproject.Images.DTO.ImageView;

public record LetterView (
    String publicToken,
    long version,
    String authorName,
    String title,
    String text,
    Instant expiresAt,
    Boolean burnAfterOpening,
    List<ImageView> images,
    List<String> reactions,
    FontData font
) {
    public LetterView {
        images = images == null ? List.of() : List.copyOf(images);
        reactions = reactions == null ? List.of() : List.copyOf(reactions);
    }
    
}
