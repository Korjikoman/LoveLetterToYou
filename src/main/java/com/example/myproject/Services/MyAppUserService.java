package com.example.myproject.Services;

import java.sql.SQLException;
import java.util.Optional;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.myproject.DTO.CreateUserResponse;
import com.example.myproject.DTO.UserData;
import com.example.myproject.DTO.UserProfileView;
import com.example.myproject.DTO.ValidationLimits;
import com.example.myproject.Images.DTO.ImageView;
import com.example.myproject.Images.Model.Image;
import com.example.myproject.Model.MyAppUser;
import com.example.myproject.Repositories.MyAppUserRepository;

@Service
public class MyAppUserService implements UserDetailsService {
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    private final MyAppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public MyAppUserService(
        MyAppUserRepository repository,
        PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

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
        try {
            repository.saveAndFlush(user);
            return CreateUserResponse.USER_CREATED;
        } catch (DataIntegrityViolationException exception) {
            if (causedByUniqueViolation(exception)) {
                return CreateUserResponse.USER_EXISTS;
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public UserProfileView getProfile(String email) {
        MyAppUser user = findUser(email);
        return toProfile(user);
    }


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
            if (normalized.length() < ValidationLimits.USERNAME_MIN_LENGTH
                || normalized.length() > ValidationLimits.USERNAME_MAX_LENGTH) {
                throw new IllegalArgumentException("Username is too long");
            }
            user.setUsername(normalized);
        }
        if (password != null && !password.isBlank()) {
            if (password.length() < ValidationLimits.PASSWORD_MIN_LENGTH
                || password.length() > ValidationLimits.PASSWORD_MAX_LENGTH) {
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

    private boolean causedByUniqueViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException
                && UNIQUE_VIOLATION_SQL_STATE.equals(
                    sqlException.getSQLState()
                )) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
