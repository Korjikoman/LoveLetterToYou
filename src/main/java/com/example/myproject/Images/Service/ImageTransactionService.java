package com.example.myproject.Images.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.myproject.FileStorage.StoredFileInfo;
import com.example.myproject.Images.DTO.ImageFileData;
import com.example.myproject.Images.DTO.ImageIsReadyResponse;
import com.example.myproject.Images.DTO.ImageMetadata;
import com.example.myproject.Images.DTO.ImagePurpose;
import com.example.myproject.Images.DTO.ImageStatus;
import com.example.myproject.Images.DTO.InspectedImage;
import com.example.myproject.Images.Model.Image;
import com.example.myproject.Images.Repository.ImageRepository;
import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Outbox.Model.OutboxEvent;
import com.example.myproject.Outbox.Repository.OutboxEventRepository;
import com.example.myproject.Repositories.MyAppUserRepository;

@Service
public class ImageTransactionService {
    private final MyAppUserRepository userRepository;
    private final OutboxEventRepository outboxRepository;
    private final Duration uploadTimeout;
    private final Clock clock;
    private final ImageRepository imageRepository;

    public ImageTransactionService(
        MyAppUserRepository userRepository,
        Clock clock,
        ImageRepository imageRepository,
        @Value("${app.images.upload-timeout:PT15M}") Duration uploadTimeout,
        OutboxEventRepository outboxRepository
    ) {
        this.userRepository = userRepository;
        this.imageRepository = imageRepository;
        this.clock = clock;
        this.uploadTimeout = uploadTimeout;
        this.outboxRepository = outboxRepository;
    }

    /** Создаёт короткую запись о начавшейся загрузке. */
    @Transactional
    public ImageMetadata registerUpload(String email, ImagePurpose purpose) {
        MyAppUser user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UUID imageId = UUID.randomUUID();
        Instant now = clock.instant();
        Instant expiresAt = now.plus(uploadTimeout);
        String mainPath = "images/" + imageId;
        String tempPath = "temp/images/" + imageId;

        Image image = Image.upload(
            imageId, user, purpose, tempPath, mainPath, now, expiresAt
        );
        imageRepository.save(image);
        return new ImageMetadata(imageId, tempPath, mainPath, expiresAt);
    }

    /** Фиксирует проверенный файл и атомарно создаёт событие переноса. */
    @Transactional
    public void markStagedAndQueuePromotion(
        UUID imageId,
        String email,
        StoredFileInfo stored,
        InspectedImage inspected
    ) {
        Image image = imageRepository.findOwnedForUpdate(imageId, email)
            .orElseThrow(() -> new NoSuchElementException("Image not found"));

        Instant now = clock.instant();
        if (image.getStatus() != ImageStatus.UPLOADING
            || image.getUploadExpiresAt() == null
            || !image.getUploadExpiresAt().isAfter(now)) {
            throw new IllegalStateException("Upload is no longer active");
        }

        image.markStaged(
            inspected.contentType(),
            stored.sizeBytes(),
            inspected.width(),
            inspected.height(),
            stored.sha256()
        );
        outboxRepository.save(OutboxEvent.promoteImage(image, now));
    }

    /** Отменяет только ещё не прикреплённое изображение. */
    @Transactional
    public void cancelUnattached(UUID imageId, String email) {
        Image image = imageRepository.findOwnedForUpdate(imageId, email).orElse(null);
        if (image == null) {
            return;
        }
        if (image.getStatus() == ImageStatus.ATTACHED) {
            throw new IllegalStateException("Attached image cannot be cancelled");
        }

        ImageStatus previous = image.getStatus();
        if (image.requestDeletion()) {
            Instant now = clock.instant();
            Instant executeAt = previous == ImageStatus.UPLOADING
                && image.getUploadExpiresAt() != null
                    ? image.getUploadExpiresAt().plus(Duration.ofMinutes(1))
                    : now;
            outboxRepository.save(OutboxEvent.deleteImage(image, now, executeAt));
        }
    }

    /** Блокирует готовое изображение перед привязкой к объекту. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Image requireReadyForAttachment(
        UUID imageId,
        String email,
        ImagePurpose purpose
    ) {
        Image image = imageRepository.findOwnedForUpdate(imageId, email)
            .orElseThrow(() -> new NoSuchElementException("Image not found"));

        Instant now = clock.instant();
        if (image.getImagePurpose() != purpose
            || image.getStatus() != ImageStatus.READY
            || image.getReadyExpiresAt() == null
            || !image.getReadyExpiresAt().isAfter(now)) {
            throw new IllegalStateException("Image is not ready for attachment");
        }
        return image;
    }

    /** Ставит удаление отвязанного изображения в ту же транзакцию. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void queueAttachedDeletion(UUID imageId, String email) {
        Image image = imageRepository.findOwnedForUpdate(imageId, email)
            .orElseThrow(() -> new NoSuchElementException("Image not found"));
        if (image.getStatus() != ImageStatus.ATTACHED) {
            throw new IllegalStateException("Image is not attached");
        }

        if (image.requestDeletion()) {
            Instant now = clock.instant();
            outboxRepository.save(OutboxEvent.deleteImage(image, now, now));
        }
    }

    /** Находит брошенные загрузки и ставит их удаление в очередь. */
    @Transactional
    public int queueExpired(int limit) {
        Instant now = clock.instant();
        List<Image> expired = imageRepository.lockExpired(now, limit);
        for (Image image : expired) {
            if (image.requestDeletion()) {
                outboxRepository.save(OutboxEvent.deleteImage(image, now, now));
            }
        }
        return expired.size();
    }

    @Transactional(readOnly = true)
    public ImageIsReadyResponse getImageReady(UUID imageId, String email) {
        Image image = findOwned(imageId, email);
        String contentUrl = image.getStatus() == ImageStatus.READY
            || image.getStatus() == ImageStatus.ATTACHED
                ? "/api/images/" + imageId + "/content"
                : null;
        return new ImageIsReadyResponse(imageId, image.getStatus(), contentUrl);
    }

    @Transactional(readOnly = true)
    public ImageFileData getOwnedFile(UUID imageId, String email) {
        Image image = findOwned(imageId, email);
        if (image.getStatus() != ImageStatus.READY
            && image.getStatus() != ImageStatus.ATTACHED) {
            throw new IllegalStateException("Image is not ready");
        }
        if (image.getSizeBytes() == null) {
            throw new IllegalStateException("Image metadata is incomplete");
        }

        return new ImageFileData(
            image.getId(),
            image.getMainRelPath(),
            image.getContentType(),
            image.getSizeBytes(),
            image.getSha256()
        );
    }

    private Image findOwned(UUID imageId, String email) {
        if (imageId == null || email == null || email.isBlank()) {
            throw new IllegalArgumentException("Image not found");
        }
        return imageRepository.findByIdAndUser_EmailIgnoreCase(imageId, email)
            .orElseThrow(() -> new NoSuchElementException("Image not found"));
    }
}
