package com.example.myproject.Images.DTO;

import org.springframework.core.io.Resource;


public record ImageContent(
    Resource resource,
    String contentType,
    long contentLength,
    String filename
) {}
