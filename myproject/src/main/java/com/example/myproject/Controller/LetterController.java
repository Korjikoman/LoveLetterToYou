package com.example.myproject.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.myproject.Model.Letter;
import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Repositories.LetterRepository;
import com.example.myproject.Repositories.MyAppUserRepository;
import com.example.myproject.Services.LetterService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/create")
public class LetterController {
    @Autowired
    private LetterRepository letterRepository;

    @Autowired
    private LetterService letterService;

    @Autowired
    private MyAppUserRepository userRepository;

    @PostMapping(value="/letter", consumes="application/json")
    public ResponseEntity<String> addLetterTextIntoDatabase(@RequestBody Map<String, String> request) {
        
        String letterText = request.get("text");

        if (letterText == null || letterText.trim().isEmpty()){
            return new ResponseEntity<>("Letter text cannot be empty", HttpStatus.BAD_REQUEST);
        }


        // Получаем текущего пользователя
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        MyAppUser user = userRepository.findByEmail(userEmail).get();

        if (user == null){
            return new ResponseEntity<>("User not found", HttpStatus.UNAUTHORIZED);

        }

        // Создаем новое письмо
        Letter letter = new Letter();
        letter.setText(letterText);
        letter.setTitle("Nigga balls");
        letter.setAuthor(user);
        letter.setURL("fuckingurl.con");

        // Сохраняем его в бд
        letterRepository.save(letter);


        return new ResponseEntity<>("Letter saved successfully!", HttpStatus.OK);
    }
    
}
