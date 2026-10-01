package com.example.myproject.DTO;

public final class ValidationLimits {
    public static final int USERNAME_MIN_LENGTH = 2;
    public static final int USERNAME_MAX_LENGTH = 100;
    public static final int EMAIL_MAX_LENGTH = 320;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 72;
    public static final int LETTER_TITLE_MAX_LENGTH = 150;
    public static final int LETTER_TEXT_MAX_LENGTH = 1_000;

    private ValidationLimits() {}
}
