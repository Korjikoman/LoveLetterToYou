package com.example.myproject.Controller;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Repositories.MyAppUserRepository;
import com.example.myproject.Repositories.RedisRepository;





@Controller
public class ContentController {


    @Autowired
    RedisRepository redisRepository;

    @Autowired
    MyAppUserRepository myAppUserRepository;



    @GetMapping("/req/login")
    public String login() {
        return "login";
    }


    @GetMapping("/req/signup")
    public String signup() {

        return "signup";
    }

    @GetMapping("/check")
    public String instance() {
        return System.getenv("HOSTNAME");
    }

    @GetMapping("/create/letter/geturl")
    public String getURL() {
        return "geturl";
    }

    @GetMapping("/index")
    public String home(Model model, Authentication authentication) {
        Optional<MyAppUser> getUser = myAppUserRepository.findByEmail(authentication.getName());
        if (!getUser.isEmpty()){
            MyAppUser user = getUser.get();
            model.addAttribute("username", user.getUsername());
        }
        return "index";
    }


    @GetMapping("/create/letter")
    public String createLetter(Authentication authentication) {
        return "create-letter";
    }



}
