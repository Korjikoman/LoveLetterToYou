package com.example.myproject.Outbox.Events;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    DEAD
}
