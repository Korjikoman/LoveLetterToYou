package com.example.myproject.Repositories;

import java.util.List;
import java.util.Map;

import org.json.JSONMLParserConfiguration;

import com.example.myproject.Model.Letter;
import com.example.myproject.Model.MyAppUser;

public interface RedisRepository {
    void add(Letter letter);
    void delete(Letter letter); 
    Letter findLetter(String publicToken);


    void setUserOnline(String email, boolean isOnline);
    void setUserWritingLetter(String email, boolean isWritingLetter);
    boolean isUserOnline(String email);
    boolean isUserWritingLetter(String email);
    long countOnlineUsers();
    long countWritingLetterUsers();
    void updateUserWritingLetter(String email);
    void updateUserOnline(String email);
    List<Letter> getAllLetters(String email);

}
