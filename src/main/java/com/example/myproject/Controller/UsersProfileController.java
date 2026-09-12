package com.example.myproject.Controller;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.example.myproject.DTO.UserProfileView;
import com.example.myproject.Images.DTO.ImageView;
import com.example.myproject.Services.MyAppUserService;
import com.example.myproject.Services.ProfileImageService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Controller
@RequestMapping("/profile")
public class UsersProfileController {
    private final MyAppUserService userService;
    private final ProfileImageService profileImageService;

    public UsersProfileController(
        MyAppUserService userService,
        ProfileImageService profileImageService
    ) {
        this.userService = userService;
        this.profileImageService = profileImageService;
    }

    @GetMapping("/get")
    public String profilePage(Authentication authentication, Model model) {
        UserProfileView profile = userService.getProfile(
            requireEmail(authentication)
        );

        model.addAttribute("username", profile.username());
        model.addAttribute("email", profile.email());
        model.addAttribute("userHasAvatar", profile.avatar() != null);
        model.addAttribute(
            "avatarPath",
            profile.avatar() == null ? null : profile.avatar().contentUrl()
        );

        return "users-profile";
    }

    @PatchMapping(
        value = "/api",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserProfileView> updateProfile(
        @RequestBody ProfileUpdateRequest request,
        Authentication authentication
    ) {
        UserProfileView profile = userService.updateProfile(
            requireEmail(authentication),
            request.username(),
            request.password()
        );

        return ResponseEntity.ok(profile);
    }

    // Прикрепляет к профилю уже готовое изображение.
    @PutMapping(
        value = "/api/avatar",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ImageView> setAvatar(
        @Valid @RequestBody AvatarRequest request,
        Authentication authentication
    ) {
        ImageView avatar = profileImageService.setAvatar(
            requireEmail(authentication),
            request.imageId()
        );

        return ResponseEntity.ok(avatar);
    }

    // Отвязывает аватар, а удаление файла выполняется в фоне.
    @DeleteMapping("/api/avatar")
    public ResponseEntity<Void> deleteAvatar(Authentication authentication) {
        profileImageService.deleteAvatar(requireEmail(authentication));
        return ResponseEntity.accepted().build();
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null
            || authentication.getName() == null
            || authentication.getName().isBlank()) {
            throw new ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Требуется авторизация"
            );
        }

        return authentication.getName();
    }

    public record ProfileUpdateRequest(
        String username,
        String password
    ) {}

    public record AvatarRequest(
        @NotNull UUID imageId
    ) {}
}
