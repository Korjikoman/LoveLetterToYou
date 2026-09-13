package com.example.myproject.Utils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

import com.example.myproject.DTO.GenPair;



public class PublicToken {
    private static final String TOKEN_ALPHABET = "qazwsxedcrfvtgbyhnujmikolpQAZWSXEDCRFVTGBYHNUJMIKOLP1234567890";
    private static final String KEY_ALPHABET = "qazwsxedcrfvtgbyhnujmikolpQAZWSXEDCRFVTGBYHNUJMIKOLP1234567890!@#$%&*()_+?.,<>";
    private static final int MAX_TOKEN_LENGTH = 32; 
    private static final int MIN_TOKEN_LENGTH = 16;
    private static final int KEY_LENGTH = 32;

    private static final Random RNG = new Random();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();


    public static synchronized GenPair generatePair() {
        byte[] token = new byte[16];
        byte[] key = new byte[16];
        RNG.nextBytes(token);
        RNG.nextBytes(key);
        return new GenPair(B64.encodeToString(token), B64.encodeToString(key));
    }

}
