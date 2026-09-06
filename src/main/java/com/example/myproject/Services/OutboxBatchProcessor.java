package com.example.myproject.Services;

import java.time.Clock;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import com.example.myproject.Component.OutboxBackoff;
import com.example.myproject.DTO.ProcessingOutboxEvent;
import com.example.myproject.Model.OutboxEvent;
import com.example.myproject.Repositories.OutboxEventRepository;
import com.example.myproject.Repositories.RedisRepository;

import jakarta.transaction.Transactional;

@Service
public class OutboxBatchProcessor {
    private static final int BATCH_SIZE = 20;
    private static final int MAX_ATTEMPTS = 20;

    private final OutboxEventRepository outboxEventRepository;
    private final RedisRepository redisRepository;
    private final OutboxBackoff outboxBackoff;
    private final OutboxLeaseService leaseService;
    private final Clock clock;
    private final ImageStorage imageStorage;

    public OutboxBatchProcessor(OutboxEventRepository outboxEventRepository, RedisRepository redisRepository, OutboxBackoff outboxBackoff, OutboxLeaseService leaseService, ImageStorage imageStorage, Clock clock) {
        this.clock = clock;
        this.outboxBackoff = outboxBackoff;
        this.redisRepository = redisRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.leaseService = leaseService;
        this.imageStorage = imageStorage;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-delay-ms:500}")
    public void processBatch() {
        List<ProcessingOutboxEvent> events = leaseService.claimBatch(BATCH_SIZE); // транзакция

        for (var event : events) {
            try {
                if (processEvent(event)) {  // не транзакция. Удаление из Redis-кеша
                    leaseService.ack(event); // транзакция
                }
            } catch (Exception ex) {
                leaseService.retryOrDead(event, ex); // транзакция
            }
            
        }
    }

    private Boolean processEvent(ProcessingOutboxEvent event) {
        switch (event.eventType()) {
            case LETTER_CACHE_EVICT: {
                return redisRepository.evictLetter(event.aggregateId(), event.aggregateEmail(), event.aggregateVersion());
            }
            case UPDATE_LETTER_IMAGES: {
                return imageStorage.replaceFromTempToPersistent(event.aggregateId(), event.aggregateTempImagesPaths(),event.aggregateMainImagesPaths()) && imageStorage.deletePaths(event.aggregateId(), event.aggregatePathsToDelete());
            }
            case DELETE_LETTER_IMAGES : {
                return imageStorage.deletePaths(event.aggregateId(), event.aggregatePathsToDelete());
            }

            default:
                return false;
        }
    }
}
