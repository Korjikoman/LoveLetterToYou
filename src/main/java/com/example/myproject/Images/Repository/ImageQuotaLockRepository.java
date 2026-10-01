package com.example.myproject.Images.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.myproject.Images.Model.ImageQuotaLock;

import jakarta.persistence.LockModeType;

@Repository
public interface ImageQuotaLockRepository
    extends JpaRepository<ImageQuotaLock, Short> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select quotaLock
        from ImageQuotaLock quotaLock
        where quotaLock.id = :id
        """)
    Optional<ImageQuotaLock> findForUpdate(@Param("id") short id);
}