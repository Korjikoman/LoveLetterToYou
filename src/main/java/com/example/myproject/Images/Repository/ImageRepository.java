package com.example.myproject.Images.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.myproject.Images.Model.Image;

import jakarta.persistence.LockModeType;

@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select image from Image image where image.id = :id")
    Optional<Image> findForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select image from Image image
        where image.id = :id
          and lower(image.user.email) = lower(:email)
        """)
    Optional<Image> findOwnedForUpdate(
        @Param("id") UUID id,
        @Param("email") String email
    );

    @Query("""
        select image
        from Image image
        where image.id = :id
          and lower(image.user.email) = lower(:email)
        """)
    Optional<Image> findByIdAndUser_EmailIgnoreCase(
        @Param("id") UUID id,
        @Param("email") String email
    );

    /** Блокирует только просроченные незакреплённые изображения. */
    @Query(value = """
        SELECT *
        FROM image
        WHERE (status = 'UPLOADING' AND upload_expires_at <= :now)
           OR (status = 'READY' AND ready_expires_at <= :now)
        ORDER BY created_at, id
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<Image> lockExpired(
        @Param("now") Instant now,
        @Param("limit") int limit
    );
}
