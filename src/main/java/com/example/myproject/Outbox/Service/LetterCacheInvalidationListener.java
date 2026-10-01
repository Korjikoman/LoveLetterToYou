package com.example.myproject.Outbox.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.myproject.Outbox.Events.LetterCacheInvalidationEvent;
import com.example.myproject.Repositories.RedisRepository;

@Service
public class LetterCacheInvalidationListener {
    private static final Logger log = LoggerFactory.getLogger(
        LetterCacheInvalidationListener.class
    );

    private final RedisRepository redisRepository;

    public LetterCacheInvalidationListener(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void evictAfterCommit(LetterCacheInvalidationEvent event) {
        try {
            boolean evicted = Boolean.TRUE.equals(redisRepository.evictLetter(
                event.publicToken(), event.email(), event.version()
            ));
            if (!evicted) {
                log.warn(
                    "Synchronous letter cache eviction was not applied: token={}, version={}",
                    event.publicToken(), event.version()
                );
            }
        } catch (RuntimeException exception) {
            log.warn(
                "Synchronous letter cache eviction failed: token={}, version={}",
                event.publicToken(), event.version(), exception
            );
        }
    }
}
