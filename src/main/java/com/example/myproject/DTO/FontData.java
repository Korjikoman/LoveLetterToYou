package com.example.myproject.DTO;

import jakarta.persistence.Embeddable;

@Embeddable
public record FontData (
    boolean isFontBold,
    boolean isFontCursive,
    boolean isFontUnderlined,
    String fontFamily,
    String fontName
) {}
