package com.example.myproject.DTO;

public record FontData (
    boolean isFontBold,
    boolean isFontCursive,
    boolean isFontUnderlined,
    String fontFamily,
    String fontName
) {}
