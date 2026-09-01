package com.example.myproject.Utils;

import java.security.SecureRandom;
import java.util.Random;



public class PublicToken {
    private static final String TOKEN_ALPHABET = "qazwsxedcrfvtgbyhnujmikolpQAZWSXEDCRFVTGBYHNUJMIKOLP1234567890";
    private static final String KEY_ALPHABET = "qazwsxedcrfvtgbyhnujmikolpQAZWSXEDCRFVTGBYHNUJMIKOLP1234567890!@#$%&*()_+?.,<>";
    private static final int MAX_TOKEN_LENGTH = 32; 
    private static final int MIN_TOKEN_LENGTH = 16;
    private static final int KEY_LENGTH = 32;
    
    public static String generatePublicToken(){
        StringBuffer token = new StringBuffer();
        SecureRandom random = new SecureRandom();
        Random randomCounter = new Random();

        int counter = MIN_TOKEN_LENGTH + randomCounter.nextInt(MAX_TOKEN_LENGTH - MIN_TOKEN_LENGTH + 1);
        int idx = 0;
        for (int i=0; i < counter; i++){
            idx = random.nextInt(TOKEN_ALPHABET.length());
            token.append(TOKEN_ALPHABET.charAt(idx));
        }
        return token.toString();
    }

    public static String generateSecurityKey() {
        StringBuffer key = new StringBuffer();
        SecureRandom random = new SecureRandom();
        int idx = 0;
        for (int i=0; i < KEY_LENGTH; i++){
            idx = random.nextInt(KEY_ALPHABET.length());
            key.append(KEY_ALPHABET.charAt(idx));
        }
        return key.toString();
    }

}
