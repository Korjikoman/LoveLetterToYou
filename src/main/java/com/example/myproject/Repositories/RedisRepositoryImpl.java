package com.example.myproject.Repositories;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import com.example.myproject.DTO.CachedWrite;
import com.example.myproject.Model.CachedLetter;
import tools.jackson.databind.ObjectMapper;

@Repository
public class RedisRepositoryImpl implements RedisRepository {

    // Fence живёт дольше максимального TTL письма и не даёт старому чтению
    // вернуть в кэш уже изменённую или удалённую версию.
    private static final long LETTER_FENCE_TTL_SECONDS = 172_800L;


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
    public Boolean evictLetter(String publicToken, String email, Long version){
        if (publicToken == null || publicToken.isBlank()
            || email == null || email.isBlank()
            || version == null || version < 0) {
            return false;
        }
        String key1 = "cache:letter:" + publicToken;
        String key2 = "cache:letter:fence:" + publicToken;

        RedisScript<Long> DELETE_LETTER_SCRIPT = RedisScript.of(
            """
            local incomingVersion = tonumber(ARGV[1])
            local publicToken = ARGV[2]
            local fenceTtl = tonumber(ARGV[3])
            local fence = tonumber(redis.call('GET', KEYS[2]) or '-1')

            if incomingVersion > fence then
                redis.call('SET', KEYS[2], incomingVersion, 'EX', fenceTtl)
            end

            local value = redis.call('GET', KEYS[1])
            if not value then
                redis.call('SREM', KEYS[3], publicToken)
                return 1
            end

            local decoded, data = pcall(cjson.decode, value)
            if not decoded then
                redis.call('DEL', KEYS[1])
                redis.call('SREM', KEYS[3], publicToken)
                return 1
            end

            local cachedVersion = tonumber(data.version) or -1
            if cachedVersion <= incomingVersion then
                redis.call('DEL', KEYS[1])
                redis.call('SREM', KEYS[3], publicToken)
            end

            return 1
            """, Long.class
        );
        String cacheUserEmail = "cache:user:" + email;
        Long result = redisTemplate.execute(
            DELETE_LETTER_SCRIPT,
            List.of(key1, key2, cacheUserEmail),
            version.toString(), publicToken,
            Long.toString(LETTER_FENCE_TTL_SECONDS)
        );

        if (result == null || result < 1){
            return false;
        }
      
        return true;
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

        if (cachedWrites == null || cachedWrites.isEmpty()) {
            return;
        }

        List<String> keys = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        RedisScript<Long> redisScript = RedisScript.of(
            """
                local itemCount = math.floor(#KEYS / 2)
                for i = 1, itemCount do
                    local incomingVer = tonumber(ARGV[(i - 1) * 3 + 1])
                    local json = ARGV[(i - 1) * 3 + 2]
                    local ttlSeconds = tonumber(ARGV[(i - 1) * 3 + 3])
                    local letterKey = KEYS[(i - 1) * 2 + 1]
                    local fenceKey = KEYS[(i - 1) * 2 + 2]
                    local currentVer = tonumber(redis.call('GET', fenceKey) or '-1')

                    if ttlSeconds > 0 and incomingVer >= currentVer then
                        local currentLetter = redis.call('GET', letterKey)
                        local shouldWrite = true
                        if currentLetter then
                            local decoded, cachedData = pcall(cjson.decode, currentLetter)
                            if decoded then
                                local cachedVersion = tonumber(cachedData.version) or -1
                                shouldWrite = incomingVer > cachedVersion
                            end
                        end

                        if shouldWrite then
                            redis.call('SET', letterKey, json, 'EX', ttlSeconds)
                            redis.call('SET', fenceKey, incomingVer, 'EX', ARGV[#ARGV])
                        end
                    end
                end
                return 1

            """, Long.class
        );

        for (CachedWrite write : cachedWrites) {
            if (write == null) {
                continue;
            }
            CachedLetter cached = write.cachedLetter();

            if (cached == null || cached.publicToken() == null
                || cached.publicToken().isBlank() || cached.version() == null) {
                continue;
            }

            Duration ttl = write.ttl();
            long ttlSeconds = ttl == null ? 0 : ttl.getSeconds();

            if (ttlSeconds <= 0) {
                continue;
            }

            try {
                String json = objectMapper.writeValueAsString(cached);
                keys.add("cache:letter:" + cached.publicToken());
                keys.add("cache:letter:fence:" + cached.publicToken());
                args.add(cached.version().toString());
                args.add(json);
                args.add(Long.toString(ttlSeconds));
            } catch (JSONException e) {
                throw new IllegalStateException("Cannot serialize CachedLetter", e);
            }

        }

        if (keys.isEmpty()) return;

        args.add(Long.toString(LETTER_FENCE_TTL_SECONDS));

        redisTemplate.execute(redisScript, keys, args.toArray());
    }

    @Override
    public Map<String, CachedLetter> getLettersByTokens(List<String> tokens) {
        if (tokens == null || tokens.isEmpty() ){
            return Map.of();
        }

        
        List<String> keys = tokens.stream().map(token -> "cache:letter:" + token).toList();
        List<Object> objects = redisTemplate.opsForValue().multiGet(keys);
        if (objects == null ) {
            return Map.of();
        }

        Map<String, CachedLetter> result = new HashMap<>(objects.size());

        for (int i= 0 ; i < objects.size(); i++) {
            Object obj = objects.get(i);
            if (obj == null) {
                continue;
            }
            try {
                CachedLetter letter = objectMapper.readValue(obj.toString(), CachedLetter.class);
                result.put(letter.publicToken(), letter);
            }catch (Exception exception) {
                log.warn("Invalid cachedLetter JSON for token={}",tokens.get(i), exception);
            }
        }


        return result;

    }

    
    @Override
    public Long putLetter(CachedLetter letter, Duration ttl) {
        if (letter == null || letter.publicToken() == null ||
            letter.authorEmail() == null ||letter.text() == null) {
            return 0L;
        }


        String  key1 = "cache:letter:" + letter.publicToken();
        String  key2 = "cache:letter:fence:" + letter.publicToken();
        RedisScript<Long> script = RedisScript.of(
            """
                local incomingVer = tonumber(ARGV[1])
                local ttlSeconds = tonumber(ARGV[3])
                if not incomingVer or not ttlSeconds or ttlSeconds <= 0 then
                    return 0
                end

                local currentVer = tonumber(redis.call('GET', KEYS[2]) or '-1')
                if incomingVer < currentVer then
                    return 0 
                end

                local currentLetter = redis.call('GET', KEYS[1])
                
                if currentLetter then
                    local decoded, data = pcall(cjson.decode, currentLetter)
                    local cachedVersion = decoded and tonumber(data.version) or -1

                    if incomingVer <= cachedVersion then
                        return 0 
                    end
                end

                redis.call('SET', KEYS[1], ARGV[2], 'EX', ttlSeconds)
                redis.call('SET', KEYS[2], incomingVer, 'EX', ARGV[4])
                return 1
            """, Long.class
        );

        long ttlSeconds = ttl == null ? 0 : ttl.getSeconds();
        if (letter.version() == null || ttlSeconds <= 0) {
            return 0L;
        }
        
        try {
            String incomingCachedLetterJson = objectMapper.writeValueAsString(letter);
            Long version = letter.version();


            Long res = redisTemplate.execute(
                script,
                List.of(key1, key2),
                String.valueOf(version),
                incomingCachedLetterJson,
                Long.toString(ttlSeconds),
                Long.toString(LETTER_FENCE_TTL_SECONDS)
            );
            return res;
        }catch (JSONException e) {
            throw new IllegalStateException("Failed to serialize CachedLetter", e);
        }
    }

    @Override
    public Optional<CachedLetter> findLetter(String publicToken) {
        String  key = "cache:letter:" + publicToken;

        Object jsonObj = redisTemplate.opsForValue().get(key);
        if (jsonObj == null) {
            return Optional.empty();
        }

        String json = jsonObj.toString();
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
            // Повреждённая запись кэша является промахом, а не ошибкой запроса.
            log.warn("Invalid CachedLetter JSON for token={}", publicToken, e);
            redisTemplate.delete(key);
            return Optional.empty();
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
