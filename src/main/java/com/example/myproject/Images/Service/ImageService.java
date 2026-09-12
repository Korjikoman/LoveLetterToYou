package com.example.myproject.Images.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.myproject.FileStorage.FileStorage;
import com.example.myproject.FileStorage.StoredFileInfo;
import com.example.myproject.Images.Component.ImageInspector;
import com.example.myproject.Images.DTO.ImageContent;
import com.example.myproject.Images.DTO.ImageFileData;
import com.example.myproject.Images.DTO.ImageIsReadyResponse;
import com.example.myproject.Images.DTO.ImageMetadata;
import com.example.myproject.Images.DTO.ImagePurpose;
import com.example.myproject.Images.DTO.ImageStatus;
import com.example.myproject.Images.DTO.ImageUploadResponse;
import com.example.myproject.Images.DTO.InspectedImage;

@Service
public class ImageService {
    private final ImageInspector imageInspector;
    private final ImageTransactionService transactions;
    private final FileStorage fileStorage;
    private final long maxBytes;

    public ImageService(
        FileStorage fileStorage,
        @Value("${app.images.max-bytes:10485760}") long maxBytes,
        ImageInspector imageInspector,
        ImageTransactionService transactions
    ) {
        this.fileStorage = fileStorage;
        this.maxBytes = maxBytes;
        this.imageInspector = imageInspector;
        this.transactions = transactions;
    }

    /** Регистрирует загрузку, проверяет файл и ставит его перенос в очередь. */
    @Transactional(propagation = Propagation.NEVER)
    public ImageUploadResponse upload(
        String email,
        ImagePurpose purpose,
        MultipartFile file
    ) {
        validateRequest(email, purpose, file);
        ImageMetadata metadata = transactions.registerUpload(email, purpose);

        try {
            StoredFileInfo stored;
            try (InputStream input = file.getInputStream()) {
                stored = fileStorage.store(metadata.tempRelPath(), input, maxBytes);
            }

            Resource staged = fileStorage.loadAsResource(metadata.tempRelPath());
            InspectedImage inspected = imageInspector.inspect(staged);
            transactions.markStagedAndQueuePromotion(
                metadata.imageId(), email, stored, inspected
            );

            return new ImageUploadResponse(
                metadata.imageId(), ImageStatus.STAGED, metadata.uploadExpiresAt()
            );
        } catch (Exception exception) {
            cleanupFailedUpload(metadata, email, exception);
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (exception instanceof IOException ioException) {
                throw new UncheckedIOException(ioException);
            }
            throw new IllegalStateException(exception);
        }
    }

    public ImageIsReadyResponse getIsReadyResponse(String email, UUID imageId) {
        return transactions.getImageReady(imageId, email);
    }

    public void cancel(String email, UUID imageId) {
        transactions.cancelUnattached(imageId, email);
    }

    public ImageContent loadOwnedContent(String email, UUID imageId) {
        ImageFileData data = transactions.getOwnedFile(imageId, email);
        try {
            return new ImageContent(
                fileStorage.loadAsResource(data.mainRelPath()),
                data.contentType(),
                data.sizeBytes(),
                data.id().toString()
            );
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void cleanupFailedUpload(
        ImageMetadata metadata,
        String email,
        Exception original
    ) {
        try {
            fileStorage.deleteFile(metadata.tempRelPath());
        } catch (Exception cleanupError) {
            original.addSuppressed(cleanupError);
        }
        try {
            transactions.cancelUnattached(metadata.imageId(), email);
        } catch (Exception cleanupError) {
            original.addSuppressed(cleanupError);
        }
    }

    private void validateRequest(
        String email,
        ImagePurpose purpose,
        MultipartFile file
    ) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }
        if (purpose == null) {
            throw new IllegalArgumentException("Image purpose is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image is empty");
        }
        if (file.getSize() <= 0 || file.getSize() > maxBytes) {
            throw new IllegalArgumentException("Invalid image size");
        }
    }
}
