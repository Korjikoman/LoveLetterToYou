package com.example.myproject.Images.Exception;

public class ImageQuotaExceededException extends RuntimeException {

    public ImageQuotaExceededException(String message) {
        super(message);
    }
}