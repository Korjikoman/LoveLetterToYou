package com.example.myproject.Repositories;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import com.example.myproject.Model.Letter;
import com.example.myproject.Model.MyAppUser;

@Repository
public class RedisRepositoryImpl implements RedisRepository {


    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, Object> hashOperations;



    private final int TIME_TO_LIVE_USER_ONLINE = 5; 
    private final int TIME_TO_LIVE_USER_WRITING_LETTER = 5; // in secs

    private static final String COUNT_ONLINE_USERS = "online_users";
    private static final String COUNT_WRITING_LETTER_USERS = "writing_letter_users";

    public RedisRepositoryImpl(RedisTemplate<String, Object> template){
        this.redisTemplate = template;
        this.hashOperations = template.opsForHash();
    }


    @Override
    public void add(Letter letter){
        String key = letter.getPublicToken();
        
        String script = """
                redis.call('HSET', KEYS[1], 'text', ARGV[1])
                redis.call('HSET', KEYS[1], 'title', ARGV[2])
                redis.call('HSET', KEYS[1], 'email', ARGV[3])
                redis.call('HSET', KEYS[1], 'password', ARGV[4])
                redis.call('EXPIRE', KEYS[1], ARGV[5])
                return 1
                """;
        RedisScript<Long> redisScript = RedisScript.of(script, Long.class);

        redisTemplate.execute(
            redisScript,
            List.of(key),
            letter.getText(),
            letter.getTitle(),
            letter.getAuthorEmail(),
            letter.getPassword(),
            String.valueOf(letter.getTTL() * 60) // TTL в минутах
        );

    }

    @Override
    public Letter findLetter(String publicToken){
        Map<String, Object> data = hashOperations.entries(publicToken);

        if (data.isEmpty()){
            return null;
        }

        Letter letter = new Letter();
        letter.setPublicToken(publicToken);
        letter.setText((String) data.get("text"));
        letter.setTitle((String) data.get("title"));
        letter.setPassword((String) data.get("password"));

        MyAppUser user = new MyAppUser();
        
        user.setEmail((String) data.get("email"));
        letter.setAuthor(user);

        return letter;
    }

    @Override
    public void delete(Letter letter){
        redisTemplate.delete(letter.getPublicToken());
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
    public String getAllLetters() {
        hashOperations.multiGet()
    }
   
  
}
