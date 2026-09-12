package com.example.myproject.Model;

import com.example.myproject.Images.Model.Image;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "my_app_user")
public class MyAppUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "my_app_user_seq", allocationSize = 50)
    private Long id;

    @Column(length = 100)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false)
    private String password;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "avatar_image_id",
        unique = true
    )
    private Image avatarImage;

    @Column(name = "reset_token")
    private String resetToken;

    public boolean hasAvatar() {
        return avatarImage != null;
    }
    
}
