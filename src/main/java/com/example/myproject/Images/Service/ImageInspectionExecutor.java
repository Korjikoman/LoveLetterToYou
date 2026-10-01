package com.example.myproject.Images.Service;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.example.myproject.Images.Component.ImageInspector;
import com.example.myproject.Images.DTO.InspectedImage;
import com.example.myproject.Images.Exception.ImageProcessingBusyException;
import com.example.myproject.Images.Exception.ImageProcessingTimeoutException;

@Component
public class ImageInspectionExecutor {
    private final ImageInspector inspector;
    private final ExecutorService executor;
    private final Duration timeout;

    public ImageInspectionExecutor(
        ImageInspector inspector,
        @Qualifier("imageInspectionThreadPool") ExecutorService executor,
        @Value("${app.images.inspection-timeout:PT20S}") Duration timeout
    ) {
        this.inspector = inspector;
        this.executor = executor;
        this.timeout = timeout;
    }

    public InspectedImage inspect(Resource resource) {
        final Future<InspectedImage> future;

        try {
            future = executor.submit(() -> inspector.inspect(resource));
        } catch (RejectedExecutionException exception) {
            throw new ImageProcessingBusyException(
                "Image processing queue is full",
                exception
            );
        }

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new ImageProcessingTimeoutException(
                "Image processing timed out",
                exception
            );
        } catch (InterruptedException exception) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new ImageProcessingTimeoutException(
                "Image processing interrupted",
                exception
            );
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Cannot inspect image", cause);
        }
    }
}