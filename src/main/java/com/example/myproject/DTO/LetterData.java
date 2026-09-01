package com.example.myproject.DTO;

import java.util.List;

public record LetterData (
    String letterTitle,
    String letterText,
    String password,
    Integer ttl,
    Boolean burn_after_opening,
    FontData font,
    List<String> images,
    List<String> reactions
) {}
