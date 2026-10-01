package com.example.myproject.Services;

import java.sql.SQLException;
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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.myproject.DTO.CreateLetterResponse;
import com.example.myproject.DTO.DeleteResult;
import com.example.myproject.DTO.EditResult;
import com.example.myproject.DTO.GenPair;
import com.example.myproject.DTO.LetterData;
import com.example.myproject.DTO.UpdateLetterData;
import com.example.myproject.DTO.ValidationLimits;
import com.example.myproject.Images.DTO.ImagePurpose;
import com.example.myproject.Images.Model.Image;
import com.example.myproject.Images.Service.ImageTransactionService;
import com.example.myproject.Model.Letter;
import com.example.myproject.Model.LetterImage;
import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Outbox.Events.LetterCacheInvalidationEvent;
import com.example.myproject.Outbox.Model.OutboxEvent;
import com.example.myproject.Outbox.Repository.OutboxEventRepository;
import com.example.myproject.Repositories.LetterRepository;
import com.example.myproject.Repositories.MyAppUserRepository;
import com.example.myproject.Utils.PublicToken;

@Service
public class LetterTransactionService {
    private static final int MAX_IMAGES = 10;
    private static final int TOKEN_SAVE_MAX_ATTEMPTS = 5;
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    private final Clock clock;
    private final LetterRepository letterRepository;
    private final MyAppUserRepository userRepository;
    private final ImageTransactionService imageTransactions;
    private final OutboxEventRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate createLetterTransaction;

    public LetterTransactionService(
        Clock clock,
        LetterRepository letterRepository,
        MyAppUserRepository userRepository,
        ImageTransactionService imageTransactions,
        OutboxEventRepository outboxRepository,
        ApplicationEventPublisher eventPublisher,
        PlatformTransactionManager transactionManager
    ) {
        this.clock = clock;
        this.letterRepository = letterRepository;
        this.userRepository = userRepository;
        this.imageTransactions = imageTransactions;
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
        this.createLetterTransaction = new TransactionTemplate(
            transactionManager
        );
        this.createLetterTransaction.setPropagationBehavior(
            TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }


    public CreateLetterResponse createLetter(String email, LetterData data) {
        if (email == null || email.isBlank() || !valid(data)) {
            return createError("Invalid data");
        }
        if (!validImageIds(data.imageIds())) {
            return createError("Invalid image list");
        }

        DataIntegrityViolationException lastCollision = null;
        for (int attempt = 0; attempt < TOKEN_SAVE_MAX_ATTEMPTS; attempt++) {
            try {
                return createLetterTransaction.execute(
                    status -> createLetterAttempt(email, data)
                );
            } catch (DataIntegrityViolationException exception) {
                if (!causedByUniqueViolation(exception)) {
                    throw exception;
                }
                lastCollision = exception;
            }
        }

        throw new IllegalStateException(
            "Unable to generate unique letter token",
            lastCollision
        );
    }


    private CreateLetterResponse createLetterAttempt(
        String email,
        LetterData data
    ) {
        MyAppUser user = userRepository.findByEmail(email)
            .orElse(null);
        if (user == null) {
            return createError("User not found");
        }

        Map<UUID, Image> images = lockNewImages(
            data.imageIds(), user.getEmail(), Map.of()
        );

        Letter letter = new Letter();

        GenPair pair = PublicToken.generatePair();
        letter.setPublicToken(pair.publicToken());
        letter.setSecurityKey(pair.securityKey());
        letter.setAuthorEmail(user.getEmail());
        letter.setUser(user);
        letter.setTitle(data.letterTitle().trim());
        letter.setText(data.letterText());
        Instant now = clock.instant();
        letter.setCreatedAt(now);
        letter.setExpiresAt(now.plus(data.ttl(), ChronoUnit.MINUTES));
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

    private boolean causedByUniqueViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException
                && UNIQUE_VIOLATION_SQL_STATE.equals(
                    sqlException.getSQLState()
                )) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }


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
        letter.setFont(data.font());
        letter.setReactions(new ArrayList<>(data.reactions()));

        letterRepository.saveAndFlush(letter);
        queueCacheEviction(publicToken, email, letter.getVersion());
        return EditResult.UPDATED;
    }


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

    private void deleteLetterAndQueueFiles(Letter letter, String email) {
        for (LetterImage link : new ArrayList<>(letter.getImageLinks())) {
            UUID imageId = link.getImage().getId();
            letter.removeImage(link);
            imageTransactions.queueAttachedDeletion(imageId, email);
        }

        long tombstoneVersion = Math.addExact(letter.getVersion(), 1L);
        String token = letter.getPublicToken();
        letterRepository.delete(letter);
        queueCacheEviction(token, email, tombstoneVersion);
    }

    private void queueCacheEviction(String token, String email, long version) {
        Instant now = clock.instant();
        outboxRepository.save(OutboxEvent.evictLetter(
            token, email, version, now
        ));
        eventPublisher.publishEvent(new LetterCacheInvalidationEvent(
            token, email, version
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
            && data.letterTitle().length()
                <= ValidationLimits.LETTER_TITLE_MAX_LENGTH
            && data.letterText() != null && !data.letterText().isBlank()
            && data.letterText().length()
                <= ValidationLimits.LETTER_TEXT_MAX_LENGTH
            && data.ttl() != null && data.ttl() >= 5 && data.ttl() <= 1440;
    }

    private boolean valid(UpdateLetterData data) {
        return data != null
            && data.letterTitle() != null && !data.letterTitle().isBlank()
            && data.letterTitle().length()
                <= ValidationLimits.LETTER_TITLE_MAX_LENGTH
            && data.letterText() != null && !data.letterText().isBlank()
            && data.letterText().length()
                <= ValidationLimits.LETTER_TEXT_MAX_LENGTH
            && data.ttl() != null && data.ttl() >= 5 && data.ttl() <= 1440;
    }

    private CreateLetterResponse createError(String error) {
        return new CreateLetterResponse(null, null, List.of(), error);
    }

}
