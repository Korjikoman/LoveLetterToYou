package com.example.myproject.Services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.myproject.Images.DTO.ImagePurpose;
import com.example.myproject.Images.DTO.ImageView;
import com.example.myproject.Images.Model.Image;
import com.example.myproject.Images.Service.ImageTransactionService;
import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Repositories.MyAppUserRepository;

@Service
public class ProfileImageService {
    private final MyAppUserRepository userRepository;
    private final ImageTransactionService imageTransactions;

    public ProfileImageService(
        MyAppUserRepository userRepository,
        ImageTransactionService imageTransactions
    ) {
        this.userRepository = userRepository;
        this.imageTransactions = imageTransactions;
    }

    /** Атомарно меняет ссылку на аватар и ставит старый файл на удаление. */
    @Transactional
    public ImageView setAvatar(String email, UUID imageId) {
        MyAppUser user = lockUser(email);
        Image previous = user.getAvatarImage();
        if (previous != null && previous.getId().equals(imageId)) {
            return toView(previous);
        }

        Image replacement = imageTransactions.requireReadyForAttachment(
            imageId, email, ImagePurpose.AVATAR
        );
        replacement.markAttached();
        user.setAvatarImage(replacement);

        if (previous != null) {
            imageTransactions.queueAttachedDeletion(previous.getId(), email);
        }
        return toView(replacement);
    }

    @Transactional
    public void deleteAvatar(String email) {
        MyAppUser user = lockUser(email);
        Image previous = user.getAvatarImage();
        if (previous == null) {
            return;
        }

        user.setAvatarImage(null);
        imageTransactions.queueAttachedDeletion(previous.getId(), email);
    }

    private MyAppUser lockUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }
        return userRepository.findByEmailForUpdate(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private ImageView toView(Image image) {
        return new ImageView(
            image.getId(), "/api/images/" + image.getId() + "/content", 0
        );
    }
}
