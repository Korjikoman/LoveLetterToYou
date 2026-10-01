package com.example.myproject.Images.Exception;

public class ImageProcessingBusyException extends RuntimeException {
    public ImageProcessingBusyException(String message, Throwable cause) {
        super(message, cause);
    }
}