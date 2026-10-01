package com.example.myproject.Images.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageProcessingConfiguration {

    @Bean(name = "imageInspectionThreadPool", destroyMethod = "shutdown")
    ExecutorService imageInspectionThreadPool(
        @Value("${app.images.inspection-workers:2}") int workers,
        @Value("${app.images.inspection-queue-capacity:8}") int queueCapacity
    ) {
        return new ThreadPoolExecutor(
            workers,
            workers,
            0,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueCapacity),
            Thread.ofPlatform().name("image-inspection-", 0).factory(),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }
}