package com.example.myproject.Controller;



import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.myproject.DTO.CreateUserResponse;
import com.example.myproject.DTO.UserData;
import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Repositories.MyAppUserRepository;
import com.example.myproject.Services.MyAppUserService;
import com.example.myproject.Utils.JwtTokenUtil;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// аннотация 
@RestController
@RequestMapping("/req")
public class RegistrationController {
    
    private PasswordEncoder passwordEncoder;
    private MyAppUserService myAppUserService;

    public RegistrationController( MyAppUserService myAppUserService, PasswordEncoder passwordEncoder) {
        this.myAppUserService = myAppUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping(value="/signup", consumes = "application/json")
    public ResponseEntity<String> createUserController(@RequestBody UserData userData) {

        CreateUserResponse response = myAppUserService.createUser(userData);
        switch (response) {
            case CreateUserResponse.USER_CREATED:{
                return new ResponseEntity<>("Successfully registered! ", HttpStatus.OK);
            }
            case CreateUserResponse.USER_EXISTS : {
                return new ResponseEntity<>("User is already exists", HttpStatus.FORBIDDEN);
            }
            default : {
                return new ResponseEntity<>("Error creating user! ", HttpStatus.BAD_REQUEST);
            }
        }
    }
    

}
