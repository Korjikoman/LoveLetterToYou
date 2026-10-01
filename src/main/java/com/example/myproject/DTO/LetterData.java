package com.example.myproject.DTO;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LetterData(
    @JsonAlias("title")
    @NotBlank
    @Size(max = ValidationLimits.LETTER_TITLE_MAX_LENGTH)
    String letterTitle,

    @JsonAlias("text")
    @NotBlank
    @Size(max = ValidationLimits.LETTER_TEXT_MAX_LENGTH)
    String letterText,

    @NotNull @Min(5) @Max(1440) Integer ttl,
    FontData font,
    List<String> reactions,
    @Size(max = 10) List<UUID> imageIds
) {
    public LetterData {
        reactions = reactions == null ? List.of() : List.copyOf(reactions);
        imageIds = imageIds == null ? List.of() : List.copyOf(imageIds);
    }
}
