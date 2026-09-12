package com.example.myproject.Images.DTO;

import java.util.UUID;

// сообщаем клиенту, готов ли файл
public record ImageIsReadyResponse(
    UUID imageId,
    ImageStatus status,
    String contentUrl
) {}
