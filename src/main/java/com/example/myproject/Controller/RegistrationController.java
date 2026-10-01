package com.example.myproject.Controller;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.myproject.DTO.CreateUserResponse;
import com.example.myproject.DTO.UserData;
import com.example.myproject.Services.MyAppUserService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/req")
public class RegistrationController {
    private final MyAppUserService myAppUserService;

    public RegistrationController(MyAppUserService myAppUserService) {
        this.myAppUserService = myAppUserService;
    }

    @PostMapping(value="/signup", consumes = "application/json")
    public ResponseEntity<String> createUserController(
        @Valid @RequestBody UserData userData
    ) {

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
