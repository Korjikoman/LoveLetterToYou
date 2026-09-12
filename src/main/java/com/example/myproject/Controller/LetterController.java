package com.example.myproject.Controller;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.example.myproject.DTO.CreateLetterResponse;
import com.example.myproject.DTO.DeleteResult;
import com.example.myproject.DTO.EditResult;
import com.example.myproject.DTO.FontData;
import com.example.myproject.DTO.LetterData;
import com.example.myproject.DTO.LetterView;
import com.example.myproject.DTO.UpdateLetterData;
import com.example.myproject.Services.LetterService;

import jakarta.validation.Valid;

@Controller
public class LetterController {
    private final LetterService letterService;

    public LetterController(LetterService letterService) {
        this.letterService = letterService;
    }

    // Письмо получает идентификаторы уже загруженных изображений из JSON.
    @PostMapping(
        value = {"/letter/create", "/create/letter"},
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CreateLetterResponse> createLetter(
        @Valid @RequestBody LetterData data,
        Authentication authentication
    ) {
        CreateLetterResponse response = letterService.createLetter(
            requireEmail(authentication),
            data
        );

        if (response.error() != null && !response.error().isBlank()) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(
        value = {"/letter/edit", "/letters/edit"},
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> updateLetter(
        @RequestParam String publicToken,
        @Valid @RequestBody UpdateLetterData body,
        Authentication authentication
    ) {
        EditResult result = letterService.updateLetter(
            publicToken,
            requireEmail(authentication),
            body
        );

        return switch (result) {
            case UPDATED -> ResponseEntity.ok(
                Map.of("message", "Письмо отредактировано")
            );
            case INVALID -> ResponseEntity.badRequest().body(
                Map.of("error", "Некорректные данные")
            );
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("error", "Письмо не найдено")
            );
            case FORBIDDEN -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of("error", "Нет доступа к письму")
            );
        };
    }

    @GetMapping({"/letter/edit", "/letters/edit"})
    public String editPage(
        @RequestParam String publicToken,
        Model model,
        Authentication authentication
    ) {
        LetterView letter = letterService
            .getLetter(publicToken, requireEmail(authentication))
            .orElse(null);

        if (letter == null) {
            return "redirect:/letters/get?error=notfound";
        }

        model.addAttribute("letterTitle", letter.title());
        model.addAttribute("letterText", letter.text());
        model.addAttribute("images", letter.images());
        model.addAttribute("letterVersion", letter.version());
        model.addAttribute(
            "letterTTL",
            remainingMinutes(letter.expiresAt())
        );
        model.addAttribute("reactions", letter.reactions());
        model.addAttribute("letterBurnAfterOpening", letter.burnAfterOpening());
        FontData font = letter.font();
        model.addAttribute("fontPresent", font != null);
        model.addAttribute("fontBold", font != null && font.isFontBold());
        model.addAttribute("fontCursive", font != null && font.isFontCursive());
        model.addAttribute("fontUnderlined", font != null && font.isFontUnderlined());
        model.addAttribute("fontFamily", font == null ? "" : font.fontFamily());
        model.addAttribute("fontName", font == null ? "" : font.fontName());
        model.addAttribute("letterexpiresAt", letter.expiresAt());
        model.addAttribute("letterPublicToken", letter.publicToken());

        return "edit-letter-from-list-of-letters";
    }

    @PostMapping({"/letter/delete", "/letters/delete"})
    public ResponseEntity<?> deleteLetter(
        @RequestParam String publicToken,
        Authentication authentication
    ) {
        DeleteResult result = letterService.deleteLetter(
            publicToken,
            requireEmail(authentication)
        );

        return switch (result) {
            case DELETED -> ResponseEntity.ok(
                Map.of("message", "Письмо удалено")
            );
            case INVALID -> ResponseEntity.badRequest().body(
                Map.of("error", "Некорректные данные")
            );
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("error", "Письмо не найдено")
            );
            case FORBIDDEN -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of("error", "Нет доступа к письму")
            );
        };
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null
            || authentication.getName() == null
            || authentication.getName().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Требуется авторизация"
            );
        }

        return authentication.getName();
    }

    private long remainingMinutes(Instant expiresAt) {
        long seconds = Math.max(
            0L,
            Duration.between(Instant.now(), expiresAt).toSeconds()
        );
        long roundedUp = (seconds + 59L) / 60L;
        return Math.max(5L, Math.min(1440L, roundedUp));
    }
}
