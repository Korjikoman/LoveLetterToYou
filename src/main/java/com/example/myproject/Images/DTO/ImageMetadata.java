package com.example.myproject.Images.DTO;

import java.time.Instant;
import java.util.UUID;


public record ImageMetadata(
    UUID imageId,
    String tempRelPath,
    String mainRelPath,
    Instant uploadExpiresAt
) {
    
}
