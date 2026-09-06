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
            copyList(letter.images()),
            copyList(letter.reactions()),
            letter.fontSettings()
        );
    }

    public LetterSummaryView toSummaryView(CachedLetter cachedLetter) {
        return new LetterSummaryView(cachedLetter.publicToken(), cachedLetter.title(), cachedLetter.text(), cachedLetter.version(), cachedLetter.expiresAt());
    }

    public LetterSummaryView toSummaryView(Letter letter) {
        return new LetterSummaryView(letter.getPublicToken(), letter.getTitle(), letter.getText(), letter.getVersion(), letter.getExpiresAt());
    }

    public CachedLetter toCached(Letter letter) {
        return new CachedLetter(
            letter.getPublicToken(),
            letter.getSecurityKey(),
            letter.getAuthorEmail(),
            letter.getUser().getUsername(),
            letter.getTitle(),
            letter.getText(),
            letter.getExpiresAt(),
            letter.getVersion(),
            letter.isBurn_after_opening(),
            copyList(letter.getImages()),
            copyList(letter.getReactions()),
            letter.getFont()
        );
        
    }


    private static <T> List<T> copyList(List<T> source) {
        return source == null ? List.of() : List.copyOf(source);
    }

    

}
