package com.example.myproject.DTO;

import java.util.List;
import java.util.UUID;

import com.example.myproject.Events.OutboxEventType;

public record ProcessingOutboxEvent (
    UUID id,
    UUID claimToken,
    OutboxEventType eventType,  // LETTER_CACHE_EVICT
    String aggregateId,   // ID письма
    Long aggregateVersion, // версия письма
    String aggregateEmail, // Email того, кто запрашивает письмо
    List<String> aggregatePathsToDelete, // пути к записям, которые нужно удалить
    List<String> aggregateTempImagesPaths, // temp path, где хранятся письма до их переноса в main
    List<String> aggregateMainImagesPaths, // main path
    int attempt

){}
