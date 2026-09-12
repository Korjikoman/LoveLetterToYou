package com.example.myproject.Images.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.myproject.DTO.ProcessingOutboxEvent;
import com.example.myproject.Images.DTO.ImageStatus;
import com.example.myproject.Images.Model.Image;
import com.example.myproject.Images.Repository.ImageRepository;
import com.example.myproject.Outbox.Events.OutboxEventType;
import com.example.myproject.Outbox.Model.OutboxEvent;
import com.example.myproject.Outbox.Repository.OutboxEventRepository;

@Service
public class ImageOutboxFinalizer {
    private final OutboxEventRepository outboxRepository;
    private final ImageRepository imageRepository;
    private final Clock clock;
    private final Duration readyTimeout;

    public ImageOutboxFinalizer(
        OutboxEventRepository outboxRepository,
        ImageRepository imageRepository,
        Clock clock,
        @Value("${app.images.ready-timeout:PT1H}") Duration readyTimeout
    ) {
        this.outboxRepository = outboxRepository;
        this.imageRepository = imageRepository;
        this.clock = clock;
        this.readyTimeout = readyTimeout;
    }

    /** Завершает перенос файла и обработку события одной транзакцией. */
    @Transactional
    public boolean finishPromotion(ProcessingOutboxEvent claimed) {
        OutboxEvent event = requireClaim(claimed, OutboxEventType.IMAGE_PROMOTE);
        if (event == null) {
            return false;
        }

        Image image = imageRepository.findForUpdate(claimed.imageId()).orElse(null);
        Instant now = clock.instant();
        if (image == null) {
            outboxRepository.save(OutboxEvent.deleteImage(
                claimed.imageId(), claimed.tempRelPath(), claimed.mainRelPath(), now
            ));
            event.eventProcessed(now);
            return true;
        }

        requireSameFile(image, claimed);
        switch (image.getStatus()) {
            case STAGED -> image.markReady(now, now.plus(readyTimeout));
            case DELETE_AFTER_PROMOTION -> {
                image.markDeletePendingAfterPromotion();
                outboxRepository.save(OutboxEvent.deleteImage(image, now, now));
            }
            case READY, ATTACHED -> {
                // Повтор после уже завершённого переноса безопасен.
            }
            default -> throw new IllegalStateException(
                "Cannot finish promotion from " + image.getStatus()
            );
        }

        event.eventProcessed(now);
        return true;
    }

    /** Завершает физическое и логическое удаление одной транзакцией. */
    @Transactional
    public boolean finishDeletion(ProcessingOutboxEvent claimed) {
        OutboxEvent event = requireClaim(claimed, OutboxEventType.IMAGE_DELETE);
        if (event == null) {
            return false;
        }

        Image image = imageRepository.findForUpdate(claimed.imageId()).orElse(null);
        Instant now = clock.instant();
        if (image != null) {
            image.markDeleted(now);
        }
        event.eventProcessed(now);
        return true;
    }

    private OutboxEvent requireClaim(
        ProcessingOutboxEvent claimed,
        OutboxEventType expectedType
    ) {
        OutboxEvent event = outboxRepository.findForUpdate(claimed.id()).orElse(null);
        if (event == null || !event.isOwnedBy(claimed.claimToken())) {
            return null;
        }
        if (event.getEventType() != expectedType
            || !Objects.equals(event.getImageId(), claimed.imageId())
            || !Objects.equals(event.getTempRelPath(), claimed.tempRelPath())
            || !Objects.equals(event.getMainRelPath(), claimed.mainRelPath())
            || !Objects.equals(event.getExpectedSha256(), claimed.expectedSha256())) {
            throw new IllegalStateException("Outbox event data changed");
        }
        return event;
    }

    private void requireSameFile(Image image, ProcessingOutboxEvent claimed) {
        if (!Objects.equals(image.getTempRelPath(), claimed.tempRelPath())
            || !Objects.equals(image.getMainRelPath(), claimed.mainRelPath())
            || !Objects.equals(image.getSha256(), claimed.expectedSha256())) {
            throw new IllegalStateException("Outbox file does not match Image");
        }
    }
}
