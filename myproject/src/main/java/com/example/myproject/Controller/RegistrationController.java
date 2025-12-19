package com.example.myproject.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Model.MyAppUserRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

// аннотация 
@Controller
@RequestMapping("/req")
public class RegistrationController {
    

    private MyAppUserRepository myAppUserRepository;

    private PasswordEncoder passwordEncoder;


    public RegistrationController( MyAppUserRepository myAppUserRepository, PasswordEncoder passwordEncoder) {
        this.myAppUserRepository = myAppUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping(value="/signup", consumes = "application/json")
    public String createUser(@RequestBody MyAppUser user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        myAppUserRepository.save(user);
        return "redirect:/req/login";
    }
    

}
