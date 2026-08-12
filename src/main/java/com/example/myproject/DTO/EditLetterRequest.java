package com.example.myproject.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EditLetterRequest(@NotBlank
    @Size(max=200)
    String title,

    @NotBlank
    @Size(max=10000)
    String text,

    @NotNull
    @Min(1)
    @Max(1440)
    Integer ttl,

    @Pattern(
        regexp = "^[A-Za-z0-9]{5}$",
        message = "Пароль должен состоять из 5 букв ил 5 цифр"
    )
    String password) {
    
}
