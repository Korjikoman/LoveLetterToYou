package com.example.myproject.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.myproject.Model.Letter;

@Repository
public interface LetterRepository extends JpaRepository <Letter, Long>{
    Optional<Letter> findById(Long id);
    

    Letter findByPublicToken(String publicToken);
}
