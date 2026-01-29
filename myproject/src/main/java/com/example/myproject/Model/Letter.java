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

    @ManyToOne
    @JoinColumn(name = "author_id", referencedColumnName = "id", nullable = false)
    private MyAppUser author; 

    private String title;
    private String text;
    
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

    public Long getAuthorID(){
        return this.author.getId();
    }
    public String getAuthorEmail(){
        return this.author.getEmail();
    }

    public void setAuthor(MyAppUser author){
        this.author = author;
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
