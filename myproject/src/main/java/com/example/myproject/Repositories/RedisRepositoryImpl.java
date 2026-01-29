package com.example.myproject.Repositories;

import java.util.Map;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.example.myproject.Model.Letter;
import com.example.myproject.Model.MyAppUser;

@Repository
public class RedisRepositoryImpl implements RedisRepository {


    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, Object> hashOperations;

    private static final String COUNT_ONLINE_USERS = "online_users";

    public RedisRepositoryImpl(RedisTemplate<String, Object> template){
        this.redisTemplate = template;
        this.hashOperations = template.opsForHash();
    }

    @Override
    public void add(Letter letter){
        String key = letter.getPublicToken();
        
        hashOperations.put(key, "text", letter.getText());
        hashOperations.put(key, "title", letter.getTitle());
        hashOperations.put(key, "email", letter.getAuthorEmail());

        // TTL -- ? EXPIRE
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

        String key = email;
        hashOperations.put( key, "isOnline", String.valueOf(isOnline));
        if (isOnline){
            redisTemplate.opsForSet().add(COUNT_ONLINE_USERS, email);
        }
        else{
            redisTemplate.opsForSet().remove(COUNT_ONLINE_USERS, email);
        }
    }

    @Override
    public void setUserWritingLetter(String email, boolean isWritingLetter) {
        String key = email;
        hashOperations.put(key, "isWritingLetter", String.valueOf(isWritingLetter));
    }

    @Override
    public long countOnlineUsers() {
        return redisTemplate.opsForSet().size(COUNT_ONLINE_USERS);
    }

   
  
}
