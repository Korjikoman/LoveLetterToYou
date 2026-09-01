package com.example.myproject.Events;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    DEAD
}
