package com.example.myproject.Outbox.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.myproject.DTO.ProcessingOutboxEvent;
import com.example.myproject.Outbox.Component.OutboxBackoff;
import com.example.myproject.Outbox.Model.OutboxEvent;
import com.example.myproject.Outbox.Repository.OutboxEventRepository;

@Service
public class OutboxLeaseService {
    private static final int MAX_ATTEMPTS = 20;

    private final Clock clock;
    private final OutboxEventRepository repository;
    private final Duration leaseTime;
    private final Duration retention;

    public OutboxLeaseService(
        Clock clock,
        OutboxEventRepository repository,
        @Value("${app.outbox.lease-time:PT2M}") Duration leaseTime,
        @Value("${app.outbox.retention:P7D}") Duration retention
    ) {
        this.clock = clock;
        this.repository = repository;
        this.leaseTime = leaseTime;
        this.retention = retention;
    }

    /** На короткое время закрепляет события за этим обработчиком. */
    @Transactional
    public List<ProcessingOutboxEvent> claimBatch(int batchSize) {
        Instant now = clock.instant();
        List<OutboxEvent> events = repository.lockAvailable(now, batchSize);
        List<ProcessingOutboxEvent> result = new ArrayList<>(events.size());

        for (OutboxEvent event : events) {
            if (event.getAttempts() >= MAX_ATTEMPTS) {
                event.dead(now);
                continue;
            }

            UUID token = UUID.randomUUID();
            event.claim(token, now.plus(leaseTime));
            result.add(new ProcessingOutboxEvent(
                event.getId(),
                token,
                event.getEventType(),
                event.getAggregateId(),
                event.getImageId(),
                event.getAggregateVersion(),
                event.getAggregateEmail(),
                event.getTempRelPath(),
                event.getMainRelPath(),
                event.getExpectedSha256(),
                event.getAttempts()
            ));
        }
        return result;
    }

    @Transactional
    public boolean ack(ProcessingOutboxEvent event) {
        return repository.setEventProcessed(
            event.id(), event.claimToken(), clock.instant()
        ) == 1;
    }

    @Transactional
    public boolean retryOrDead(ProcessingOutboxEvent event, Exception exception) {
        String error = errorText(exception);
        Instant now = clock.instant();
        if (event.attempts() >= MAX_ATTEMPTS) {
            return repository.markDead(
                event.id(), event.claimToken(), now, error
            ) == 1;
        }

        Instant nextAttempt = now.plus(OutboxBackoff.calculate(event.attempts()));
        return repository.releaseForRetry(
            event.id(), event.claimToken(), nextAttempt, error
        ) == 1;
    }

    @Scheduled(cron = "${app.outbox.cleanup-cron:0 15 * * * *}")
    @Transactional
    public void cleanupProcessed() {
        repository.deleteProcessedBefore(clock.instant().minus(retention), 1000);
    }

    private String errorText(Exception exception) {
        String text = exception.getMessage();
        if (text == null || text.isBlank()) {
            text = exception.getClass().getSimpleName();
        }
        return text.substring(0, Math.min(2000, text.length()));
    }
}
