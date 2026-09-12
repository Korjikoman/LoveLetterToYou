package com.example.myproject.DTO;

import com.example.myproject.Images.DTO.ImageView;

public record UserProfileView(
    String username,
    String email,
    ImageView avatar
) {}
