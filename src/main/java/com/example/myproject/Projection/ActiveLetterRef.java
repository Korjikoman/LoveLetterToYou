package com.example.myproject.Projection;

import java.time.Instant;

public interface ActiveLetterRef {
    Long getId();
    String getPublicToken();
    long getVersion();
    Instant getExpiresAt();
    
}
