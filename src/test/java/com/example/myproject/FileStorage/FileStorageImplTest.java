package com.example.myproject.FileStorage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileStorageImplTest {

    @TempDir
    Path storageRoot;

    @Test
    void storePreservesExactBytesAndReturnsTheirMetadata() throws IOException {
        FileStorageImpl storage = new FileStorageImpl(storageRoot.toString());
        byte[] content = {0, 1, 2, 10, 13, 42, 127, -128, -1, 0};
        String relativePath = "staged/nested/image.upload";

        StoredFileInfo stored = storage.store(
            relativePath,
            new ByteArrayInputStream(content),
            content.length
        );

        assertArrayEquals(content, Files.readAllBytes(storageRoot.resolve(relativePath)));
        assertEquals(content.length, stored.sizeBytes());
        assertEquals(sha256(content), stored.sha256());
    }

    @Test
    void storeRemovesPartialAndTemporaryFilesWhenMaximumSizeIsExceeded()
        throws IOException {
        FileStorageImpl storage = new FileStorageImpl(storageRoot.toString());
        byte[] content = {1, 2, 3, 4, 5};
        Path target = storageRoot.resolve("staged/too-large.upload");

        assertThrows(
            IOException.class,
            () -> storage.store(
                "staged/too-large.upload",
                new ByteArrayInputStream(content),
                content.length - 1L
            )
        );

        assertFalse(Files.exists(target));
        try (var paths = Files.walk(storageRoot)) {
            assertEquals(0L, paths.filter(Files::isRegularFile).count());
        }
    }

    @Test
    void replaceIsIdempotentWhenTargetAlreadyHasExpectedChecksum() throws IOException {
        FileStorageImpl storage = new FileStorageImpl(storageRoot.toString());
        byte[] content = {9, 8, 7, 0, -1, 6};
        String sourcePath = "staged/image.upload";
        String targetPath = "ready/image.png";
        StoredFileInfo stored = storage.store(
            sourcePath,
            new ByteArrayInputStream(content),
            content.length
        );

        storage.replace(sourcePath, targetPath, stored.sha256());

        assertFalse(Files.exists(storageRoot.resolve(sourcePath)));
        assertArrayEquals(content, Files.readAllBytes(storageRoot.resolve(targetPath)));
        assertDoesNotThrow(() -> storage.replace(sourcePath, targetPath, stored.sha256()));
        assertArrayEquals(content, Files.readAllBytes(storageRoot.resolve(targetPath)));
    }

    @Test
    void replaceRejectsChecksumMismatchWithoutMovingSource() throws IOException {
        FileStorageImpl storage = new FileStorageImpl(storageRoot.toString());
        byte[] content = {3, 1, 4, 1, 5, 9};
        String sourcePath = "staged/image.upload";
        String targetPath = "ready/image.png";
        storage.store(sourcePath, new ByteArrayInputStream(content), content.length);

        assertThrows(
            IOException.class,
            () -> storage.replace(sourcePath, targetPath, "0".repeat(64))
        );

        assertArrayEquals(content, Files.readAllBytes(storageRoot.resolve(sourcePath)));
        assertFalse(Files.exists(storageRoot.resolve(targetPath)));
    }

    @Test
    void publicOperationsRejectParentDirectoryTraversal() {
        FileStorageImpl storage = new FileStorageImpl(storageRoot.toString());
        String escapedName = "escape-" + UUID.randomUUID() + ".bin";
        String traversalPath = "../" + escapedName;
        Path escapedTarget = storageRoot.getParent().resolve(escapedName);
        String checksum = "0".repeat(64);

        assertThrows(
            IllegalArgumentException.class,
            () -> storage.store(
                traversalPath,
                new ByteArrayInputStream(new byte[] {1}),
                1
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> storage.loadAsResource(traversalPath)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> storage.replace(traversalPath, "ready/image.png", checksum)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> storage.deleteFile(traversalPath)
        );
        assertFalse(Files.exists(escapedTarget));
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 must be available in the test runtime", exception);
        }
    }
}
