package com.example.myproject.Utils;

import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public class JwtTokenUtil {
    private final static SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final static long EXPIRATION_TIME = 86400000;
    
    public static String generateToken(String email){
        return Jwts.builder()
        .setSubject(email)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
        .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
        .compact();

    }

    public boolean validateToken(String token){
        return !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        JwtParser jwtParser = Jwts.parserBuilder()
        .setSigningKey(SECRET_KEY)
        .build();

        return jwtParser.parseClaimsJws(token).getBody().getExpiration();
    }

    public String extractEmail(String token){
        JwtParser jwtParser = Jwts.parserBuilder()
        .setSigningKey(SECRET_KEY)
        .build();

        return jwtParser.parseClaimsJws(token).getBody().getSubject();
    }
}
