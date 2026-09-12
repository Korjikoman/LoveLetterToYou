package com.example.myproject.FileStorage;

public record StoredFileInfo (
    long sizeBytes,
    String sha256
) {}
