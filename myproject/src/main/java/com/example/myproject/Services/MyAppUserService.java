package com.example.myproject.Services;

import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Repositories.MyAppUserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor 
public class MyAppUserService implements UserDetailsService{
    
    @Autowired
    private MyAppUserRepository repository;
    

    

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
