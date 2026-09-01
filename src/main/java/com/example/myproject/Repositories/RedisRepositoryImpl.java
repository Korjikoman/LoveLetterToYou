package com.example.myproject.Repositories;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import com.example.myproject.DTO.CachedWrite;
import com.example.myproject.DTO.FontData;
import com.example.myproject.Model.CachedLetter;
import com.example.myproject.Model.FontSettings;
import com.example.myproject.Model.Letter;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Repository
public class RedisRepositoryImpl implements RedisRepository {


    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, Object> hashOperations;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(RedisRepositoryImpl.class);

    private final int TIME_TO_LIVE_USER_ONLINE = 5; 
    private final int TIME_TO_LIVE_USER_WRITING_LETTER = 5; // in secs

    private static final String COUNT_ONLINE_USERS = "online_users";
    private static final String COUNT_WRITING_LETTER_USERS = "writing_letter_users";

    public RedisRepositoryImpl(RedisTemplate<String, Object> template, ObjectMapper objectMapper){
        this.redisTemplate = template;
        this.hashOperations = template.opsForHash();
        this.objectMapper = objectMapper;
    }


    @Override
    public Boolean evictLetter(String publicToken, String email){
        if (publicToken == null || email == null) {
            return false;
        }
        String key = "cache:letter:" + publicToken;


        RedisScript<Long> DELETE_LETTER_SCRIPT = RedisScript.of(
            """
            
            local value = redis.call('GET', KEYS[1])
            
            if not value then
                return 0
            end
            local data = cjson.decode(value)

            local owner = data.authorEmail;

            if not owner then
                return -1;
            end
            
            redis.call('DEL', KEYS[1])
            redis.call('SREM', KEYS[2], KEYS[1])

            return 1

            """, Long.class
        );

        Long result = redisTemplate.execute(
            DELETE_LETTER_SCRIPT,
            List.of(key, email),
            email
        );

        if (result == null){
            return false;
        }
        if (result.intValue() == 1) {
            return true;
        }
        
        return false;
    }


    @Override
    public boolean isUserOnline(String email) {
        Map<String, Object> data = hashOperations.entries(email);
        if (data.isEmpty()){
            return false;
        }
        String isOnline = (String) data.get("isOnline");

        return Boolean.parseBoolean(isOnline);
    }

    @Override
    public boolean isUserWritingLetter(String email) {
        Map<String, Object> data = hashOperations.entries(email);
        if (data.isEmpty()){
            return false;
        }
        String isWritingLetter = (String) data.get("isWritingLetter");

        return Boolean.parseBoolean(isWritingLetter);
    }

    @Override
    public void setUserOnline(String email, boolean isOnline) {
        if (email.isEmpty()) return;

        String key = "user:online:"+email;


        String script = String.format("""
                redis.call('SET', KEYS[1], '%b', 'EX', ARGV[1])
                return 1
                """, isOnline);

        RedisScript<Long> redisScript = RedisScript.of(script, Long.class);

        redisTemplate.execute(
            redisScript,
            List.of(key),
            String.valueOf(TIME_TO_LIVE_USER_ONLINE)
        );

        if (isOnline){
            redisTemplate.opsForSet().add(COUNT_ONLINE_USERS, email);
        }
        else{
            redisTemplate.opsForSet().remove(COUNT_ONLINE_USERS, email);
        }

        
    }

    @Override
    public void setUserWritingLetter(String email, boolean isWritingLetter) {
        if (email.isEmpty()) return;

        String key = "user:writing:"+email;


        String script = String.format("""
                redis.call('SET', KEYS[1], '%b', 'EX', ARGV[1])
                return 1
                """, isWritingLetter);

        RedisScript<Long> redisScript = RedisScript.of(script, Long.class);

        redisTemplate.execute(
            redisScript,
            List.of(key),
            String.valueOf(TIME_TO_LIVE_USER_WRITING_LETTER)
        );

        if (isWritingLetter){
            redisTemplate.opsForSet().add(COUNT_WRITING_LETTER_USERS, email);
        }
        else{
            redisTemplate.opsForSet().remove(COUNT_WRITING_LETTER_USERS, email);
        }

    }

    @Override
    public long countOnlineUsers() {
        return redisTemplate.opsForSet().size(COUNT_ONLINE_USERS);
    }


    @Override
    public long countWritingLetterUsers() {
        return redisTemplate.opsForSet().size(COUNT_WRITING_LETTER_USERS);
    }


    @Override
    public void updateUserOnline(String email) {
        if (email.isBlank() || email.isEmpty()) return;
        redisTemplate.expire("user:online:"+email, TIME_TO_LIVE_USER_ONLINE, TimeUnit.SECONDS);
    }

    @Override
    public void updateUserWritingLetter(String email) {
        if (email.isBlank() || email.isEmpty()) return;
        redisTemplate.expire("user:writing:"+email, TIME_TO_LIVE_USER_WRITING_LETTER, TimeUnit.SECONDS);
    }

    @Override
    public Map<Object, Object> dumpTestData() {
    Map<Object, Object> result = new HashMap<>();
    Set<String> keys = redisTemplate.keys("*");
    if (keys == null) return result;

    for (String key : keys) {
        String type = redisTemplate.type(key).code(); // получаем тип ключа

        switch (type) {
            case "string":
                result.put(key, redisTemplate.opsForValue().get(key));
                break;
            case "hash":
                result.put(key, redisTemplate.opsForHash().entries(key));
                break;
            case "list":
                result.put(key, redisTemplate.opsForList().range(key, 0, -1));
                break;
            case "set":
                result.put(key, redisTemplate.opsForSet().members(key));
                break;
            case "zset":
                result.put(key, redisTemplate.opsForZSet().rangeWithScores(key, 0, -1));
                break;
            default:
                result.put(key, "Unsupported type: " + type);
        }
    }

    return result;
}



    @Override
    public void putLetters(List<CachedWrite> cachedWrites) {

        List<String> keys = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        RedisScript<Long> redisScript = RedisScript.of(
            """
                for i = 1, #KEYS do
                    local json = ARGV[(i - 1) * 2 + 1]
                    local ttl = ARGV[(i - 1) * 2 + 2]
                    if ttl <= 0 do
                        goto continue
                    end
                    
                    redis.call('SET', KEYS[i], json, 'EX', ttl)

                    ::continue::
                end
            """, Long.class
        );

        for (CachedWrite write : cachedWrites) {
            CachedLetter cached = write.cachedLetter();

            if (cached == null || cached.publicToken() == null || ) {
                continue;
            }

            Duration ttl = write.ttl();

            if (ttl.isNegative() || ttl.isZero() || ttl == null) {
                continue;
            }

            try {
                String json = objectMapper.writeValueAsString(cached);
                keys.add("cache:letter" + cached.publicToken());
                args.add(json);
                args.add(String.valueOf(ttl.getSeconds()));
            } catch (JSONException e) {
                throw new IllegalStateException("Cannot serialize CachedLetter", e);
            }

        }

        if (keys.isEmpty()) return;

        redisTemplate.execute(redisScript, keys, args);
    }

    @Override
    public Map<String, CachedLetter> getLettersByTokens(List<String> tokens) {
        if (tokens == null || tokens.isEmpty() ){
            return Map.of();
        }

    
        RedisScript<String> script = RedisScript.of("""
                keys = redis.call('HVALS', KEYS[1])

                local result = {}
                for i = 1, #keys do 
                    local key = keys[i]
                    local value = redis.call('GET', key)

                    if value then
                        result[key] = cjson.decode(value)
                    end
                end

                return cjison.encode(result)

                """, String.class);
        

        String json = redisTemplate.execute(script,tokens);


        Map<String, CachedLetter> cachedLetters = objectMapper.readValue(json, new TypeReference<Map<String, CachedLetter>>() {});

        return cachedLetters;

    }
    private Boolean checkNewVersion(String token, Long currentVersion) {
        String json = redisTemplate.opsForValue().get("cache:letter" + token).toString();
        if (json == null) {
            return true;
        }
        CachedLetter letter = objectMapper.readValue(json, CachedLetter.class);
        return currentVersion > letter.version();
    }
    @Override
    public Long putLetter(CachedLetter letter, Duration ttl) {
        if (letter == null || letter.publicToken() == null ||
            letter.authorEmail() == null ||letter.text() == null) {
            return 0L;
        }


        String  key = "cache:letter:" + letter.publicToken();

        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            return 0L;
        }
        
        try {
            String json = objectMapper.writeValueAsString(letter);
            if (checkNewVersion(letter.publicToken(), letter.version())) {
                redisTemplate.opsForValue().set(key, json, ttl);
                return 1L;
            } 
            return 0L;
        }catch (JSONException e) {
            throw new IllegalStateException("Failed to serialize CachedLetter", e);
        }
    }

    @Override
    public Optional<CachedLetter> findLetter(String publicToken) {
        String  key = "cache:letter:" + publicToken;

        String json = redisTemplate.opsForValue().get(key).toString();
        if (json == null) {
            return Optional.empty();
        }
        Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        if (ttlSeconds == null || ttlSeconds  <= -2) {
            return Optional.empty();
        }
        try {
            CachedLetter letter = objectMapper.readValue(json, CachedLetter.class);
            return Optional.of(letter);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize CachedLetter", e);
        }
    }


    public Boolean putToken(String email, String new_token) {
        if (email == null || email.isBlank() || new_token == null || new_token.isBlank()) {
            return false;
        }
        
        String key = "cache:user:" + email;
        Long added = redisTemplate.opsForSet().add(key, new_token);
        return Long.valueOf(1L).equals(added);
    }

    public Set<String> getTokens(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        String key = "cache:user:" + email;
        Set<Object> tokenSet = redisTemplate.opsForSet().members(key);
        if (tokenSet == null || tokenSet.isEmpty()) {
            return Set.of();
        }

        return tokenSet.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toSet());

    }

    public Long getTokensLength(String email) {
        if (email == null || email.isBlank()) {
            return 0L;
        }

        String key = "cache:user:" + email;
        Long tokenSize = redisTemplate.opsForSet().size(key);
    
        return tokenSize;

    }

    public Boolean deleteToken(String email, String token) {
        if (email == null || email.isBlank() || token == null || token.isBlank()) {
            return false;
        }

        String key = "cache:user:" + email;
        Long deleted = redisTemplate.opsForSet().remove(key, token);

        return Long.valueOf(1L).equals(deleted);

    }

}
