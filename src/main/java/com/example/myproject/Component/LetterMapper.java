package com.example.myproject.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.myproject.DTO.LetterSummaryView;
import com.example.myproject.DTO.LetterView;
import com.example.myproject.Images.DTO.ImageView;
import com.example.myproject.Model.CachedLetter;
import com.example.myproject.Model.Letter;
import com.example.myproject.Model.LetterImage;

@Component
public class LetterMapper {

    private final ImageMapper imageMapper;

    public LetterMapper(ImageMapper imageMapper) {
        this.imageMapper = imageMapper;
    }

    public LetterView toView(CachedLetter letter) {
        return new LetterView(
            letter.publicToken(),
            letter.version(),
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
            letter.isBurnAfterOpening(),
            toImageViews(letter),
            copyList(letter.getReactions()),
            letter.getFont()
        );
        
    }

    /** Отбирает готовые изображения письма и сохраняет заданный порядок. */
    private List<ImageView> toImageViews(Letter letter) {
        if (letter.getImageLinks() == null || letter.getImageLinks().isEmpty()) {
            return List.of();
        }

        List<LetterImage> orderedLinks = new ArrayList<>(letter.getImageLinks());
        orderedLinks.sort(Comparator.comparingInt(LetterImage::getPosition));

        List<ImageView> views = new ArrayList<>(orderedLinks.size());
        for (LetterImage link : orderedLinks) {
            if (imageMapper.isVisibleLetterImage(link)) {
                views.add(imageMapper.toView(link));
            }
        }

        return List.copyOf(views);
    }


    private static <T> List<T> copyList(List<T> source) {
        return source == null ? List.of() : List.copyOf(source);
    }

    

}
