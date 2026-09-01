package com.example.myproject.Repositories;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.example.myproject.DTO.CachedWrite;
import com.example.myproject.Model.CachedLetter;
import com.example.myproject.Model.Letter;

public interface RedisRepository {
    Optional<CachedLetter> findLetter(String publicToken);
    Long putLetter(CachedLetter letter, Duration ttl);
    Boolean evictLetter(String publicToken, String email);
    Map<String, CachedLetter> getLettersByTokens(List<String> tokens);
    void putLetters(List<CachedWrite> cachedWrites);
    
    Long getTokensLength(String email);
    Boolean deleteToken(String email, String token);
    Set<String> getTokens(String email);
    Boolean putToken(String email, String new_token);
    
    void setUserOnline(String email, boolean isOnline);
    void setUserWritingLetter(String email, boolean isWritingLetter);
    boolean isUserOnline(String email);
    boolean isUserWritingLetter(String email);
    long countOnlineUsers();
    long countWritingLetterUsers();
    void updateUserWritingLetter(String email);
    void updateUserOnline(String email);
    
    Map<Object, Object> dumpTestData();
}
