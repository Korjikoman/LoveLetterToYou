package com.example.myproject.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Letter {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String authorEmail; 

    private String title;
    private String text;
    
    private String password;
    private Integer ttl;

    public void setPassword(String newPassword){
        this.password = newPassword;
    }

    public String getPassword(){
        return this.password;
    }


    public void setTTL(Integer ttl){
        this.ttl = ttl;
    }

    public Integer getTTL(){
        return this.ttl;
    }


    @Column(name="public_token", nullable=false, unique=true)
    private String publicToken;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    

    public void setCreatedAt(LocalDateTime time){
        createdAt = time;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public Long getLetterID(){
        return this.id;
    }


    public void setAuthorEmail(String email){
        this.authorEmail = email;
    }

    public String getAuthorEmail(){
        return this.authorEmail;
    }

    public void setTitle(String newTitle){
        this.title = newTitle;
    }

    public String getTitle(){
        return this.title;
    }

    public void setText(String text){
        this.text = text;
    }

    public String getText(){
        return this.text;
    }

    public void setPublicToken(String publicToken){
        this.publicToken = publicToken;
    }

    public String getPublicToken(){
        return this.publicToken;
    }
}
