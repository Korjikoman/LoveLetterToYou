package com.example.myproject.Component;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.example.myproject.Images.DTO.ImagePurpose;
import com.example.myproject.Images.DTO.ImageStatus;
import com.example.myproject.Images.DTO.ImageView;
import com.example.myproject.Images.Model.Image;
import com.example.myproject.Model.LetterImage;

@Component
public class ImageMapper {

    private static final String CONTENT_URL_PREFIX = "/api/images/";
    private static final String CONTENT_URL_SUFFIX = "/content";

    /** Возвращает true, только если изображение уже прикреплено к письму. */
    public boolean isVisibleLetterImage(LetterImage link) {
        if (link == null || link.getImage() == null) {
            return false;
        }

        Image image = link.getImage();
        return image.getStatus() == ImageStatus.ATTACHED
            && image.getImagePurpose() == ImagePurpose.LETTER;
    }

    /** Создаёт безопасное представление изображения без сущностей JPA. */
    public ImageView toView(LetterImage link) {
        Objects.requireNonNull(link, "Связь изображения с письмом не указана");

        Image image = Objects.requireNonNull(
            link.getImage(),
            "Изображение в связи с письмом не указано"
        );

        return new ImageView(
            image.getId(),
            CONTENT_URL_PREFIX + image.getId() + CONTENT_URL_SUFFIX,
            link.getPosition()
        );
    }
}
