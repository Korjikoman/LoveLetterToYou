package com.example.myproject.Images.Exception;

public class ImageProcessingTimeoutException extends RuntimeException {
    public ImageProcessingTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}