package com.example.myproject.Repositories;

import java.util.Map;

import com.example.myproject.Model.Letter;

public interface RedisRepository {
    void add(Letter letter);
    void delete(Letter letter); 
    Letter findLetter(String publicToken);
}
