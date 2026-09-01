package com.example.myproject.DTO;

import java.util.List;

import com.example.myproject.Model.Reaction;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateLetterData (
    
    @NotBlank
    @Size(max=200)
    String letterTitle,

    @NotBlank
    @Size(max=10000)
    String letterText,

    String password,

    @NotNull
    @Min(1)
    @Max(1440)
    Integer ttl,

    
    boolean burn_after_opening,
    boolean isFontBold,
    boolean isFontCursive,
    boolean isFontUnderlined,
    String fontFamily,
    String fontName,
    List<Reaction> reactions
) {}
