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

        // TTL -- ?
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
  
}
