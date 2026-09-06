package com.example.myproject.Services;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.myproject.DTO.ImageMetadata;
import com.example.myproject.FileStorage.FileStorage;
import com.example.myproject.Model.Image;
import com.example.myproject.Model.Letter;
import com.example.myproject.Repositories.ImageRepository;

@Service
public class ImageService {
    
    private static final int MAX_IMAGES = 10;
    private ImageRepository imageRepository;
    private FileStorage fileStorage;
    private final String imageDir;
    private final String urlPrefix;


    public ImageService(ImageRepository imageRepository, FileStorage fileStorage, @Value("${app.storage.images-dir}") String imageDir, @Value("$app.storage.images-url-prefix") String urlPrefix) {
        this.imageRepository = imageRepository;
        this.fileStorage = fileStorage;
        this.imageDir = imageDir;
        this.urlPrefix = urlPrefix;
    }

    public List<Image> addImagesUploading(List<ImageMetadata> metadatas) {
        if (metadatas == null || metadatas.isEmpty()) {
            return List.of();
        }

        List<Image> images = new ArrayList<>();
        for (ImageMetadata metadata : metadatas) {
            Image image = addImageUploading(metadata);
            images.add(image);
        }        
        return images;
    }

    public Image addImageUploading(ImageMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        
        Path mainDir = Paths.get(metadata.mainDir());
        Image image = new Image(metadata.letter(), mainDir, metadata.extension());
        imageRepository.save(image);
        return  image;
    }

    public String storeImageInTemporaryDir() {

    } 
}
