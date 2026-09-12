package com.example.myproject.FileStorage;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;

public interface FileStorage {
    StoredFileInfo store(String relativePath, InputStream input, long maxBytes)
        throws IOException;

    Resource loadAsResource(String relativePath) throws IOException;

    void replace(String sourcePath, String targetPath, String expectedSha256)
        throws IOException;

    boolean deleteFile(String relativePath) throws IOException;
}
