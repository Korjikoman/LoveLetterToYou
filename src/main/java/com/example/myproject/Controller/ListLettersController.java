package com.example.myproject.Controller;

import org.springframework.web.bind.annotation.RequestMapping;

import com.example.myproject.Services.LetterService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import com.example.myproject.DTO.LetterPage;



@Controller
@RequestMapping("/letters")
public class ListLettersController {

    private LetterService letterService;
    private static final int PAGE_SIZE = 10;

    public ListLettersController(LetterService letterService) {
        this.letterService = letterService;

    }

    @GetMapping("/api/get")
    public ResponseEntity<LetterPage> getListOfLettersJSON(Authentication auth, @RequestParam(required = false) Long beforeId) {
        String email = auth.getName();
        LetterPage letters = letterService.getLetters(email, beforeId, PAGE_SIZE);

        return ResponseEntity.ok(letters);
    }

    @GetMapping("/get")
    public String getListOfLettersHTML(Authentication auth, @RequestParam(required = false) Long beforeId, Model model) {
        String email = auth.getName();
        LetterPage letters = letterService.getLetters(email, beforeId, PAGE_SIZE);
        model.addAttribute("letters", letters.items());
        model.addAttribute("nextBeforeId", letters.nextBeforeId());

        return "list-of-letters";
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
    
    
}
