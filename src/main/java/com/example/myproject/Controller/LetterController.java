package com.example.myproject.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.myproject.DTO.CreateLetterResponse;
import com.example.myproject.DTO.DeleteResult;
import com.example.myproject.DTO.EditResult;
import com.example.myproject.DTO.LetterData;
import com.example.myproject.DTO.LetterView;
import com.example.myproject.DTO.UpdateLetterData;
import com.example.myproject.Services.LetterService;

import jakarta.mail.Multipart;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/letter")
public class LetterController {
    @Autowired
    private LetterService letterService;
    
    
    @Value("${app.public-url}")
    private String publicURL;
    

    @PostMapping(value="/create", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<CreateLetterResponse> createLetter(@Valid @RequestPart("data") LetterData data, @RequestPart(value = "images", required = false) List<MultipartFile> images, Authentication auth) {
        
        String userEmail = auth.getName();
        CreateLetterResponse response = letterService.createLetter(userEmail, data, images == null ? List.of() : images);

        if (!response.error().isEmpty()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/edit")
    public ResponseEntity<?>  postEditLetterJSON(@RequestParam String publicToken, @Valid @RequestBody UpdateLetterData body, Authentication auth) {
        
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

    @GetMapping("/edit")
    public String getEditLetterHTML(
            @RequestParam String publicToken,
            Model model,Authentication auth) {
        String email = auth.getName();
        
        LetterView letter = letterService.getLetter(publicToken, email).orElse(null);

        if (letter == null) {
            return "redirect:/letters/get?error=notfound";
        }
        model.addAttribute("letterTitle", letter.title());
        model.addAttribute("letterText", letter.text());
        model.addAttribute("images", letter.images());
        model.addAttribute("reactions", letter.reactions());
        model.addAttribute("letterexpiresAt", letter.expiresAt());
        model.addAttribute("letterPublicToken", letter.publicToken());

        // НИЧЕГО не передаем про Redis
        model.addAttribute("redis-data", null);

        return "edit-letter-from-list-of-letters";
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
    
}
