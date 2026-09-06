package com.example.myproject.DTO;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public record LetterData (
    String letterTitle,
    String letterText,
    Integer ttl,
    Boolean burn_after_opening,
    FontData font,
    List<String> reactions
) {}
