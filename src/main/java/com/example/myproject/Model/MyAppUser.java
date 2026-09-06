package com.example.myproject.Model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class MyAppUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String username;
    @Column( name="email", nullable = false, unique = true)
    private String email;
    private String password;
    private String verificationToken;
    private boolean isVerified;

    @Column(name="avatar_path")
    private String avatarPath;

    @Column(name="pending_avatar_operation_id")
    private UUID pendingAvatarOperationId;

    @Column(name = "reset_token")
    private String resetToken;


    @Column(name = "user_has_avatar", nullable = false)
    private boolean hasAvatar;

    public Boolean hasAvatar(){
        return avatarPath != null;
    }

    
}
