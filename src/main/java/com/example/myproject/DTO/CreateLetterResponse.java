package com.example.myproject.DTO;

import java.util.List;

public record CreateLetterResponse (
    String publicToken,
    String securityKey,
    List<String> images,
    String error
){}
    
