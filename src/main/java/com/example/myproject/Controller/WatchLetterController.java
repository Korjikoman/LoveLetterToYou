package com.example.myproject.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.myproject.DTO.LetterView;
import com.example.myproject.Services.LetterService;

@Controller
@RequestMapping("/watch/letter")
public class WatchLetterController {
    LetterService letterService;

    public WatchLetterController(LetterService letterService) {
        this.letterService = letterService;
    }

    @PostMapping("/{publicToken}")
    public String confirmPassword(@RequestParam String key, @PathVariable String publicToken, Model model){
        LetterView letter = letterService.getLetterToAnonymous(publicToken, key).orElse(null);
        if (letter == null){
            return "letter-not-found";
        }

        if (!letter.passwordProtected()) {
            model.addAttribute("letterTitle", letter.title());
            model.addAttribute("letterText", letter.text());
            model.addAttribute("authorEmail", letter.authorName());

            return "open-letter-and-watch-content";
        }

        return "confirm-password";
    }

    @GetMapping("/{publicToken}")
    public String confirmPasswordGET() {
        return "confirm-password";
    }
        
        
}
    
    
