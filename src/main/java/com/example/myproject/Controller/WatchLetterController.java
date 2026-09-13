package com.example.myproject.Controller;

import java.time.Clock;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.example.myproject.DTO.LetterView;
import com.example.myproject.Images.DTO.ImageContent;
import com.example.myproject.Images.DTO.ImageStatus;
import com.example.myproject.Images.Service.ImageService;
import com.example.myproject.Model.Letter;
import com.example.myproject.Repositories.LetterRepository;
import com.example.myproject.Services.LetterService;
import com.example.myproject.Services.SmartLetterRenderer;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/watch/letter")
public class WatchLetterController {
    private final LetterService letterService;
    private final LetterRepository letterRepository;
    private final ImageService imageService;
    private final Clock clock;
    private SmartLetterRenderer smartLetterRenderer;

    public WatchLetterController(
        LetterService letterService,
        LetterRepository letterRepository,
        ImageService imageService,
        Clock clock,
        SmartLetterRenderer smartLetterRenderer
    ) {
        this.letterService = letterService;
        this.letterRepository = letterRepository;
        this.imageService = imageService;
        this.clock = clock;
        this.smartLetterRenderer = smartLetterRenderer;
    }

    @PostMapping("/{publicToken}")
    public String confirmPassword(@RequestParam String key, @PathVariable String publicToken, Model model){
        return renderLetter(publicToken, key, model);
    }

    @GetMapping("/{publicToken}")
    public String openOrConfirm(
        @PathVariable String publicToken,
        @RequestParam(required = false) String key,
        Model model
    ) {
        if (key == null || key.isBlank()) {
            model.addAttribute("publicToken", publicToken);
            return "confirm-password";
        }
        return renderLetter(publicToken, key, model);
    }

    private String renderLetter(String publicToken, String key, Model model) {
        LetterView letter = letterService.getLetterToAnonymous(publicToken, key).orElse(null);
        if (letter == null){
            model.addAttribute("publicToken", publicToken);
            model.addAttribute("error", "Неверный ключ или письмо больше недоступно");
            return "confirm-password";
        }

        model.addAttribute("letterText", smartLetterRenderer.render(letter));
        model.addAttribute("authorName", letter.authorName());
        model.addAttribute("images", letter.images());
        model.addAttribute("reactions", letter.reactions());
        model.addAttribute("publicToken", publicToken);
        model.addAttribute("securityKey", key);
        return "open-letter-and-watch-content";
    }

    /** Отдаёт файл только после проверки ключа и связи изображения с письмом. */
    @GetMapping("/{publicToken}/images/{imageId}")
    @ResponseBody
    public ResponseEntity<Resource> image(
        @PathVariable String publicToken,
        @PathVariable UUID imageId,
        @RequestParam String key
    ) {
        Letter letter = letterRepository
            .findByPublicTokenAndSecurityKeyAndExpiresAtAfter(
                publicToken, key, clock.instant()
            )
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        boolean attached = letter.getImageLinks().stream().anyMatch(link ->
            link.getImage().getId().equals(imageId)
                && link.getImage().getStatus() == ImageStatus.ATTACHED
        );
        if (!attached) {
            throw new ResponseStatusException(NOT_FOUND);
        }

        ImageContent content = imageService.loadOwnedContent(
            letter.getAuthorEmail(), imageId
        );
        MediaType type;
        try {
            type = MediaType.parseMediaType(content.contentType());
        } catch (IllegalArgumentException exception) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
            .contentType(type)
            .contentLength(content.contentLength())
            .cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff")
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline().filename(content.filename()).build().toString()
            )
            .body(content.resource());
    }
        
        
}
    
    
