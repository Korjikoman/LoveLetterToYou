package com.example.myproject.Controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Repositories.MyAppUserRepository;
import com.example.myproject.Services.EmailService;
import com.example.myproject.Services.MyAppUserService;
import com.example.myproject.Utils.JwtTokenUtil;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;


@Controller
@RequestMapping("/profile")
public class UsersProfileController {
    @Autowired
    MyAppUserRepository myAppUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    MyAppUserService userService;

    @Autowired
    EmailService emailService;


    private void uploadAvatar(MultipartFile file, MyAppUser user) throws IOException {

        String uploadDir = "uploads/avatars/";
        Files.createDirectories(Paths.get(uploadDir));

        if (user.getAvatarPath() != null && !user.getAvatarPath().isBlank()) {
            Path oldPath = Paths.get(user.getAvatarPath().replaceFirst("^/", "")); 
            if (Files.exists(oldPath)) {
                Files.delete(oldPath);
            }
        }   

        String filename = "user_" + user.getId() + "_avatar_"+ file.getOriginalFilename();
        Path path = Paths.get(uploadDir + filename);

        Files.write(path, file.getBytes());

        user.setAvatarPath(uploadDir + filename);
        user.isHasAvatar(true);
    }

    @GetMapping("/uploads/avatars/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveAvatar(@PathVariable String filename) throws MalformedURLException {
        Path file = Paths.get("uploads/avatars/").resolve(filename);
        Resource resource = new UrlResource(file.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                .body(resource);
    }

    @GetMapping("/get")
    public String getMethodName(Authentication auth, Model model) {
        String email = auth.getName();
        MyAppUser user = new MyAppUser();
        Optional<MyAppUser> optionalUser = myAppUserRepository.findByEmail(email);
        if (optionalUser.isEmpty()){
            return "redirect:/index?error=true";
        }
        user = optionalUser.get();
        if (user == null){
            return "redirect:/index?error=true";
        }

        String username = user.getUsername();
        Boolean passwordIsVerified = user.getIsVerified();
        Boolean userHasAvatar = user.gethasAvatar();
        String avatarPath = user.getAvatarPath();
        
        model.addAttribute("username", username);
        model.addAttribute("email", email);
        model.addAttribute("passwordIsVerified", passwordIsVerified);
        model.addAttribute("userHasAvatar", userHasAvatar);
        model.addAttribute("avatarPath", avatarPath);

        return "users-profile";
    }

    @PostMapping(value = "/get", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String changeValues(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) MultipartFile file,
            Authentication auth
    ) {

        String email = auth.getName();

        MyAppUser user = myAppUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            if (username != null && !username.isBlank()) {
                user.setUsername(username);
            }

            if (password != null && !password.isBlank()) {
                user.setPassword(passwordEncoder.encode(password));

                String verificationToken = JwtTokenUtil.generateToken(user.getEmail());
                user.setVerificationToken(verificationToken);
                user.setIsVerified(false);
                //emailService.sendVerificationEmail(user.getEmail(), verificationToken);
            }

            if (file != null && !file.isEmpty()) {
                uploadAvatar(file, user);
            }

            myAppUserRepository.save(user);

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/profile/get?error=true";
        }

        return "redirect:/profile/get?success=true";
    }


    
}
