package com.example.myproject.Services;

import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.myproject.DTO.CreateUserResponse;
import com.example.myproject.DTO.UserData;
import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Repositories.MyAppUserRepository;
import com.example.myproject.Utils.JwtTokenUtil;

import jakarta.transaction.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.AllArgsConstructor;

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
