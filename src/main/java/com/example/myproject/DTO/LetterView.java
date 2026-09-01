package com.example.myproject.DTO;

import java.time.Instant;
import java.util.List;

public record LetterView (
    String publicToken,
    String authorName,
    String title,
    String text,
    Instant expiresAt,
    Boolean burnAfterOpening,
    Boolean passwordProtected,
    List<String> imagesPaths,
    List<String> reactions,
    FontData font
) {
    public LetterView {
        imagesPaths = imagesPaths == null ? List.of() : List.copyOf(imagesPaths);
        reactions = reactions == null ? List.of() : List.copyOf(reactions);
    }
    
}
