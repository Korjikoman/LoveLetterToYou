package com.example.myproject.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.myproject.DTO.CreateLetterResponse;
import com.example.myproject.DTO.LetterData;

import com.example.myproject.Services.LetterService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/create")
public class LetterController {
    @Autowired
    private LetterService letterService;
    
    
    @Value("${app.public-url}")
    private String publicURL;
    

    @PostMapping(value="/letter", consumes="application/json")
    @ResponseBody
    public ResponseEntity<Map<String, String>> addLetterTextIntoDatabase(@RequestBody LetterData body, HttpServletRequest request) {
        
        String letterText = body.letterText();

        
        if (letterText == null || letterText.trim().isEmpty()){
            return ResponseEntity.badRequest().body(Map.of("error", "Letter text is NULL"));
        }

        // Получаем email пользователя
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        CreateLetterResponse response = letterService.createLetter(userEmail, body);

        if (!response.error().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid letter data"));
        }
        String publicToken = response.publicToken();
        String fullURL = publicURL + "/watch/letter/" + publicToken; 

        return ResponseEntity.ok().body(
            Map.of("url", fullURL)
        );
    }
    
}
