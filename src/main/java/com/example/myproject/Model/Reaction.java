package com.example.myproject.Model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Reaction {
    private String emojiCode;
}
