package com.example.myproject.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.myproject.Repositories.DeleteResult;
import com.example.myproject.Repositories.EditResult;
import com.example.myproject.Repositories.MyAppUserRepository;
import com.example.myproject.Repositories.RedisRepository;
import com.example.myproject.Services.LetterService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.example.myproject.DTO.EditLetterRequest;
import com.example.myproject.Model.Letter;
import com.example.myproject.Model.MyAppUser;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


@Controller
@RequestMapping("/letters")
public class ListLettersController {

    private LetterService letterService;

    public ListLettersController(LetterService letterService) {
        this.letterService = letterService;

    }

    @GetMapping("/get")
    public String getListOfLetters(Authentication auth, Model model) {
        String email = auth.getName();

        List<Letter> letters = new ArrayList<Letter>();

        letters = letterService.getLetters(email);
        
        model.addAttribute("letters", letters);
        return "list-of-letters";
    }

    @PostMapping("/delete")
    public ResponseEntity<?>  deleteLetter(@RequestParam String publicToken, Authentication auth) {
        String email = auth.getName();

        DeleteResult res = letterService.deleteLetter(publicToken, email);
        
        return switch (res) {
            case DELETED ->
                    ResponseEntity.ok().build();

            case INVALID ->
                    ResponseEntity.badRequest()
                            .body(Map.of("error", "Некорректные данные"));

            case NOT_FOUND ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of(
                                    "error",
                                    "Письмо не найдено или у вас нет доступа"
                            ));
            case FORBIDDEN ->
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "У Вас нет доступа к данному письму"));

        };
    }
    
    @GetMapping("/edit")
    public String editLetterFromListOfLettersGET(
            @RequestParam String publicToken,
            Model model,Authentication auth) {
        String email = auth.getName();
        
        Letter letter = letterService.getLetter(publicToken, email);

        if (letter == null) {
            return "redirect:/letters/get?error=notfound";
        }
        model.addAttribute("letterTitle", letter.getTitle());
        model.addAttribute("letterText", letter.getText()); // XSS entry point
        model.addAttribute("letterTTL", letter.getTTL());
        model.addAttribute("letterPassword", letter.getPassword());
        model.addAttribute("letterPublicToken", letter.getPublicToken());

        // НИЧЕГО не передаем про Redis
        model.addAttribute("redis-data", null);

        return "edit-letter-from-list-of-letters";
    }

    // УЯЗВИМЫЙ КОНТРОЛЛЕР - отладочный эндпоинт для доступа к базе данных
    // @GetMapping("/debug/redis-data")
    // @ResponseBody
    // public Map<Object, Object> debugRedisData(@RequestHeader(value = "X-Debug-Token", required = false) String debugToken) {
    //     // Проверка токена для безопасности
    //     if (debugToken != null && debugToken.equals("")) {
    //         return redisRepository.dumpTestData();
    //     }
    //     return Map.of("error", "Unauthorized access");
    // }
    
// !!!
    @PostMapping("/edit")
    public ResponseEntity<?>  editLetterFromListOfLettersPOST(@RequestParam String publicToken, @Valid @RequestBody EditLetterRequest body, Authentication auth) {
        
        String email = auth.getName();

        EditResult letter_is_set = letterService.setLetter(publicToken, email, body.title(), body.text(), body.password(), body.ttl());

       
        return switch (letter_is_set) {
            case UPDATED ->
                    ResponseEntity.noContent().build();

            case INVALID ->
                    ResponseEntity.badRequest()
                            .body(Map.of("error", "Некорректные данные"));

            case NOT_FOUND ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of(
                                    "error",
                                    "Письмо не найдено или у вас нет доступа"
                            ));
            case FORBIDDEN ->
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "У Вас нет доступа к данному письму"));
        };
    }
    

    
    
}
