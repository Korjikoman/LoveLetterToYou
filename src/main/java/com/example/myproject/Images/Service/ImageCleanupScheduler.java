package com.example.myproject.Images.Service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ImageCleanupScheduler {
    private final ImageTransactionService transactions;

    public ImageCleanupScheduler(ImageTransactionService transactions) {
        this.transactions = transactions;
    }

    @Scheduled(fixedDelayString = "${app.images.cleanup-delay-ms:60000}")
    public void cleanup() {
        // Прикреплённые изображения сюда намеренно не входят.
        transactions.queueExpired(100);
    }
}
