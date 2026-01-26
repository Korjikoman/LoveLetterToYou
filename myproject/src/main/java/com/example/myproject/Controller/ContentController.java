package com.example.myproject.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;





@Controller
public class ContentController {
    
    @GetMapping("/req/login")
    public String login() {
        return "login";
    }

    @GetMapping("/req/signup")
    public String signup() {
        return "signup";
    }
    

    
    @GetMapping("/index")
    public String home() {
        return "index";
    }

    @GetMapping("/create/letter")
    public String getMethodName() {
        return "create-letter";
    }
    
}
