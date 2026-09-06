package com.example.myproject.DTO;

import com.example.myproject.Model.Image;
import com.example.myproject.Model.Letter;

public record ImageMetadata(
    ImageStatus status,
    String tempDir,
    String mainDir,
    String extension,
    Letter letter
) {
    
}
