package com.example.myproject.FileStorage.Exception;

import java.io.IOException;

public class StorageCapacityException extends IOException {
    public StorageCapacityException(String message) {
        super(message);
    }
}