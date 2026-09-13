package com.example.myproject.Services;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.myproject.DTO.CreateLetterResponse;
import com.example.myproject.DTO.DeleteResult;
import com.example.myproject.DTO.EditResult;
import com.example.myproject.DTO.GenPair;
import com.example.myproject.DTO.LetterData;
import com.example.myproject.DTO.UpdateLetterData;
import com.example.myproject.Images.DTO.ImagePurpose;
import com.example.myproject.Images.Model.Image;
import com.example.myproject.Images.Service.ImageTransactionService;
import com.example.myproject.Model.Letter;
import com.example.myproject.Model.LetterImage;
import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Outbox.Model.OutboxEvent;
import com.example.myproject.Outbox.Repository.OutboxEventRepository;
import com.example.myproject.Repositories.LetterRepository;
import com.example.myproject.Repositories.MyAppUserRepository;
import com.example.myproject.Utils.PublicToken;

@Service
public class LetterTransactionService {
    private static final int MAX_IMAGES = 10;

    private final Clock clock;
    private final LetterRepository letterRepository;
    private final MyAppUserRepository userRepository;
    private final ImageTransactionService imageTransactions;
    private final OutboxEventRepository outboxRepository;

    public LetterTransactionService(
        Clock clock,
        LetterRepository letterRepository,
        MyAppUserRepository userRepository,
        ImageTransactionService imageTransactions,
        OutboxEventRepository outboxRepository
    ) {
        this.clock = clock;
        this.letterRepository = letterRepository;
        this.userRepository = userRepository;
        this.imageTransactions = imageTransactions;
        this.outboxRepository = outboxRepository;
    }

    private GenPair generateUniquePair() {
    for (int i = 0; i < 10; i++) {
        GenPair pair = PublicToken.generatePair();
        if (!letterRepository.existsByPublicToken(pair.publicToken())) {
            return pair;
        }
    }
    throw new IllegalStateException("Unable to generate unique token");
}

    /** Создаёт письмо и привязывает заранее загруженные изображения одной транзакцией. */
    @Transactional
    public CreateLetterResponse createLetter(String email, LetterData data) {
        if (email == null || email.isBlank() || !valid(data)) {
            return createError("Invalid data");
        }
        if (!validImageIds(data.imageIds())) {
            return createError("Invalid image list");
        }

        MyAppUser user = userRepository.findByEmail(email)
            .orElse(null);
        if (user == null) {
            return createError("User not found");
        }

        Map<UUID, Image> images = lockNewImages(
            data.imageIds(), user.getEmail(), Map.of()
        );

        Letter letter = new Letter();

        GenPair pair = generateUniquePair();
        letter.setPublicToken(pair.publicToken());
        letter.setSecurityKey(pair.securityKey());
        letter.setAuthorEmail(user.getEmail());
        letter.setUser(user);
        letter.setTitle(data.letterTitle().trim());
        letter.setText(data.letterText());
        Instant now = clock.instant();
        letter.setCreatedAt(now);
        letter.setExpiresAt(now.plus(data.ttl(), ChronoUnit.MINUTES));
        letter.setBurnAfterOpening(data.burn_after_opening());
        letter.setFont(data.font());
        letter.setReactions(new ArrayList<>(data.reactions()));

        for (int position = 0; position < data.imageIds().size(); position++) {
            Image image = images.get(data.imageIds().get(position));
            image.markAttached();
            letter.addImage(image, position);
        }
        if (!data.imageIds().isEmpty()) {
            letter.incrementImagesRevision();
        }

        letterRepository.saveAndFlush(letter);
        List<String> imageUrls = data.imageIds().stream()
            .map(id -> "/api/images/" + id + "/content")
            .toList();
        return new CreateLetterResponse(
            letter.getPublicToken(), letter.getSecurityKey(), imageUrls, null
        );
    }

    /** Меняет письмо, порядок картинок и очередь удаления одной транзакцией. */
    @Transactional
    public EditResult updateLetter(
        String publicToken,
        String email,
        UpdateLetterData data
    ) {
        if (publicToken == null || publicToken.isBlank()
            || email == null || email.isBlank()
            || !valid(data)
            || !validImageIds(data.imageIds())) {
            return EditResult.INVALID;
        }

        Letter letter = letterRepository.findByPublicTokenForUpdate(publicToken)
            .orElse(null);
        if (letter == null || !letter.getExpiresAt().isAfter(clock.instant())) {
            return EditResult.NOT_FOUND;
        }
        if (!letter.getUser().getEmail().equalsIgnoreCase(email)) {
            return EditResult.FORBIDDEN;
        }
        if (letter.getVersion() != data.expectedVersion()) {
            throw new ObjectOptimisticLockingFailureException(
                Letter.class, letter.getId()
            );
        }

        List<LetterImage> orderedCurrent = new ArrayList<>(letter.getImageLinks());
        orderedCurrent.sort(Comparator.comparingInt(LetterImage::getPosition));
        List<UUID> currentIds = orderedCurrent.stream()
            .map(link -> link.getImage().getId())
            .toList();

        Map<UUID, LetterImage> currentById = new HashMap<>();
        for (LetterImage link : orderedCurrent) {
            currentById.put(link.getImage().getId(), link);
        }
        Map<UUID, Image> desiredImages = lockNewImages(
            data.imageIds(), email, currentById
        );

        List<LetterImage> removed = orderedCurrent.stream()
            .filter(link -> !data.imageIds().contains(link.getImage().getId()))
            .toList();
        for (LetterImage link : removed) {
            UUID imageId = link.getImage().getId();
            letter.removeImage(link);
            imageTransactions.queueAttachedDeletion(imageId, email);
        }

        for (int position = 0; position < data.imageIds().size(); position++) {
            UUID imageId = data.imageIds().get(position);
            LetterImage existing = currentById.get(imageId);
            if (existing != null) {
                existing.setPosition(position);
            } else {
                Image image = desiredImages.get(imageId);
                image.markAttached();
                letter.addImage(image, position);
            }
        }

        if (!currentIds.equals(data.imageIds())) {
            letter.incrementImagesRevision();
        }
        letter.setTitle(data.letterTitle().trim());
        letter.setText(data.letterText());
        letter.setExpiresAt(clock.instant().plus(data.ttl(), ChronoUnit.MINUTES));
        letter.setBurnAfterOpening(data.burn_after_opening());
        letter.setFont(data.font());
        letter.setReactions(new ArrayList<>(data.reactions()));

        letterRepository.saveAndFlush(letter);
        outboxRepository.save(OutboxEvent.evictLetter(
            publicToken, email, letter.getVersion(), clock.instant()
        ));
        return EditResult.UPDATED;
    }

    /** Удаляет письмо в БД, а физические файлы передаёт фоновой очереди. */
    @Transactional
    public DeleteResult deleteLetter(String publicToken, String email) {
        if (publicToken == null || publicToken.isBlank()
            || email == null || email.isBlank()) {
            return DeleteResult.INVALID;
        }

        Letter letter = letterRepository.findByPublicTokenForUpdate(publicToken)
            .orElse(null);
        if (letter == null) {
            return DeleteResult.NOT_FOUND;
        }
        if (!letter.getUser().getEmail().equalsIgnoreCase(email)) {
            return DeleteResult.FORBIDDEN;
        }

        deleteLetterAndQueueFiles(letter, email);
        return DeleteResult.DELETED;
    }

    /** Удаляет самоисчезающее письмо только в ожидаемой версии. */
    @Transactional
    public boolean burnLetter(String publicToken, String email, long version) {
        Letter letter = letterRepository.findByPublicTokenForUpdate(publicToken)
            .orElse(null);
        if (letter == null
            || letter.getVersion() != version
            || !letter.isBurnAfterOpening()
            || !letter.getUser().getEmail().equalsIgnoreCase(email)) {
            return false;
        }

        deleteLetterAndQueueFiles(letter, email);
        return true;
    }

    private void deleteLetterAndQueueFiles(Letter letter, String email) {
        for (LetterImage link : new ArrayList<>(letter.getImageLinks())) {
            UUID imageId = link.getImage().getId();
            letter.removeImage(link);
            imageTransactions.queueAttachedDeletion(imageId, email);
        }

        long tombstoneVersion = Math.addExact(letter.getVersion(), 1L);
        String token = letter.getPublicToken();
        letterRepository.delete(letter);
        outboxRepository.save(OutboxEvent.evictLetter(
            token, email, tombstoneVersion, clock.instant()
        ));
    }

    private Map<UUID, Image> lockNewImages(
        List<UUID> desiredIds,
        String email,
        Map<UUID, LetterImage> current
    ) {
        Map<UUID, Image> result = new HashMap<>();
        List<UUID> toLock = desiredIds.stream()
            .filter(id -> !current.containsKey(id))
            .sorted()
            .toList();
        for (UUID imageId : toLock) {
            result.put(imageId, imageTransactions.requireReadyForAttachment(
                imageId, email, ImagePurpose.LETTER
            ));
        }
        return result;
    }

    private boolean validImageIds(List<UUID> ids) {
        return ids != null
            && ids.size() <= MAX_IMAGES
            && ids.stream().noneMatch(java.util.Objects::isNull)
            && new HashSet<>(ids).size() == ids.size();
    }

    private boolean valid(LetterData data) {
        return data != null
            && data.letterTitle() != null && !data.letterTitle().isBlank()
            && data.letterText() != null && !data.letterText().isBlank()
            && data.ttl() != null && data.ttl() >= 5 && data.ttl() <= 1440;
    }

    private boolean valid(UpdateLetterData data) {
        return data != null
            && data.letterTitle() != null && !data.letterTitle().isBlank()
            && data.letterText() != null && !data.letterText().isBlank()
            && data.ttl() != null && data.ttl() >= 5 && data.ttl() <= 1440;
    }

    private CreateLetterResponse createError(String error) {
        return new CreateLetterResponse(null, null, List.of(), error);
    }

    // private String generateUniqueToken() {
    //     for (int attempt = 0; attempt < 5; attempt++) {
    //         String token = PublicToken.generatePublicToken();
    //         if (!letterRepository.existsByPublicToken(token)) {
    //             return token;
    //         }
    //     }
    //     throw new IllegalStateException("Cannot generate a unique letter token");
    // }
}
