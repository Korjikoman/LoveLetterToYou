package com.example.myproject.Outbox.Service;

import java.io.IOException;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.myproject.DTO.ProcessingOutboxEvent;
import com.example.myproject.FileStorage.FileStorage;
import com.example.myproject.Images.Service.ImageOutboxFinalizer;
import com.example.myproject.Repositories.RedisRepository;

@Service
public class OutboxBatchProcessor {
    private static final int BATCH_SIZE = 20;

    private final RedisRepository redisRepository;
    private final OutboxLeaseService leaseService;
    private final FileStorage fileStorage;
    private final ImageOutboxFinalizer imageFinalizer;

    public OutboxBatchProcessor(
        RedisRepository redisRepository,
        OutboxLeaseService leaseService,
        FileStorage fileStorage,
        ImageOutboxFinalizer imageFinalizer
    ) {
        this.redisRepository = redisRepository;
        this.leaseService = leaseService;
        this.fileStorage = fileStorage;
        this.imageFinalizer = imageFinalizer;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:500}")
    public void processBatch() {
        // Каждое событие получает полный срок аренды перед своей обработкой.
        for (int i = 0; i < BATCH_SIZE; i++) {
            List<ProcessingOutboxEvent> claimed = leaseService.claimBatch(1);
            if (claimed.isEmpty()) {
                return;
            }

            ProcessingOutboxEvent event = claimed.getFirst();
            try {
                processEvent(event);
            } catch (Exception exception) {
                leaseService.retryOrDead(event, exception);
            }
        }
    }

    private void processEvent(ProcessingOutboxEvent event) throws IOException {
        switch (event.eventType()) {
            case LETTER_CACHE_EVICT -> {
                redisRepository.evictLetter(
                    event.aggregateId(),
                    event.aggregateEmail(),
                    event.aggregateVersion()
                );
                leaseService.ack(event);
            }
            case IMAGE_PROMOTE -> {
                fileStorage.replace(
                    event.tempRelPath(),
                    event.mainRelPath(),
                    event.expectedSha256()
                );
                imageFinalizer.finishPromotion(event);
            }
            case IMAGE_DELETE -> {
                fileStorage.deleteFile(event.mainRelPath());
                fileStorage.deleteFile(event.tempRelPath());
                imageFinalizer.finishDeletion(event);
            }
        }
    }
}
