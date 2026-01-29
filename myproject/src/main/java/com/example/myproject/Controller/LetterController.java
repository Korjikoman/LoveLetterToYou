package com.example.myproject.Controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.myproject.Model.Letter;
import com.example.myproject.Model.MyAppUser;

import com.example.myproject.Repositories.MyAppUserRepository;
import com.example.myproject.Repositories.RedisRepository;
import com.example.myproject.Services.LetterService;
import com.example.myproject.Utils.PublicToken;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/create")
public class LetterController {


    @Autowired
    private LetterService letterService;

    @Autowired
    private MyAppUserRepository userRepository;

    @Autowired
    private RedisRepository redisRepository;

    @PostMapping(value="/letter", consumes="application/json")
    @ResponseBody
    public ResponseEntity<String> addLetterTextIntoDatabase(@RequestBody Map<String, String> body, HttpServletRequest request, Model model) {
        
        String letterText = body.get("text");

        String letterTitle = body.get("title");

        if (letterText == null || letterText.trim().isEmpty()){
            return new ResponseEntity<>("Letter text cannot be empty", HttpStatus.BAD_REQUEST);
        }


        // Получаем текущего пользователя
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        Optional<MyAppUser> userOptional = userRepository.findByEmail(userEmail);

        if (userOptional == null){
            return new ResponseEntity<>("User not found", HttpStatus.UNAUTHORIZED);
        }
        
        MyAppUser user = userOptional.get();
        

        // Создаем новое письмо
        Letter letter = new Letter();
        letter.setText(letterText);

        // ДОБАВИТЬ TITLE в html
        letter.setTitle(letterTitle);
        letter.setAuthor(user);

        // генерируем уникальный токен
        String publicToken = PublicToken.generatePublicToken();
        String fullURL;

        while (letterService.checkUniquePublicToken(publicToken) != null) {
            publicToken = PublicToken.generatePublicToken();

        };

        letter.setPublicToken(publicToken);

        // Сохраняем письмо в Redis
        redisRepository.add(letter);

        String url = getURL(request);

        fullURL = url + "/watch/letter"+ "/" + publicToken; 

        return ResponseEntity.ok(fullURL);
    }

    
    

    public  String getURL(HttpServletRequest request){
        return request.getScheme() + "://" + request.getServerName() + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort() );
    }
    
}
