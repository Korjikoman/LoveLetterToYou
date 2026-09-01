package com.example.myproject.Services;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import com.example.myproject.Component.OutboxBackoff;
import com.example.myproject.DTO.ProcessingOutboxEvent;
import com.example.myproject.Events.OutboxStatus;
import com.example.myproject.Model.OutboxEvent;
import com.example.myproject.Repositories.OutboxEventRepository;

import jakarta.transaction.Transactional;

@Service
public class OutboxLeaseService {
    private final Clock clock;
    private final OutboxEventRepository outboxEventRepository;
    private static final int MAX_ATTEMPTS = 20;
    private static final int MAX_LOCKED_TIME = 20000; // ms

    
    OutboxLeaseService(Clock clock, OutboxEventRepository outboxEventRepository) {
        this.clock = clock;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public List<ProcessingOutboxEvent> claimBatch(int batchSize) {
        Instant now =  clock.instant();
        List<OutboxEvent> events = outboxEventRepository.lockAvailable(now, batchSize);

        List<ProcessingOutboxEvent> result = new ArrayList<>();
        for (OutboxEvent event : events) {
            if (event.getAttempts() >= MAX_ATTEMPTS) {
                event.dead(now);
                continue;
            }

            UUID token = UUID.randomUUID();
            
            event.claim(
                token, now.plus(MAX_LOCKED_TIME, ChronoUnit.MILLIS)
            );

            result.add(new ProcessingOutboxEvent(
                event.getId(),
                token,
                event.getEvent_type(),
                event.getAggregate_id(),
                event.getAggregate_email(),
                event.getAttempts()
            ));

        }

        return result;
    }

    @Modifying
    @Transactional
    public int ack(ProcessingOutboxEvent event) {
        int updated = outboxEventRepository.setEventProcessed(clock.instant(), event.id(), event.claimToken());
        if (updated != 1) {
            return 0;
        }
        return outboxEventRepository.deleteProcessed(event.id(), event.claimToken()); // удаляем PROCESSED события, чтобы они не захламляли бд
    }

    @Transactional
    public int retryOrDead(ProcessingOutboxEvent event, Exception exception) {
        OutboxEvent outboxEvent = outboxEventRepository.findById(event.id()).orElse(null);

        if (outboxEvent == null) {
            return 0;
        }

        if (event.attempt() < MAX_ATTEMPTS) {
            outboxEvent.setStatus(OutboxStatus.PENDING);
            Instant nextAttempt = clock.instant().plus(OutboxBackoff.calculate(event.attempt()));


            outboxEvent.setNext_attempt_at(nextAttempt);
            outboxEvent.setClaim_token(null);
            outboxEvent.setLocked_until(null);
            outboxEvent.setLast_error(null);
        } else {
            outboxEvent.setStatus(OutboxStatus.DEAD);
            outboxEvent.setDead_at(clock.instant());

            return outboxEventRepository.deleteProcessed(event.id(), event.claimToken());
            
        }

        return 1;
    }
    
}
