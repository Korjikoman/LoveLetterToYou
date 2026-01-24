package com.example.myproject.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.myproject.Model.MyAppUser;

@Repository
public interface MyAppUserRepository  extends JpaRepository<MyAppUser, Long>{
    Optional<MyAppUser> findByUsername(String email);

    Optional<MyAppUser> findByEmail(String email);
}

