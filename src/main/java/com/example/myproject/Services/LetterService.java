package com.example.myproject.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.myproject.Model.Letter;
import com.example.myproject.Repositories.DeleteResult;
import com.example.myproject.Repositories.EditResult;
import com.example.myproject.Repositories.LetterRepository;
import com.example.myproject.Repositories.RedisRepository;

@Service
public class LetterService {
    private LetterRepository letterRepository;
    private RedisRepository redisRepository;
    

    public LetterService(LetterRepository letterRepository, RedisRepository redisRepository){
        this.letterRepository = letterRepository;
        this.redisRepository = redisRepository;
    }

    public Letter checkUniquePublicToken(String token){
        return letterRepository.findByPublicToken(token);
    }


    public List<Letter> getLetters(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email пользователя не указан");
        }
        List<Letter> letters = redisRepository.getAllLetters(email);

        List<Letter> returnLettersAfterFilter = new ArrayList<>();
        for (var letter : letters) {
            if (letter == null) continue;
            String authorEmail = letter.getAuthorEmail();
            if (email.equalsIgnoreCase(authorEmail)) {
                returnLettersAfterFilter.add(letter);
            }

        }

        return returnLettersAfterFilter;
    }

    public Letter getLetter(String publicToken, String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email пользователя не указан");
        }
        
        
        Letter letter = redisRepository.findLetter(publicToken);

        
        if (letter == null || !email.equals(letter.getAuthorEmail())) {
            return null;
        }

        return letter;
    }

    public EditResult setLetter(String publicToken, String email, String letterTitle, String letterText, String password, Integer ttl) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email пользователя не указан");
        }
        
        Letter letter = new Letter();

        if (letterText == null || letterText.trim().isEmpty()){
            return EditResult.INVALID;
        }
        

        letter.setText(letterText);
        letter.setTitle(letterTitle);
        letter.setAuthorEmail(email);

        if (password != null){
            letter.setPassword(password);

        }
        letter.setPublicToken(publicToken);
        if (ttl < 5 || ttl > 1140) return  EditResult.INVALID; // 1440 min = 24 hours
        
        letter.setTTL(ttl);

        return redisRepository.updateLetter(letter);
        
    }

    public DeleteResult deleteLetter(String publicToken, String email) {
        
        if (publicToken == null || publicToken.isEmpty()){
            return DeleteResult.INVALID;
        }
            
        return redisRepository.delete(publicToken, email);
    }

    
}   