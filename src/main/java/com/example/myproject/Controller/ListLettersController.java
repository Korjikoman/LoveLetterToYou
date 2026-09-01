package com.example.myproject.Controller;

import org.springframework.web.bind.annotation.RequestMapping;

import com.example.myproject.Services.LetterService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.example.myproject.DTO.DeleteResult;
import com.example.myproject.DTO.EditResult;
import com.example.myproject.DTO.LetterPage;
import com.example.myproject.DTO.LetterView;
import com.example.myproject.DTO.UpdateLetterData;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Controller
@RequestMapping("/letters")
public class ListLettersController {

    private LetterService letterService;
    private static final int PAGE_SIZE = 10;

    public ListLettersController(LetterService letterService) {
        this.letterService = letterService;

    }

    @GetMapping("/get")
    public ResponseEntity<LetterPage> getListOfLetters(Authentication auth, @RequestParam(required = false) Long beforeId) {
        String email = auth.getName();
        LetterPage letters = letterService.getLetters(email, beforeId, PAGE_SIZE);

        return ResponseEntity.ok(letters);
    }

    @PostMapping("/delete")
    public ResponseEntity<?>  deleteLetter(@RequestParam String publicToken, Authentication auth) {
        String email = auth.getName();

        DeleteResult res = letterService.deleteLetter(publicToken, email);
        
        return switch (res) {
            case DELETED ->
                    ResponseEntity.ok().body("Письмо удалено!");

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
        
        LetterView letter = letterService.getLetter(publicToken, email).orElse(null);

        if (letter == null) {
            return "redirect:/letters/get?error=notfound";
        }
        model.addAttribute("letterTitle", letter.title());
        model.addAttribute("letterText", letter.text());
        model.addAttribute("images", letter.imagesPaths());
        model.addAttribute("reactions", letter.reactions());
        model.addAttribute("letterexpiresAt", letter.expiresAt());
        model.addAttribute("letterPasswordProtected", letter.passwordProtected());
        model.addAttribute("letterPublicToken", letter.publicToken());

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
    public ResponseEntity<?>  editLetterFromListOfLettersPOST(@RequestParam String publicToken, @Valid @RequestBody UpdateLetterData body, Authentication auth) {
        
        String email = auth.getName();

        EditResult letter_is_set = letterService.updateLetter(publicToken, email, body);

       
        return switch (letter_is_set) {
            case UPDATED ->
                    ResponseEntity.ok().body("Письмо отредактировано");

            case INVALID ->
                    ResponseEntity.badRequest()
                            .body(Map.of("error", "Некорректные данные"));

            case NOT_FOUND ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of(
                                    "error",
                                    "Письмо не найдено"
                            ));
            case FORBIDDEN ->
                    ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "У Вас нет доступа к данному письму"));
        };
    }
    

    
    
}
