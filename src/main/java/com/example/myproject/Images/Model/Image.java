package com.example.myproject.Images.Model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.example.myproject.Images.DTO.ImagePurpose;
import com.example.myproject.Images.DTO.ImageStatus;
import com.example.myproject.Model.MyAppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {
    @Id
    private UUID id;

    @Version
    @Column(nullable = false)
    private long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private MyAppUser user;

    @Column(name = "main_rel_path", nullable = false, length = 512)
    private String mainRelPath;

    @Column(name = "temp_rel_path", length = 512)
    private String tempRelPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_purpose", nullable = false, length = 40)
    private ImagePurpose imagePurpose;

    @Column(length = 16)
    private String extension;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ImageStatus status;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    private Integer height;
    private Integer width;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "ready_expires_at")
    private Instant readyExpiresAt;

    @Column(name = "upload_expires_at")
    private Instant uploadExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 64)
    private String sha256;

    public static Image upload(
        UUID id,
        MyAppUser user,
        ImagePurpose purpose,
        String tempRelPath,
        String mainRelPath,
        Instant now,
        Instant uploadExpiresAt
    ) {
        Image image = new Image();
        image.id = Objects.requireNonNull(id, "Image id is required");
        image.user = Objects.requireNonNull(user, "Image owner is required");
        image.imagePurpose = Objects.requireNonNull(purpose, "Image purpose is required");
        image.tempRelPath = requirePath(tempRelPath, "Temporary path is required");
        image.mainRelPath = requirePath(mainRelPath, "Final path is required");
        image.status = ImageStatus.UPLOADING;
        image.createdAt = Objects.requireNonNull(now, "Creation time is required");
        image.uploadExpiresAt = Objects.requireNonNull(uploadExpiresAt, "Upload deadline is required");
        return image;
    }

    public void markStaged(
        String contentType,
        long sizeBytes,
        int width,
        int height,
        String sha256
    ) {
        requireStatus(ImageStatus.UPLOADING);
        if (sizeBytes <= 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid image properties");
        }

        this.contentType = Objects.requireNonNull(contentType, "Content type is required");
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.sha256 = Objects.requireNonNull(sha256, "Checksum is required");
        this.extension = extensionFor(contentType);
        this.status = ImageStatus.STAGED;
        this.uploadExpiresAt = null;
    }

    public void markReady(Instant now, Instant expiresAt) {
        requireStatus(ImageStatus.STAGED);
        status = ImageStatus.READY;
        readyAt = Objects.requireNonNull(now, "Ready time is required");
        readyExpiresAt = Objects.requireNonNull(expiresAt, "Ready deadline is required");
        tempRelPath = null;
    }

    public void markAttached() {
        requireStatus(ImageStatus.READY);
        status = ImageStatus.ATTACHED;
        readyExpiresAt = null;
    }

    /** Возвращает true, если файл уже можно удалить. */
    public boolean requestDeletion() {
        return switch (status) {
            case UPLOADING, READY, ATTACHED, FAILED -> {
                status = ImageStatus.DELETE_PENDING;
                yield true;
            }
            case STAGED -> {
                status = ImageStatus.DELETE_AFTER_PROMOTION;
                yield false;
            }
            case DELETE_AFTER_PROMOTION, DELETE_PENDING, DELETED -> false;
        };
    }

    public void markDeletePendingAfterPromotion() {
        requireStatus(ImageStatus.DELETE_AFTER_PROMOTION);
        status = ImageStatus.DELETE_PENDING;
        tempRelPath = null;
    }

    public void markDeleted(Instant now) {
        if (status != ImageStatus.DELETE_PENDING && status != ImageStatus.DELETED) {
            throw new IllegalStateException("Image is not scheduled for deletion");
        }
        status = ImageStatus.DELETED;
        deletedAt = Objects.requireNonNull(now, "Deletion time is required");
        tempRelPath = null;
    }

    private void requireStatus(ImageStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Expected " + expected + ", but got " + status);
        }
    }

    private static String requirePath(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> throw new IllegalArgumentException("Unsupported image type");
        };
    }
}
