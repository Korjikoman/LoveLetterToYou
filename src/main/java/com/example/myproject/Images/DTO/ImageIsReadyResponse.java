package com.example.myproject.Images.DTO;

import java.util.UUID;

public record ImageIsReadyResponse(
    UUID imageId,
    ImageStatus status,
    String contentUrl
) {}
