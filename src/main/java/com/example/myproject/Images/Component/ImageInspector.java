package com.example.myproject.Images.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.example.myproject.Images.DTO.InspectedImage;


@Component 
public class ImageInspector {
    private final int maxWidth;
    private final int maxHeight;
    private final long maxPixels;

    public ImageInspector(
        @Value("${app.images.max-width}") int maxWidth,
        @Value("${app.images.max-height}") int maxHeight,
        @Value("${app.images.max-pixels}") long maxPixels
    ) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxPixels = maxPixels;
    }

    public InspectedImage inspect(Resource resource) throws IOException {
        try (InputStream raw =resource.getInputStream();
            ImageInputStream inputStream = ImageIO.createImageInputStream(raw) 
        ) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Invalid Image"); 
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Unsupported image");
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(inputStream, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT); 

                String contentType = switch (format) {
                    case "jpeg", "jpg" -> "image/jpeg";
                    case "png" -> "image/png";
                    default -> throw new IllegalArgumentException("Only JPEG and PNG are allowed");
                };

                int width = reader.getWidth(0);
                int height =  reader.getHeight(0);
                long pixels = Math.multiplyExact((long) width, (long) height);

                if (width <= 0 || height <= 0 || width > maxWidth || height > maxHeight || pixels > maxPixels) {
                    throw new IllegalArgumentException(
                        "Invalid image dimensions"
                    );
                }
                // Полное чтение отбрасывает повреждённые файлы.
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("Cannot decode image");
                }

                return new InspectedImage(
                    contentType,
                    width,
                    height
                );
            } finally {
                reader.dispose();
            }
        }

    }
}
