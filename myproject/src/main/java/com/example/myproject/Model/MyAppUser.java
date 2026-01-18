package com.example.myproject.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

@Getter
@Entity
public class MyAppUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String username;
    private String email;
    private String password;
    private String verificationToken;
    private boolean isVerified;

    @Column(name = "reset_token")
    private String resetToken;


    public void setVerificationToken(String verificationToken){
        this.verificationToken = verificationToken;
    }

    public String getVerificationToken(){
        return verificationToken;
    }

    public void setResetToken(String resetToken){
        this.resetToken = resetToken;
    }

    public String getResetToken(){
        return resetToken;
    }

    public void setIsVerified(boolean isVerified){
        this.isVerified = isVerified;
    }

    public boolean getIsVerified(){
        return isVerified;
    }

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

   

    
}
