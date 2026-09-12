package com.example.myproject.Outbox.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.myproject.Outbox.Model.OutboxEvent;

import jakarta.persistence.LockModeType;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = """
        SELECT *
        FROM outbox_event
        WHERE (status = 'PENDING' AND next_attempt_at <= :now)
           OR (status = 'PROCESSING' AND locked_until <= :now)
        ORDER BY created_at, id
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> lockAvailable(
        @Param("now") Instant now,
        @Param("batchSize") int batchSize
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from OutboxEvent event where event.id = :id")
    Optional<OutboxEvent> findForUpdate(@Param("id") UUID id);

    @Modifying
    @Query(value = """
        UPDATE outbox_event
        SET status = 'PROCESSED', processed_at = :now,
            claim_token = NULL, locked_until = NULL, last_error = NULL
        WHERE id = :id AND status = 'PROCESSING' AND claim_token = :claimToken
        """, nativeQuery = true)
    int setEventProcessed(
        @Param("id") UUID id,
        @Param("claimToken") UUID claimToken,
        @Param("now") Instant now
    );

    @Modifying
    @Query(value = """
        UPDATE outbox_event
        SET status = 'PENDING', next_attempt_at = :nextAttempt,
            claim_token = NULL, locked_until = NULL, last_error = :error
        WHERE id = :id AND status = 'PROCESSING' AND claim_token = :token
        """, nativeQuery = true)
    int releaseForRetry(
        @Param("id") UUID id,
        @Param("token") UUID token,
        @Param("nextAttempt") Instant nextAttempt,
        @Param("error") String error
    );

    @Modifying
    @Query(value = """
        UPDATE outbox_event
        SET status = 'DEAD', dead_at = :now,
            claim_token = NULL, locked_until = NULL, last_error = :error
        WHERE id = :id AND status = 'PROCESSING' AND claim_token = :token
        """, nativeQuery = true)
    int markDead(
        @Param("id") UUID id,
        @Param("token") UUID token,
        @Param("now") Instant now,
        @Param("error") String error
    );

    @Modifying
    @Query(value = """
        DELETE FROM outbox_event
        WHERE id IN (
            SELECT id FROM outbox_event
            WHERE status = 'PROCESSED' AND processed_at < :cutoff
            ORDER BY processed_at
            LIMIT :limit
        )
        """, nativeQuery = true)
    int deleteProcessedBefore(
        @Param("cutoff") Instant cutoff,
        @Param("limit") int limit
    );
}
