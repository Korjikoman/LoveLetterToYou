package com.example.myproject.Repositories;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        // Добавляем письмо по ключу -- PUBLIC TOKEN  + 
        // Добавляем public token в список писем, написанных юзером, 
        // чтобы потом их все (оставшиеся в живых) 
        // можно было получать
        String public_token = letter.getPublicToken();
        
        String script = """
                redis.call('HSET', KEYS[1], 'text', ARGV[1])
                redis.call('HSET', KEYS[1], 'title', ARGV[2])
                redis.call('HSET', KEYS[1], 'email', ARGV[3])
                redis.call('HSET', KEYS[1], 'password', ARGV[4])
                redis.call('SADD', KEYS[2],  ARGV[5])
                redis.call('EXPIRE', KEYS[1], ARGV[6])
                return 1
        """;



        RedisScript<Long> redisScript = RedisScript.of(script, Long.class);
        
        String email = letter.getAuthorEmail();

        redisTemplate.execute(
            redisScript,
            List.of(public_token, email),
            letter.getText(),
            letter.getTitle(),
            email,
            letter.getPassword(),
            public_token,
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
        letter.setAuthorEmail(user.getEmail());

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
    public List<Letter> getAllLetters(String email) {
        if (email == null || email.isEmpty() ){
            return List.of();
        }

        if (!Boolean.TRUE.equals(redisTemplate.hasKey(email))){
            return List.of();
        }

        List<Letter> listOfLetters = new ArrayList<Letter>();
        Set<Object> objects = redisTemplate.opsForSet().members(email);

        if (objects == null || objects.isEmpty()){
            return List.of();
        }

        Set<String> publicTokens = new HashSet<String>();

        // Преобразуем из Object в String
        for (Object obj : objects){
            if (obj != null){
                publicTokens.add(obj.toString());
            }
        }

        

        for (String publicToken : publicTokens){
            if (Boolean.TRUE.equals(redisTemplate.hasKey(publicToken))){

                Map<String, Object> data = hashOperations.entries(publicToken);

                Letter letter = new Letter();
                letter.setText(data.get("text").toString());
                letter.setTitle(data.get("title").toString());
                letter.setAuthorEmail(data.get("email").toString());
                letter.setPassword(data.get("password").toString());
                letter.setPublicToken(publicToken);
                
                
                listOfLetters.add(letter);

            }
            else{
                redisTemplate.opsForSet().remove(email, publicToken);
            }
        }

        return listOfLetters;

    }
  
  
}
