package com.example.myproject.Images.Service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ImageCleanupScheduler {
    private final ImageTransactionService transactions;
    private int batchSize;
    private Duration maxDuration;



    public ImageCleanupScheduler(ImageTransactionService transactions,@Value("${app.images.cleanup-batch-size:100}") int batchSize, @Value("${app.images.cleanup-max-duration:PT10S}") Duration maxDuration ) {
        this.transactions = transactions;
        this.batchSize = batchSize;
        this.maxDuration = maxDuration;
    }

    @Scheduled(fixedDelayString = "${app.images.cleanup-delay-ms:60000}")
    public void cleanup() {
        long deadline = System.nanoTime() + maxDuration.toNanos();

        while (System.nanoTime() < deadline) {
            int processed = transactions.queueExpired(batchSize);

            if (processed < batchSize) {
                return;
            }
        }
    }
}
