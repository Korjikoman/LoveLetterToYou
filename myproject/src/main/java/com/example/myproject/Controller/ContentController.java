package com.example.myproject.Controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.myproject.Model.Letter;
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
    
    @GetMapping("/create/letter/geturl")
    public String getURL() {
        return "geturl";
    }
    
    @GetMapping("/index")
    public String home(Model model, Authentication authentication) {
        String email = authentication.getName();
        
        MyAppUser user = myAppUserRepository.findByEmail(email).get();
        

        //model.addAttribute("user", user);
        //model.addAttribute("onlineCount", usersOnlineCounter);
        return "index";
    }

    @GetMapping("/create/letter")
    public String createLetter() {
        return "create-letter";
    }
    
    @GetMapping("/watch/letter/{publicToken}")
    public String watchLetter(@PathVariable String publicToken, Model model) {
        Letter letter = new Letter();
        letter = redisRepository.findLetter(publicToken);
        model.addAttribute("letterTitle", letter.getTitle());
        model.addAttribute("letterText", letter.getText());
        model.addAttribute("authorEmail", letter.getAuthorEmail());

        return "open-letter-and-watch-content";
    }
    
}
