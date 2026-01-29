package com.example.myproject.Services;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.myproject.Model.Letter;
import com.example.myproject.Repositories.LetterRepository;

@Service
public class LetterService {
    @Autowired
    private LetterRepository repository;
    
    public Letter loadLetterById(Long id){
        
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Letter with id " + id + " not found"));
    }

    
    public List<Letter> loadUserLetters(String email)throws UsernameNotFoundException {
        

        Optional<List<Letter>> letters = repository.findByAuthor(email);
        return letters.orElseThrow(() -> new UsernameNotFoundException(email));

    }

    public Letter checkUniquePublicToken(String token){
        return repository.findByPublicToken(token);
    }
}