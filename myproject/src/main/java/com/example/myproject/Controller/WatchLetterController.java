package com.example.myproject.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.myproject.Model.Letter;
import com.example.myproject.Repositories.RedisRepository;

@Controller
@RequestMapping("/watch/letter")
public class WatchLetterController {
    @Autowired
    RedisRepository redisRepository;


    @GetMapping("/{publicToken}")
    public String watchLetter(@PathVariable String publicToken, Model model) {

        Letter letter = new Letter();
        letter = redisRepository.findLetter(publicToken);
        if (letter == null){
            return "letter-not-found";
        }

        if (letter.getPassword().isEmpty() || letter.getPassword() == null){
            model.addAttribute("letterTitle", letter.getTitle());
            model.addAttribute("letterText", letter.getText());
            model.addAttribute("authorEmail", letter.getAuthorEmail());

            return "open-letter-and-watch-content";
        }
     

        return "confirm-password";
    }

    @PostMapping("/{publicToken}")
    public String confirmPassword(@RequestParam String password, @PathVariable String publicToken, Model model){
        Letter letter = new Letter();
        letter = redisRepository.findLetter(publicToken);
        String pwd = letter.getPassword().toString();
        if (password.equals(pwd)){
            model.addAttribute("letterTitle", letter.getTitle());
            model.addAttribute("letterText", letter.getText());
            model.addAttribute("authorEmail", letter.getAuthorEmail());
           return "open-letter-and-watch-content";
        }
        else{
            model.addAttribute("error", "Password is incorrect, please retry");
            return "confirm-password";
        }
        
        
    }
    
    
}
