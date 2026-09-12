package com.example.myproject.Images.DTO;

import java.util.UUID;

// используем в письмах и профиле вместо сущности Image
public record ImageView(
    UUID id,
    String contentUrl,
    int position
) {}