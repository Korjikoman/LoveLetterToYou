package com.example.myproject.Model;

import java.util.Objects;

import com.example.myproject.Images.Model.Image;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "letter_image",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_letter_image_position",
        columnNames = {"letter_id", "position"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LetterImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "letter_id", nullable = false)
    private Letter letter;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id", nullable = false, unique = true)
    private Image image;

    @Column(nullable = false)
    private int position;

    public LetterImage(Letter letter, Image image, int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Image position cannot be negative");
        }
        this.letter = Objects.requireNonNull(letter);
        this.image = Objects.requireNonNull(image);
        this.position = position;
    }

    public void setPosition(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Image position cannot be negative");
        }
        this.position = position;
    }
}
