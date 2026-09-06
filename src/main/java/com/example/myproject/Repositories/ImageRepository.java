package com.example.myproject.Repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.myproject.Model.Image;
import com.example.myproject.Model.Letter;

@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {
    void deleteById(UUID id);
    void deleteByLetter(Letter letter);

    Optional<Image> findByImagePath(String image_path);
    Optional<Image> findByID(UUID id);
    
}
