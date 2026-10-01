package com.example.myproject.Controller;

import java.io.UncheckedIOException;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.myproject.Images.Exception.ImageProcessingBusyException;
import com.example.myproject.Images.Exception.ImageProcessingTimeoutException;
import com.example.myproject.Images.Exception.ImageQuotaExceededException;

@RestControllerAdvice(assignableTypes = {
    ImageApiController.class,
    LetterController.class,
    UsersProfileController.class,
    RegistrationController.class
})
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalid(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> notFound(NoSuchElementException exception) {
        return response(HttpStatus.NOT_FOUND, "Ресурс не найден");
    }

    @ExceptionHandler({
        IllegalStateException.class,
        ObjectOptimisticLockingFailureException.class
    })
    public ResponseEntity<ApiError> conflict(RuntimeException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<ApiError> storageUnavailable(
        UncheckedIOException exception
    ) {
        return response(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Хранилище файлов временно недоступно"
        );
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message) {
        String safeMessage = message == null || message.isBlank()
            ? status.getReasonPhrase()
            : message;

        return ResponseEntity.status(status).body(new ApiError(safeMessage));
    }

    @ExceptionHandler(ImageProcessingBusyException.class)
    public ResponseEntity<ApiError> imageQueueFull(
        ImageProcessingBusyException exception
    ) {
        return response(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage());
    }

    @ExceptionHandler(ImageProcessingTimeoutException.class)
    public ResponseEntity<ApiError> imageTimeout(
        ImageProcessingTimeoutException exception
    ) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @ExceptionHandler(ImageQuotaExceededException.class)
    public ResponseEntity<ApiError> quotaExceeded(
        ImageQuotaExceededException exception
    ) {
        return ResponseEntity.status(507)
            .body(new ApiError(exception.getMessage()));
    }

    public record ApiError(String error) {}
}
