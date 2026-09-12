package com.example.myproject.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.myproject.Model.MyAppUser;

import jakarta.persistence.LockModeType;

@Repository
public interface MyAppUserRepository  extends JpaRepository<MyAppUser, Long>{
    Optional<MyAppUser> findByUsername(String username);

    Optional<MyAppUser> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select user
        from MyAppUser user
        where lower(user.email) = lower(:email)
        """)
    Optional<MyAppUser> findByEmailForUpdate(@Param("email") String email);
}
