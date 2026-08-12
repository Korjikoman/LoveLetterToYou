package com.example.myproject.Utils;

import java.security.SecureRandom;
import java.util.Random;


public class PublicToken {
    private static final String ALPHABET = "qazwsxedcrfvtgbyhnujmikolpQAZWSXEDCRFVTGBYHNUJMIKOLP1234567890";
    private static final int MAX_TOKEN_LENGTH = 32; 
    private static final int MIN_TOKEN_LENGTH = 16; 
    
    
    public static String generatePublicToken(){
        StringBuffer token = new StringBuffer();
        SecureRandom random = new SecureRandom();
        Random randomCounter = new Random();

        int counter = MIN_TOKEN_LENGTH + randomCounter.nextInt(MAX_TOKEN_LENGTH - MIN_TOKEN_LENGTH + 1);
        int idx = 0;
        for (int i=0; i < counter; i++){
            idx = random.nextInt(ALPHABET.length());
            token.append(ALPHABET.charAt(idx));
        }
        return token.toString();
    }

}
