package com.example.myproject.Images.DTO;

import java.util.UUID;

public record ImageFileData(
    UUID id,
    String mainRelPath,
    String contentType,
    long sizeBytes,
    String sha256
) {}
