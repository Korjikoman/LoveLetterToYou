package com.example.myproject.Controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
        return "index";
    }



    // Обновляем TTL user is online
    @PostMapping(value="/api/user/user-is-online")
    @ResponseBody
    public void check(Authentication auth){
        if (auth == null){
            return;
        }
        redisRepository.updateUserOnline(auth.getName());
    }

    @GetMapping("/api/user/user-is-online")
    public String redirectToIndex() {
        
        System.out.println("NIGGA");
        return "redirect:/index";
    }
    
    // Обновляем TTL user is writing letter
    @PostMapping(value="/api/letter/heartbeat-writing-letter")
    @ResponseBody
    public void leave(Authentication auth){
        if (auth == null){
            return;
        }
        // Пользователь не пишет письмо
        redisRepository.updateUserWritingLetter(auth.getName());
    }


    @GetMapping("/api/letter/heartbeat-writing-letter")
    public String redirectToCreateLetter() {
        return "redirect:/create-letter";
    }

    @GetMapping("/create/letter")
    public String createLetter(Authentication authentication) {
        redisRepository.setUserWritingLetter(authentication.getName(), true);
        
        return "create-letter";
    }
    
    
    
}
