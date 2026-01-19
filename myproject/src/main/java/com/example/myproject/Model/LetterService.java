package com.example.myproject.Model;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service

public class LetterService {
    @Autowired
    private LetterRepository repository;
    
    public Letter loadLetterById(Long id){
        
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Letter with id " + id + " not found"));
    }

    
    public List<Letter> loadUserLetters(String email)throws UsernameNotFoundException {
        

        Optional<List<Letter>> user = repository.findByAuthor(email);
        if (user.isPresent()) {
            var userObj = user.get();
            return User.builder()
                    .username(userObj.getUsername())
                    .password(userObj.getPassword())
                    .build();    
        }else{
            throw new UsernameNotFoundException(email);
        }
    }
}
