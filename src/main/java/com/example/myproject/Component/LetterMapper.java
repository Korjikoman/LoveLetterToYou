package com.example.myproject.Component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.myproject.DTO.LetterSummaryView;
import com.example.myproject.DTO.LetterView;
import com.example.myproject.Model.CachedLetter;
import com.example.myproject.Model.Letter;

@Component
public class LetterMapper {

    public LetterView toView(CachedLetter letter) {
        return new LetterView(
            letter.publicToken(),
      
            letter.authorName(),
            letter.title(),
            letter.text(),
            letter.expiresAt(),
            letter.burnAfterOpening(),
            hasPassword(letter.password()),
            copyList(letter.imagesPaths()),
            copyList(letter.reactions()),
            letter.fontSettings()
        );
    }

    public LetterSummaryView toSummaryView(CachedLetter cachedLetter) {
        return new LetterSummaryView(cachedLetter.publicToken(), cachedLetter.version(), cachedLetter.expiresAt());
    }

    public LetterSummaryView toSummaryView(Letter letter) {
        return new LetterSummaryView(letter.getPublicToken(), letter.getVersion(), letter.getExpiresAt());
    }

    public CachedLetter toCached(Letter letter) {
        return new CachedLetter(
            letter.getPublicToken(),
            letter.getSecurityKey(),
            letter.getAuthorEmail(),
            letter.getUser().getUsername(),
            letter.getTitle(),
            letter.getText(),
            letter.getPassword(),
            letter.getExpiresAt(),
            letter.getVersion(),
            letter.isBurn_after_opening(),
            copyList(letter.getImagesPaths()),
            copyList(letter.getReactions()),
            letter.getFont()
        );
        
    }


    private Boolean hasPassword(String password) {
        return password != null && !password.isBlank();
    }

    private List<String> copyList(List<String> source) {
        return source == null ? List.of() : List.copyOf(source);
    }

    

}
