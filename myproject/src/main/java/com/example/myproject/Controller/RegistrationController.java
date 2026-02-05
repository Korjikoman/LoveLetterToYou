package com.example.myproject.Controller;



import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Repositories.MyAppUserRepository;
import com.example.myproject.Services.EmailService;
import com.example.myproject.Utils.JwtTokenUtil;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// аннотация 
@RestController
@RequestMapping("/req")
public class RegistrationController {
    
    @Autowired
    private MyAppUserRepository myAppUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    EmailService emailService;

    public RegistrationController( MyAppUserRepository myAppUserRepository, PasswordEncoder passwordEncoder) {
        this.myAppUserRepository = myAppUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping(value="/signup", consumes = "application/json")
    public ResponseEntity<String> createUser(@RequestBody MyAppUser user) {

        Optional<MyAppUser> existingUserOptional = myAppUserRepository.findByEmail(user.getEmail());

        if (!existingUserOptional.isEmpty()){
            MyAppUser existingUser = existingUserOptional.get();

            if (existingUser.getIsVerified()){
                return new ResponseEntity<>("User Already Exists And Verified!", HttpStatus.BAD_REQUEST);
            }else{
                String verificationToken = JwtTokenUtil.generateToken(existingUser.getEmail());
                existingUser.setVerificationToken(verificationToken);
                existingUser.isHasAvatar(false);
                existingUser.setAvatarPath(null);

                myAppUserRepository.save(existingUser);
                // SEND EMAIL CODe
                emailService.sendVerificationEmail(existingUser.getEmail(), verificationToken);
                return new ResponseEntity<>("Verification email resent. Check your email-box!", HttpStatus.OK);


            }
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        String verificationToken = JwtTokenUtil.generateToken(user.getEmail());
        user.setVerificationToken(verificationToken);
        user.isHasAvatar(false);
        user.setAvatarPath(null);
        myAppUserRepository.save(user);
        // SEND EMAIL CODe
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        return new ResponseEntity<>("Successfully registered! Verify your email in email-box :)", HttpStatus.OK);
    }
    

}
