package com.example.myproject.Model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.example.myproject.DTO.FontData;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Letter {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name="user_email")
    private String authorEmail; 
    
    @Column(unique=true, nullable = false, updatable = false)
    private String publicToken;

    @Column(unique=true, nullable = false, updatable = false)
    private String securityKey;

    
    @Version
    @Column(nullable = false)
    private long version;

    private String password;

    private String title;

    private String text;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private MyAppUser user;
    
    @Column(nullable = false)
    private Instant expiresAt;

    @Column(name="burn_after_opening", nullable = false)
    private boolean burn_after_opening; 


    /*
    
    letter
    +----+--------------+
    | id | public_token |
    +----+--------------+
    |  5 | abc123       |
    +----+--------------+

    letter_images
    +--------------------+--------------------+
    | letter_id | image_path                  |
    +--------------------+--------------------+
    | 5                  | /images/photo1.jpg |
    | 5                  | /images/photo2.jpg |
    +--------------------+--------------------+
    
    Вот это описано в аннотациях
    
    */
    @ElementCollection
    @CollectionTable(
        name = "letter_images",
        joinColumns = @JoinColumn(name = "letter_id", referencedColumnName = "id")
    )
    @Column(name = "image_path")
    private List<String> imagesPaths;

    @Embedded
    private FontData font;

    
    @Column(name= "reaction_code")
    private List<String> reactions;


}
