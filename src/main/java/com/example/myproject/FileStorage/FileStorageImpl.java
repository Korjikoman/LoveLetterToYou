package com.example.myproject.FileStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

@Service
public class FileStorageImpl implements FileStorage {
    private final Path root;

    public FileStorageImpl(@Value("${app.storage.root:./data/storage}") String root) {
        try {
            this.root = Paths.get(root).toAbsolutePath().normalize();
            Files.createDirectories(this.root);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot initialize storage", exception);
        }
    }

    /** Записывает файл целиком и одновременно считает его контрольную сумму. */
    @Override
    public StoredFileInfo store(
        String relativePath,
        InputStream input,
        long maxBytes
    ) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Input is null");
        }
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("Invalid file limit");
        }

        Path target = resolveSafely(relativePath);
        Files.createDirectories(target.getParent());
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileAlreadyExistsException(relativePath);
        }

        Path temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        MessageDigest digest = sha256Digest();
        long total = 0;

        try {
            try (
                OutputStream raw = Files.newOutputStream(temporary);
                DigestOutputStream output = new DigestOutputStream(raw, digest)
            ) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > maxBytes) {
                        throw new IOException("Image exceeds max size");
                    }
                    output.write(buffer, 0, read);
                }
            }

            moveWithoutReplace(temporary, target);
            return new StoredFileInfo(total, HexFormat.of().formatHex(digest.digest()));
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public Resource loadAsResource(String relativePath) throws IOException {
        Path file = resolveSafely(relativePath);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
            || !Files.isReadable(file)) {
            throw new NoSuchFileException(relativePath);
        }
        return new UrlResource(file.toUri());
    }

    /** Повторный вызов безопасен: уже перенесённый файл сверяется по сумме. */
    @Override
    public void replace(
        String sourcePath,
        String targetPath,
        String expectedSha256
    ) throws IOException {
        requireChecksum(expectedSha256);
        Path source = resolveSafely(sourcePath);
        Path target = resolveSafely(targetPath);
        Files.createDirectories(target.getParent());

        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            verifyChecksum(target, expectedSha256);
            Files.deleteIfExists(source);
            return;
        }
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
            throw new NoSuchFileException(sourcePath);
        }

        verifyChecksum(source, expectedSha256);
        try {
            moveWithoutReplace(source, target);
        } catch (FileAlreadyExistsException exception) {
            verifyChecksum(target, expectedSha256);
            Files.deleteIfExists(source);
        }
        verifyChecksum(target, expectedSha256);
    }

    @Override
    public boolean deleteFile(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        return Files.deleteIfExists(resolveSafely(relativePath));
    }

    private void moveWithoutReplace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private void verifyChecksum(Path file, String expected) throws IOException {
        String actual = sha256(file);
        if (!MessageDigest.isEqual(
            actual.getBytes(StandardCharsets.US_ASCII),
            expected.getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new IOException("Image checksum mismatch");
        }
    }

    private String sha256(Path file) throws IOException {
        MessageDigest digest = sha256Digest();
        try (
            InputStream input = Files.newInputStream(file);
            DigestInputStream digestInput = new DigestInputStream(input, digest)
        ) {
            digestInput.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void requireChecksum(String checksum) {
        if (checksum == null || !checksum.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid SHA-256 checksum");
        }
    }

    private Path resolveSafely(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("File key is empty");
        }

        Path resolved = root.resolve(relativePath).normalize();
        if (resolved.equals(root) || !resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid file key");
        }
        return resolved;
    }
}
