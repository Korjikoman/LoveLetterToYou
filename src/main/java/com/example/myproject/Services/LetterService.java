package com.example.myproject.Services;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.myproject.Component.LetterMapper;
import com.example.myproject.DTO.CachedWrite;
import com.example.myproject.DTO.CreateLetterResponse;
import com.example.myproject.DTO.DeleteResult;
import com.example.myproject.DTO.EditResult;
import com.example.myproject.DTO.LetterData;
import com.example.myproject.DTO.LetterPage;
import com.example.myproject.DTO.LetterSummaryView;
import com.example.myproject.DTO.LetterView;
import com.example.myproject.DTO.UpdateLetterData;
import com.example.myproject.Model.CachedLetter;
import com.example.myproject.Model.Letter;
import com.example.myproject.Projection.ActiveLetterRef;
import com.example.myproject.Repositories.LetterRepository;
import com.example.myproject.Repositories.RedisRepository;

@Service
public class LetterService {
    private static final Duration MAX_CACHE_TTL = Duration.ofHours(24);
    private static final Logger log = LoggerFactory.getLogger(LetterService.class);

    private final LetterRepository letterRepository;
    private final RedisRepository redisRepository;
    private final Clock clock;
    private final LetterMapper letterMapper;
    private final LetterTransactionService transactions;

    public LetterService(
        LetterRepository letterRepository,
        RedisRepository redisRepository,
        Clock clock,
        LetterMapper letterMapper,
        LetterTransactionService transactions
    ) {
        this.letterRepository = letterRepository;
        this.redisRepository = redisRepository;
        this.clock = clock;
        this.letterMapper = letterMapper;
        this.transactions = transactions;
    }

    /** БД задаёт страницу и порядок, Redis ускоряет получение её содержимого. */
    public LetterPage getLetters(String email, Long beforeId, int pageSize) {
        requireEmail(email);
        int limit = Math.max(1, Math.min(pageSize, 100));
        Instant now = clock.instant();

        List<ActiveLetterRef> fetched = letterRepository.findActiveLetterRefs(
            email, now, beforeId, PageRequest.of(0, limit + 1)
        );
        boolean hasNext = fetched.size() > limit;
        List<ActiveLetterRef> refs = hasNext
            ? List.copyOf(fetched.subList(0, limit))
            : List.copyOf(fetched);
        if (refs.isEmpty()) {
            return new LetterPage(List.of(), null);
        }

        List<String> tokens = new ArrayList<>(refs.size());
        for (ActiveLetterRef ref : refs) {
            if (ref == null || ref.getPublicToken() == null) {
                throw new IllegalStateException("Repository returned invalid letter reference");
            }
            tokens.add(ref.getPublicToken());
        }

        Map<String, CachedLetter> cachedByToken;
        try {
            cachedByToken = redisRepository.getLettersByTokens(tokens);
        } catch (DataAccessException | IllegalStateException exception) {
            log.warn("Redis batch read failed", exception);
            cachedByToken = Map.of();
        }

        Map<String, LetterSummaryView> resultByToken = new HashMap<>();
        List<String> missingTokens = new ArrayList<>();
        for (ActiveLetterRef ref : refs) {
            CachedLetter cached = cachedByToken.get(ref.getPublicToken());
            boolean valid = cached != null
                && Objects.equals(cached.version(), ref.getVersion())
                && cached.expiresAt() != null
                && cached.expiresAt().isAfter(now)
                && email.equalsIgnoreCase(cached.authorEmail());

            if (valid) {
                resultByToken.put(
                    ref.getPublicToken(), letterMapper.toSummaryView(cached)
                );
            } else {
                missingTokens.add(ref.getPublicToken());
            }
        }

        List<CachedWrite> cacheWrites = new ArrayList<>();
        if (!missingTokens.isEmpty()) {
            List<Letter> loaded = letterRepository.findActiveByTokens(
                missingTokens, email, now
            );
            for (Letter letter : loaded) {
                resultByToken.put(
                    letter.getPublicToken(), letterMapper.toSummaryView(letter)
                );
                Duration ttl = calculateTtl(letter.getExpiresAt(), now);
                if (ttl.isPositive()) {
                    cacheWrites.add(new CachedWrite(letterMapper.toCached(letter), ttl));
                }
            }
        }

        if (!cacheWrites.isEmpty()) {
            try {
                redisRepository.putLetters(cacheWrites);
            } catch (DataAccessException | IllegalStateException exception) {
                log.warn("Redis batch write failed", exception);
            }
        }

        List<LetterSummaryView> ordered = new ArrayList<>(refs.size());
        for (ActiveLetterRef ref : refs) {
            LetterSummaryView view = resultByToken.get(ref.getPublicToken());
            if (view != null) {
                ordered.add(view);
            }
        }

        Long nextBeforeId = hasNext ? refs.getLast().getId() : null;
        return new LetterPage(List.copyOf(ordered), nextBeforeId);
    }

    public Optional<LetterView> getLetter(String publicToken, String email) {
        requireEmail(email);
        requireToken(publicToken);
        Instant now = clock.instant();

        Optional<CachedLetter> cached = readCache(publicToken);
        if (cached.isPresent()) {
            CachedLetter letter = cached.get();
            if (letter.expiresAt() != null
                && letter.expiresAt().isAfter(now)
                && email.equalsIgnoreCase(letter.authorEmail())) {
                return Optional.of(letterMapper.toView(letter));
            }
        }

        Optional<Letter> found = letterRepository
            .findByPublicTokenAndAuthorEmailIgnoreCaseAndExpiresAtAfter(
                publicToken, email, now
            );
        return found.map(letter -> cacheAndMap(letter, now));
    }

    public Optional<LetterView> getLetterToAnonymous(
        String publicToken,
        String securityKey
    ) {
        requireToken(publicToken);
        if (securityKey == null || securityKey.isBlank()) {
            throw new IllegalArgumentException("Letter key is required");
        }
        Instant now = clock.instant();

        Optional<CachedLetter> cached = readCache(publicToken);
        if (cached.isPresent()) {
            CachedLetter letter = cached.get();
            if (letter.expiresAt() != null
                && letter.expiresAt().isAfter(now)
                && securityKey.equals(letter.securityKey())) {
                if (!letter.burnAfterOpening()
                    || transactions.burnLetter(
                        publicToken, letter.authorEmail(), letter.version()
                    )) {
                    return Optional.of(letterMapper.toView(letter));
                }
                return Optional.empty();
            }
        }

        Optional<Letter> found = letterRepository
            .findByPublicTokenAndSecurityKeyAndExpiresAtAfter(
                publicToken, securityKey, now
            );
        if (found.isEmpty()) {
            return Optional.empty();
        }

        Letter letter = found.get();
        LetterView view = cacheAndMap(letter, now);
        if (letter.isBurnAfterOpening()
            && !transactions.burnLetter(
                publicToken, letter.getAuthorEmail(), letter.getVersion()
            )) {
            return Optional.empty();
        }
        return Optional.of(view);
    }

    public CreateLetterResponse createLetter(String email, LetterData data) {
        return transactions.createLetter(email, data);
    }

    public EditResult updateLetter(
        String publicToken,
        String email,
        UpdateLetterData data
    ) {
        return transactions.updateLetter(publicToken, email, data);
    }

    public DeleteResult deleteLetter(String publicToken, String email) {
        return transactions.deleteLetter(publicToken, email);
    }

    private LetterView cacheAndMap(Letter letter, Instant now) {
        CachedLetter cached = letterMapper.toCached(letter);
        Duration ttl = calculateTtl(letter.getExpiresAt(), now);
        if (ttl.isPositive()) {
            try {
                redisRepository.putLetter(cached, ttl);
            } catch (DataAccessException | IllegalStateException exception) {
                log.warn("Redis write failed", exception);
            }
        }
        return letterMapper.toView(cached);
    }

    private Optional<CachedLetter> readCache(String publicToken) {
        try {
            return redisRepository.findLetter(publicToken);
        } catch (DataAccessException | IllegalStateException exception) {
            log.warn("Redis read failed", exception);
            return Optional.empty();
        }
    }

    private Duration calculateTtl(Instant expiresAt, Instant now) {
        Duration remaining = Duration.between(now, expiresAt);
        if (!remaining.isPositive()) {
            return Duration.ZERO;
        }
        return remaining.compareTo(MAX_CACHE_TTL) < 0
            ? remaining
            : MAX_CACHE_TTL;
    }

    private void requireEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }
    }

    private void requireToken(String publicToken) {
        if (publicToken == null || publicToken.isBlank()) {
            throw new IllegalArgumentException("Letter token is required");
        }
    }
}
