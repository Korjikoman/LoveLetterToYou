package com.example.myproject.Services;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class LetterDeliveryStateException extends RuntimeException {
    public LetterDeliveryStateException() {
        super("Letter delivery state conflict");
    }

    LetterDeliveryStateException(Throwable cause) {
        super("Letter delivery state conflict", cause);
    }
}
