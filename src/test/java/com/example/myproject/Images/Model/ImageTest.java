package com.example.myproject.Images.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.myproject.Images.DTO.ImagePurpose;
import com.example.myproject.Images.DTO.ImageStatus;
import com.example.myproject.Model.MyAppUser;

class ImageTest {

    private static final String CHECKSUM = "a".repeat(64);

    @Test
    void followsUploadingStagedReadyAttachedLifecycle() {
        UUID id = UUID.randomUUID();
        MyAppUser owner = new MyAppUser();
        Instant createdAt = Instant.parse("2026-09-13T05:00:00Z");
        Instant uploadExpiresAt = createdAt.plus(15, ChronoUnit.MINUTES);
        Image image = Image.upload(
            id,
            owner,
            ImagePurpose.LETTER,
            "staged/letter.upload",
            "ready/letter.png",
            createdAt,
            uploadExpiresAt
        );

        assertEquals(id, image.getId());
        assertSame(owner, image.getUser());
        assertEquals(ImagePurpose.LETTER, image.getImagePurpose());
        assertEquals(ImageStatus.UPLOADING, image.getStatus());
        assertEquals("staged/letter.upload", image.getTempRelPath());
        assertEquals("ready/letter.png", image.getMainRelPath());
        assertEquals(createdAt, image.getCreatedAt());
        assertEquals(uploadExpiresAt, image.getUploadExpiresAt());

        image.markStaged("image/png", 1_024L, 640, 480, CHECKSUM);

        assertEquals(ImageStatus.STAGED, image.getStatus());
        assertEquals("image/png", image.getContentType());
        assertEquals(1_024L, image.getSizeBytes());
        assertEquals(640, image.getWidth());
        assertEquals(480, image.getHeight());
        assertEquals("png", image.getExtension());
        assertEquals(CHECKSUM, image.getSha256());
        assertNull(image.getUploadExpiresAt());

        Instant readyAt = createdAt.plus(1, ChronoUnit.MINUTES);
        Instant readyExpiresAt = readyAt.plus(1, ChronoUnit.HOURS);
        image.markReady(readyAt, readyExpiresAt);

        assertEquals(ImageStatus.READY, image.getStatus());
        assertEquals(readyAt, image.getReadyAt());
        assertEquals(readyExpiresAt, image.getReadyExpiresAt());
        assertNull(image.getTempRelPath());

        image.markAttached();

        assertEquals(ImageStatus.ATTACHED, image.getStatus());
        assertNull(image.getReadyExpiresAt());
    }

    @Test
    void stagedDeletionWaitsForPromotionBeforeBecomingPending() {
        Image image = uploadingImage();
        image.markStaged("image/jpeg", 512L, 320, 200, CHECKSUM);

        assertFalse(image.requestDeletion());
        assertEquals(ImageStatus.DELETE_AFTER_PROMOTION, image.getStatus());
        assertEquals("staged/avatar.upload", image.getTempRelPath());

        image.markDeletePendingAfterPromotion();

        assertEquals(ImageStatus.DELETE_PENDING, image.getStatus());
        assertNull(image.getTempRelPath());

        Instant deletedAt = Instant.parse("2026-09-13T06:00:00Z");
        image.markDeleted(deletedAt);

        assertEquals(ImageStatus.DELETED, image.getStatus());
        assertEquals(deletedAt, image.getDeletedAt());
        assertFalse(image.requestDeletion());
    }

    @Test
    void attachedImageCanMoveDirectlyToPendingAndDeleted() {
        Image image = uploadingImage();
        Instant readyAt = Instant.parse("2026-09-13T05:01:00Z");
        image.markStaged("image/jpeg", 512L, 320, 200, CHECKSUM);
        image.markReady(readyAt, readyAt.plus(1, ChronoUnit.HOURS));
        image.markAttached();

        assertTrue(image.requestDeletion());
        assertEquals(ImageStatus.DELETE_PENDING, image.getStatus());

        Instant deletedAt = Instant.parse("2026-09-13T06:00:00Z");
        image.markDeleted(deletedAt);

        assertEquals(ImageStatus.DELETED, image.getStatus());
        assertEquals(deletedAt, image.getDeletedAt());
    }

    private static Image uploadingImage() {
        Instant createdAt = Instant.parse("2026-09-13T05:00:00Z");
        return Image.upload(
            UUID.randomUUID(),
            new MyAppUser(),
            ImagePurpose.AVATAR,
            "staged/avatar.upload",
            "ready/avatar.jpg",
            createdAt,
            createdAt.plus(15, ChronoUnit.MINUTES)
        );
    }
}
