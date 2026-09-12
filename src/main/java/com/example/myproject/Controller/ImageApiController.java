package com.example.myproject.Controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.myproject.Images.DTO.ImageContent;
import com.example.myproject.Images.DTO.ImageIsReadyResponse;
import com.example.myproject.Images.DTO.ImagePurpose;
import com.example.myproject.Images.DTO.ImageUploadResponse;
import com.example.myproject.Images.Service.ImageService;

@RestController
@RequestMapping("/api/images")
public class ImageApiController {
    private final ImageService imageService;

    public ImageApiController(ImageService imageService) {
        this.imageService = imageService;
    }

    // Регистрирует загрузку и ставит перенос файла в очередь.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> upload(
        @RequestParam ImagePurpose purpose,
        @RequestPart("file") MultipartFile file,
        Authentication authentication
    ) {
        ImageUploadResponse response = imageService.upload(
            requireEmail(authentication),
            purpose,
            file
        );

        return ResponseEntity
            .accepted()
            .location(statusUri(response.imageId()))
            .body(response);
    }

    // Возвращает состояние асинхронной обработки изображения.
    @GetMapping("/{imageId}/status")
    public ResponseEntity<ImageIsReadyResponse> status(
        @PathVariable UUID imageId,
        Authentication authentication
    ) {
        ImageIsReadyResponse response = imageService.getIsReadyResponse(
            requireEmail(authentication),
            imageId
        );

        return ResponseEntity.ok(response);
    }

    // Отдаёт только готовый файл, принадлежащий текущему пользователю.
    @GetMapping("/{imageId}/content")
    public ResponseEntity<Resource> content(
        @PathVariable UUID imageId,
        Authentication authentication
    ) {
        ImageContent content = imageService.loadOwnedContent(
            requireEmail(authentication),
            imageId
        );

        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(content.contentType());
        } catch (IllegalArgumentException exception) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
            .contentType(contentType)
            .contentLength(content.contentLength())
            .cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff")
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline()
                    .filename(content.filename())
                    .build()
                    .toString()
            )
            .body(content.resource());
    }

    // Ставит удаление файла в очередь; повторный вызов безопасен.
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> cancel(
        @PathVariable UUID imageId,
        Authentication authentication
    ) {
        imageService.cancel(
            requireEmail(authentication),
            imageId
        );

        return ResponseEntity
            .accepted()
            .location(statusUri(imageId))
            .build();
    }

    private URI statusUri(UUID imageId) {
        return URI.create("/api/images/" + imageId + "/status");
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication.getName() == null
            || authentication.getName().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Требуется авторизация"
            );
        }

        return authentication.getName();
    }
}
