package com.example.myproject.Model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.example.myproject.DTO.FontData;
import com.example.myproject.Images.Model.Image;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "letter")
public class Letter {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "letter_seq")
    @SequenceGenerator(name = "letter_seq", sequenceName = "letter_seq", allocationSize = 50)
    private Long id;

    @Column(name = "author_email", nullable = false, length = 320)
    private String authorEmail;
    
    @Column(unique = true, nullable = false, updatable = false, length = 64)
    private String publicToken;

    @Column(unique = true, nullable = false, updatable = false, length = 128)
    private String securityKey;

    
    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 10_000)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private MyAppUser user;
    
    @Column(nullable = false)
    private Instant expiresAt;

    @Column(name="burn_after_opening", nullable = false)
    private boolean burnAfterOpening;


    @OneToMany(
        mappedBy =  "letter",
        cascade = CascadeType.ALL, 
        orphanRemoval = true
    )
    @OrderBy("position ASC")
    private List<LetterImage> imageLinks = new ArrayList<>();

    @Column(name = "images_revision", nullable = false)
    private long imagesRevision;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "isFontBold", column = @Column(name = "font_bold")),
        @AttributeOverride(name = "isFontCursive", column = @Column(name = "font_cursive")),
        @AttributeOverride(name = "isFontUnderlined", column = @Column(name = "font_underlined")),
        @AttributeOverride(name = "fontFamily", column = @Column(name = "font_family", length = 100)),
        @AttributeOverride(name = "fontName", column = @Column(name = "font_name", length = 100))
    })
    private FontData font;
    
    @Column(name= "reaction_code")
    private List<String> reactions;

    public void addImage(Image image, int position) {
        imageLinks.add(new LetterImage(this, image, position));
    }

    public void removeImage(LetterImage link) {
        imageLinks.remove(link);
    }

    public void incrementImagesRevision() {
        imagesRevision++;
    }
}
