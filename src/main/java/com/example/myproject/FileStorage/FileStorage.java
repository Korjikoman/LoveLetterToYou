package com.example.myproject.FileStorage;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public interface FileStorage {
    String store(MultipartFile file) throws IOException;
    String storeInSpecificDirectory(MultipartFile file, String dirname)throws IOException;
    String storeInDirectoryFileWithName(MultipartFile file, String dirname, String filename) throws IOException;
    void promote(String source, String target) throws IOException;
    Resource loadAsResource(String filename) throws IOException;
    boolean deleteFile(String path_to_file) throws IOException;
    void deleteDir(String dirPath);
    String getPublicUrl(String objectKey, String urlPrefix);
}
