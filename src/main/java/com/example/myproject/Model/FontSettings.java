package com.example.myproject.Model;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class FontSettings {
    private boolean bold;
    private boolean cursive;
    private boolean underlined;
    private String family;
    private String name;
    
    public FontSettings(boolean bold, boolean cursive, boolean underlined, String family, String name) {
        this.bold = bold;
        this.cursive = cursive;
        this.underlined = underlined;
        this.family = family;
        this.name = name;
    }

    public FontSettings() {}
}
