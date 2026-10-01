package com.example.myproject.Images.DTO;

import java.util.UUID;

public record ImageView(
    UUID id,
    String contentUrl,
    int position
) {}