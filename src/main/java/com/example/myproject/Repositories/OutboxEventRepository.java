package com.example.myproject.Repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.myproject.Model.OutboxEvent;


public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID>{
    @Query(value = """
        SELECT * FROM outbox_event WHERE 
            (
                status='PENDING'
                AND next_attempt_at <= :now
            )
        OR 
            (
                status = 'PROCESSING'
                AND locked_until <= :now
            )   
        ORDER BY created_at, id
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> lockAvailable(@Param("now") Instant now, @Param("batchSize") Integer batchSize);
    
    @Modifying 
    @Query(value = """
            UPDATE outbox_event
            SET status = 'PROCESSED',
            processed_at = :now,
            claim_token = NULL,
            locked_until = NULL,
            last_error = NULL
            WHERE id  = :id AND STATUS = 'PROCESSING' AND claim_token = :claim_token;
            """, nativeQuery = true)
    Integer setEventProcessed(@Param("now") Instant now, @Param("id") UUID id, @Param("claim_token") UUID claimToken);

    Optional<OutboxEvent> findById(UUID id);
    
    @Modifying 
    @Query(
        value = """
           DELETE FROM outbox_event WHERE id = :id 
           AND status = 'PROVESSING'
           AND claim_token = :claimToken 
        """, nativeQuery = true
    )
    Integer deleteProcessed( @Param("id") UUID id, @Param("claim_token") UUID claimToken);

}
