package com.example.myproject.Repositories;

import java.util.Map;

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


}
