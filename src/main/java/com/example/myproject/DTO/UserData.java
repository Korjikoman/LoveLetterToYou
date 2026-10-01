package com.example.myproject.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserData(
    @NotBlank
    @Size(
        min = ValidationLimits.USERNAME_MIN_LENGTH,
        max = ValidationLimits.USERNAME_MAX_LENGTH
    )
    String username,

    @NotBlank
    @Size(
        min = ValidationLimits.PASSWORD_MIN_LENGTH,
        max = ValidationLimits.PASSWORD_MAX_LENGTH
    )
    String password,

    @NotBlank
    @Email
    @Size(max = ValidationLimits.EMAIL_MAX_LENGTH)
    String email
) {}
