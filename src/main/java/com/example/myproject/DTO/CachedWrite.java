package com.example.myproject.DTO;

import java.time.Duration;

import com.example.myproject.Model.CachedLetter;

public record CachedWrite (
    CachedLetter cachedLetter,
    Duration ttl
){}
