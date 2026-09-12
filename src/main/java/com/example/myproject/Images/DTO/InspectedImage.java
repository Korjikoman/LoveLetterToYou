package com.example.myproject.Images.DTO;

// тут резельтат проверки содержимого изображения
public record InspectedImage(
    String contentType,
    int width,
    int height
) {}
