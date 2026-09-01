package com.example.myproject.Services;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.myproject.Component.LetterMapper;
import com.example.myproject.DTO.CachedWrite;
import com.example.myproject.DTO.CreateLetterResponse;
import com.example.myproject.DTO.DeleteResult;
import com.example.myproject.DTO.EditResult;
import com.example.myproject.DTO.LetterSummaryView;
import com.example.myproject.DTO.FontData;
import com.example.myproject.DTO.LetterData;
import com.example.myproject.DTO.LetterPage;
import com.example.myproject.DTO.LetterView;
import com.example.myproject.DTO.UpdateLetterData;
import com.example.myproject.Model.CachedLetter;
import com.example.myproject.Model.Letter;
import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Model.OutboxEvent;
import com.example.myproject.Projection.ActiveLetterRef;
import com.example.myproject.Repositories.LetterRepository;
import com.example.myproject.Repositories.MyAppUserRepository;
import com.example.myproject.Repositories.OutboxEventRepository;
import com.example.myproject.Repositories.RedisRepository;
import com.example.myproject.Utils.PublicToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LetterService {
    private LetterRepository letterRepository;
    private RedisRepository redisRepository;
    private MyAppUserRepository userRepository;
    private PasswordEncoder encoder;
    private OutboxEventRepository outboxEventRepository;
    private Clock clock;
    private LetterMapper letterMapper;

    private static final Duration MAX_CACHE_TTL = Duration.ofHours(24);
    private static final Logger log = LoggerFactory.getLogger(LetterService.class);
    
    public LetterService(LetterRepository letterRepository, RedisRepository redisRepository, MyAppUserRepository userRepository, PasswordEncoder encoder, Clock clock, OutboxEventRepository outboxEventRepository, LetterMapper letterMapper){
        this.letterRepository = letterRepository;
        this.redisRepository = redisRepository;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.clock = clock;
        this.outboxEventRepository = outboxEventRepository;
        this.letterMapper = letterMapper;
    }

    public LetterPage getLetters(String email, Long beforeId, int pageSize) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email пользователя не указан");
        }

        int limit = Math.max(1, Math.min(pageSize, 100));
        Instant now = clock.instant();


        List<ActiveLetterRef> refs = letterRepository.findActiveLetterRefs(email, now, beforeId, PageRequest.of(0, limit));

        if (refs.isEmpty()) {
            return new LetterPage(List.of(), null);
        }

        List<String> tokens = new ArrayList<>(refs.size());
        for (ActiveLetterRef ref : refs) {
            if (ref == null) {
                throw new IllegalStateException("Repository returned null ActiveLetterRef");
            }

            String publicToken = ref.getPublicToken();
            if (publicToken == null) {
                throw new IllegalStateException("Letter public token is null");
            }
            tokens.add(publicToken);
        }


        
        Map<String, CachedLetter> cachedByToken; // сохраняем сюда письма из кэша

        try {
            cachedByToken = redisRepository.getLettersByTokens(tokens);
        } catch (DataAccessException exception) {
            log.warn("Redis MGET failed", exception);
            cachedByToken = Map.of();
        }

        Map<String, LetterSummaryView> gainedTokens = new HashMap<>();
        List<String> missingTokens = new ArrayList<>();
        
        // находим невошедшие в кэш токены
        for (ActiveLetterRef ref : refs) {
            CachedLetter cached = cachedByToken.get(ref.getPublicToken());

            boolean valid = cached != null && cached.version() == ref.getVersion() && cached.expiresAt() != null && cached.expiresAt().isAfter(now) && email.equalsIgnoreCase(cached.authorEmail());

            if (valid) {
                gainedTokens.put(ref.getPublicToken(), letterMapper.toSummaryView(cached));
            } else{
                missingTokens.add(ref.getPublicToken());
            }
        }

        // Filling cache miss
        List<CachedWrite> needToAddToCache = new ArrayList<>();

        if (!missingTokens.isEmpty()) {
            List<Letter> loaded = letterRepository.findActiveByTokens(
                missingTokens, email, now
            );
            
            for (Letter letter : loaded) {
                gainedTokens.put(letter.getPublicToken(), letterMapper.toSummaryView(letter));

                Duration ttl = calculateTtl(letter.getExpiresAt());

                if (ttl.isPositive()) {
                    needToAddToCache.add(new CachedWrite(letterMapper.toCached(letter),ttl));
                }
            }
        }

        try {
            redisRepository.putLetters(needToAddToCache);
        } catch (DataAccessException exception) {
            log.warn("Redis pipeline write failed", exception);
        }
        
        List<LetterSummaryView> ordered = new ArrayList<>();
        for (ActiveLetterRef ref : refs) {
            LetterSummaryView view = gainedTokens.get(ref.getPublicToken());

            if (view != null) {
                ordered.add(view);
            }
        }

        Long nextBeforeId = refs.get(refs.size() - 1).getId();

        return new LetterPage(ordered, nextBeforeId);


    }

    private void burnLetter(String publicToken,  String email){
        redisRepository.evictLetter(publicToken, email);
        letterRepository.deleteByPublicToken(publicToken);

    }

    private Duration calculateTtl(Instant expiresAt) {
        Duration remaining = Duration.between(clock.instant(), expiresAt);
        if (remaining.isNegative() || remaining.isZero()) {
            return Duration.ZERO;
        }

        return remaining.compareTo(MAX_CACHE_TTL) < 0 ? remaining : MAX_CACHE_TTL;
    }
    
    public Optional<LetterView> getLetterToAnonymous(String publicToken, String key) {
        if (publicToken == null || publicToken.isBlank() || key == null || key.isBlank() ) {
            throw new IllegalArgumentException("Ошибка, не указан public token или ключ");
        }
        Instant now = clock.instant() ;
        try {
            Optional<CachedLetter> cached = redisRepository.findLetter(publicToken);

            if (cached.isPresent()) {
                CachedLetter cachedLetter = cached.get();
                if (cachedLetter.expiresAt().isAfter(now) && cachedLetter.securityKey().equals(key)) {
                    if (cachedLetter.burnAfterOpening()) {
                        burnLetter(publicToken, cachedLetter.authorEmail());
                    }
                    return Optional.of(letterMapper.toView(cachedLetter));
                }
            }
        } catch (DataAccessException exception) {
            log.warn("Redis read failed", exception);
        }

        Optional<Letter> foundLetter = letterRepository.findByPublicTokenAndSecurityKeyAndExpiresAtAfter(publicToken, key ,now);

        if (foundLetter.isEmpty()) {
            return Optional.empty();
        }

        Letter letter = foundLetter.get();

        // adding letter to cache
        CachedLetter cached = letterMapper.toCached(letter); 

        Duration remaining = Duration.between(now, letter.getExpiresAt());

        Duration cacheTtl = remaining.compareTo(MAX_CACHE_TTL) < 0 ? remaining : MAX_CACHE_TTL;

        if (!cacheTtl.isNegative() && !cacheTtl.isZero()){
            try{
                redisRepository.putLetter(cached, cacheTtl);
            } catch (DataAccessException exception) {
                log.warn("Redis write failed", exception);
            }
        }

        if (cached.burnAfterOpening()) {
            burnLetter(publicToken, cached.authorEmail());
        }
        

        return Optional.of(letterMapper.toView(cached));

    }
    public Optional<LetterView> getLetter(String publicToken, String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email пользователя не указан");
        }

        if (publicToken == null || publicToken.isBlank()) {
            throw new IllegalArgumentException("public token письма не указан");
        }
        Instant now = clock.instant() ;
        try {
            Optional<CachedLetter> cached = redisRepository.findLetter(publicToken);

            if (cached.isPresent()) {
                CachedLetter cachedLetter = cached.get();
                if (cachedLetter.expiresAt().isAfter(now) && cachedLetter.authorEmail().equals(email)) {
                    return Optional.of(letterMapper.toView(cachedLetter));
                }
            }
        } catch (DataAccessException exception) {
            log.warn("Redis read failed", exception);
        }

        Optional<Letter> foundLetter = letterRepository.findByPublicTokenAndAuthorEmailIgnoreCaseAndExpiresAtAfter(publicToken, email,now);

        if (foundLetter.isEmpty()) {
            return Optional.empty();
        }

        Letter letter = foundLetter.get();

        // adding letter to cache
        CachedLetter cached = letterMapper.toCached(letter); 

        Duration remaining = Duration.between(now, letter.getExpiresAt());

        Duration cacheTtl = remaining.compareTo(MAX_CACHE_TTL) < 0 ? remaining : MAX_CACHE_TTL;

        if (!cacheTtl.isNegative() && !cacheTtl.isZero()){
            try{
                redisRepository.putLetter(cached, cacheTtl);
            } catch (DataAccessException exception) {
                log.warn("Redis write failed", exception);
            }
        }

        return Optional.of(letterMapper.toView(cached));
    }

    @Transactional
    public EditResult updateLetter(String publicToken, String email, UpdateLetterData updateData) {

        if (email == null || email.isBlank() || updateData == null || updateData.reactions().isEmpty() || updateData.ttl() == null ||
    updateData.ttl() < 5 || updateData.ttl() > 1440 || updateData.letterText() == null || updateData.letterText().isBlank() ||updateData.letterTitle() == null || updateData.letterTitle().isBlank() ) {
            return EditResult.INVALID;
        }
        

        Letter letter = letterRepository.findByPublicToken(publicToken).orElse(null);
        if (letter == null) {
            return EditResult.NOT_FOUND;
        }

        if (!letter.getAuthorEmail().equalsIgnoreCase(email)) {
            return EditResult.FORBIDDEN;
        }


        letter.setText(updateData.letterText());
        letter.setTitle(updateData.letterTitle());

        if (updateData.password() != null){
            letter.setPassword(encoder.encode(updateData.password()));

        }
        
        letter.setExpiresAt(clock.instant().plus(updateData.ttl(), ChronoUnit.MINUTES));

        letterRepository.save(letter);


       
        outboxEventRepository.save(
            OutboxEvent.evictLetter(publicToken, email, clock.instant())
        );

        
        return EditResult.UPDATED;
    }

    @Transactional
    public CreateLetterResponse createLetter(String email, LetterData createData) {
        if (email == null || email.isBlank() || createData == null || createData.reactions().isEmpty() || createData.ttl() == null ||
    createData.ttl() < 5 || createData.ttl() > 1440 || createData.letterText() == null || createData.letterText().isBlank() ||createData.letterTitle() == null || createData.letterTitle().isBlank() ) {

            return new CreateLetterResponse(null, "Invalid data");
        }

        Letter letter = new Letter();
        String publicToken = PublicToken.generatePublicToken();
        String password = createData.password();

        String securityKey = PublicToken.generateSecurityKey();
        String title = createData.letterTitle();
        String text = createData.letterText();
        if (text.isBlank() || text.isEmpty()) {
            return new CreateLetterResponse(null, "Empty letter");

        }

        MyAppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return new CreateLetterResponse(null, "User not found");
        }

        Integer ttl = createData.ttl();
        Boolean burnAfterOpening = createData.burn_after_opening();
        List<String> imagesPaths = createData.images();
        List<String> reactions = createData.reactions();
        
        // font settings
        Boolean bold = createData.font().isFontBold();
        Boolean cursive = createData.font().isFontCursive();
        Boolean underlined = createData.font().isFontUnderlined();
        String family = createData.font().fontFamily();
        String name = createData.font().fontName();
        
        FontData font = new FontData(bold, cursive, underlined, family, name);

        letter.setAuthorEmail(email);
        letter.setPublicToken(publicToken);
        letter.setSecurityKey(securityKey);
        if (!password.isEmpty()) {
            letter.setPassword(encoder.encode(password));
        }else{
            letter.setPassword(null);
        }
        letter.setTitle(title);
        letter.setText(text);
        
        letter.setUser(user);
        
        letter.setBurn_after_opening(burnAfterOpening);
        letter.setImagesPaths(imagesPaths);
        letter.setFont(font);
        letter.setReactions(reactions);
        Instant expiresAt = clock.instant().plus(ttl, ChronoUnit.MINUTES);
        letter.setExpiresAt(expiresAt);
        letterRepository.save(letter);

        return new CreateLetterResponse(publicToken, null);
    }

    @Transactional
    public DeleteResult deleteLetter(String publicToken, String email) {
        
        if (publicToken == null || publicToken.isEmpty()|| email == null || email.isBlank()){
            return DeleteResult.INVALID;
        }
        

        Letter letter = letterRepository.findByPublicToken(publicToken).orElse(null);
        if (letter == null) {
            return DeleteResult.NOT_FOUND;
        }

        if (!letter.getAuthorEmail().equalsIgnoreCase(email)) {
            return DeleteResult.FORBIDDEN;
        }

        letterRepository.delete(letter);

        outboxEventRepository.save(
            OutboxEvent.evictLetter(publicToken, email, clock.instant())
        );

        return DeleteResult.DELETED;
    }

    
}   