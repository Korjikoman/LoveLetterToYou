package com.example.myproject.Services;

import java.util.Optional;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.myproject.DTO.CreateUserResponse;
import com.example.myproject.DTO.UserData;
import com.example.myproject.DTO.UserProfileView;
import com.example.myproject.Images.DTO.ImageView;
import com.example.myproject.Images.Model.Image;
import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Repositories.MyAppUserRepository;

@Service
public class MyAppUserService implements UserDetailsService {
    private final MyAppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public MyAppUserService(
        MyAppUserRepository repository,
        PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreateUserResponse createUser(UserData data) {
        if (data == null
            || data.email() == null || data.email().isBlank()
            || data.password() == null || data.password().isBlank()) {
            return CreateUserResponse.ERROR;
        }
        if (repository.findByEmail(data.email()).isPresent()) {
            return CreateUserResponse.USER_EXISTS;
        }

        MyAppUser user = new MyAppUser();
        user.setEmail(data.email().trim());
        user.setUsername(normalizeUsername(data.username(), data.email()));
        user.setPassword(passwordEncoder.encode(data.password()));
        repository.save(user);
        return CreateUserResponse.USER_CREATED;
    }

    @Transactional(readOnly = true)
    public UserProfileView getProfile(String email) {
        MyAppUser user = findUser(email);
        return toProfile(user);
    }

    /** Изменяет только переданные непустые поля профиля. */
    @Transactional
    public UserProfileView updateProfile(
        String email,
        String username,
        String password
    ) {
        MyAppUser user = repository.findByEmailForUpdate(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (username != null && !username.isBlank()) {
            String normalized = username.trim();
            if (normalized.length() > 100) {
                throw new IllegalArgumentException("Username is too long");
            }
            user.setUsername(normalized);
        }
        if (password != null && !password.isBlank()) {
            if (password.length() < 8 || password.length() > 200) {
                throw new IllegalArgumentException("Invalid password length");
            }
            user.setPassword(passwordEncoder.encode(password));
        }
        return toProfile(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
        throws UsernameNotFoundException {
        MyAppUser user = repository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(email));
        return User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .build();
    }

    private MyAppUser findUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }
        return repository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private UserProfileView toProfile(MyAppUser user) {
        Image avatar = user.getAvatarImage();
        ImageView avatarView = avatar == null
            ? null
            : new ImageView(
                avatar.getId(),
                "/api/images/" + avatar.getId() + "/content",
                0
            );
        return new UserProfileView(
            user.getUsername(), user.getEmail(), avatarView
        );
    }

    private String normalizeUsername(String username, String email) {
        return username == null || username.isBlank()
            ? email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length())
            : username.trim();
    }
}
