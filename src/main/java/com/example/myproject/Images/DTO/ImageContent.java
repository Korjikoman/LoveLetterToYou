package com.example.myproject.Images.DTO;

import org.springframework.core.io.Resource;


// передаем контроллеру проверенный файл
public record ImageContent(
    Resource resource,
    String contentType,
    long contentLength,
    String filename
) {}
