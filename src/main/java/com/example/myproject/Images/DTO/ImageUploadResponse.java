package com.example.myproject.Images.DTO;

import java.time.Instant;
import java.util.UUID;

public record ImageUploadResponse(
    UUID imageId,
    ImageStatus status,
    Instant uploadExpiresAt
) {}
