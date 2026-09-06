package com.example.myproject.Services;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.myproject.DTO.CreateUserResponse;
import com.example.myproject.DTO.UserData;
import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Repositories.MyAppUserRepository;
import com.example.myproject.Utils.JwtTokenUtil;

import jakarta.transaction.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class MyAppUserService implements UserDetailsService{
    
    private MyAppUserRepository repository;
    private PasswordEncoder passwordEncoder;


    public MyAppUserService(MyAppUserRepository repository,  PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Transactional
    public CreateUserResponse createUser(UserData data) {

        Optional<MyAppUser> userOptional = repository.findByEmail(data.email());

        if (!userOptional.isEmpty()){
            return CreateUserResponse.USER_EXISTS;
        }
        try {
            MyAppUser user = new MyAppUser();
            user.setEmail(data.email());

            String hash = passwordEncoder.encode(data.password());

            user.setPassword(hash);

            String verificationToken = JwtTokenUtil.generateToken(user.getEmail());

            user.setVerificationToken(verificationToken);
            user.setIsVerified(true);

            user.isHasAvatar(false);

            user.setAvatarPath(null);

            repository.save(user);

            return CreateUserResponse.USER_CREATED;
        } catch (Exception exception) {
            return CreateUserResponse.ERROR;
        }
        
    }
    
    public uploadAvatar(MultipartFile file, UserData data) {
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

    @Override
    public UserDetails loadUserByUsername(String email)throws UsernameNotFoundException {
        

        Optional<MyAppUser> user = repository.findByEmail(email);
        if (user.isPresent()) {
            var userObj = user.get();
            return User.builder()
                    .username(userObj.getEmail())
                    .password(userObj.getPassword())
                    .build();    
        }else{
            throw new UsernameNotFoundException(email);
        }
    }
    
    
    
}
