package com.example.myproject.FileStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;

public class FileStorageImpl implements FileStorage {
    private final Path root;

    public FileStorageImpl(@Value("${app.storage.storage-root}") String root) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } 
    }

    @Override
    public String storeInDirectoryFileWithName(MultipartFile file, String dirname, String filename) throws IOException {
        validateFile(file);
        Path target = resolveSafely(dirname + filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
    }
    
    @Override
    public String storeInSpecificDirectory(MultipartFile file, String dirname)throws IOException {
        validateFile(file);
        String filename = dirname + sanitizeFilename(file.getOriginalFilename());
        Path target = resolveSafely(filename);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        validateFile(file);
        String filename = sanitizeFilename(file.getOriginalFilename());
        Path target = resolveSafely(filename);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
    }

    @Override
    public String getPublicUrl(String objectKey, String urlPrefix) {
        Path resolved = resolveSafely(objectKey);

        String normKey = root.relativize(resolved).toString().replace('\\', '/');
        return urlPrefix + "/" + normKey;
    }


    @Override
    public void promote(String source, String target) throws IOException {
        Path sourcePath = resolveSafely(source);
        Path targetPath = resolveSafely(target);
       
        try {
            Files.move(sourcePath, targetPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }


    @Override
    public void deleteDir(String dirPath) {
        
        Path dir = resolveSafely(dirPath);
        if (Files.notExists(dir, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Image path is not a directory");
        }

        try (Stream<Path> paths = Files.walk(dir)) {
            List<Path> pathsToDelete = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path path : pathsToDelete) {
                Files.deleteIfExists(path);
            }

        } catch (IOException e) { 
            throw new UncheckedIOException("Cannot delete directory " + dirPath, e);
        }

    }

    @Override 
    public Resource loadAsResource(String filename) throws IOException {
        Path resolved = resolveSafely(filename);

        if (!Files.isRegularFile(resolved) || !Files.isReadable(resolved)) {
            throw new IOException("Файл отсутствует или недоступен для чтения");
        }
        return new UrlResource(resolved.toUri());// ???
    }

    @Override
    public boolean deleteFile(String path_to_file) throws IOException {
        Path filePath = resolveSafely(path_to_file);
        return Files.deleteIfExists(filePath);
    }
    

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("File empty");
    }
    private Path resolveSafely(String relPath) {
        if (relPath == null || relPath.isBlank()) {
            throw new IllegalArgumentException("Image object key is empty");
        }
        Path resolved = root.resolve(relPath).normalize();
        if (resolved.equals(root) || !resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid image object key");
        }
        return resolved;
    }

    private String sanitizeFilename(String original) {
        String cleaned = Paths.get(original).getFileName().toString();
        return UUID.randomUUID() + "-" + cleaned.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    
}
