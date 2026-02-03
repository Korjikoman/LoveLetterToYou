package com.example.myproject.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.myproject.Repositories.RedisRepository;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.example.myproject.Model.Letter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Controller
@RequestMapping("/letters")
public class ListLettersController {

    @Autowired
    RedisRepository redisRepository;

    @GetMapping("/get")
    public String getListOfLetters(Authentication auth, Model model) {
        String email = auth.getName();

        List<Letter> letters = new ArrayList<Letter>();

        letters = redisRepository.getAllLetters(email);
        
        model.addAttribute("letters", letters);
        return "list-of-letters";
    }

    @GetMapping("/edit")
    public String editLetterFromListOfLettersGET(@RequestParam String publicToken, Model model) {
        Letter letter = redisRepository.findLetter(publicToken);
        if(letter == null) {
            return "redirect:/letters/get?error=notfound";
        }
        model.addAttribute("letterTitle", letter.getTitle());
        model.addAttribute("letterText", letter.getText());
        model.addAttribute("letterTTL", letter.getTTL());
        model.addAttribute("letterPassword", letter.getPassword());
        model.addAttribute("letterPublicToken", letter.getPublicToken());
        return "edit-letter-from-list-of-letters";
    }

    
    @PostMapping("/edit")
    public String  editLetterFromListOfLettersPOST(@RequestParam String publicToken, @RequestBody Map<String, String>body, Authentication auth) {
        String letterText = body.get("text");

        String letterTitle = body.get("title");

        String password = body.get("password");

        String email = auth.getName();
        Integer ttl = Integer.parseInt(body.get("ttl"));

        if (letterText == null || letterText.trim().isEmpty()){
            return "redirect:/letters/get?error=notfound";
        }

        Letter letter = redisRepository.findLetter(publicToken);
        letter.setText(letterText);
        letter.setTitle(letterTitle);
        letter.setAuthorEmail(email);
        letter.setPassword(password);
        letter.setPublicToken(publicToken);
        letter.setTTL(ttl);

        // add работает как и update
        redisRepository.add(letter);



        return "redirect:/letters/get?success=true";
    }
    

    
    
}
