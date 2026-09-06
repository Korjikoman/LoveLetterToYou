CREATE TABLE letter_images (
    letter_id BIGINT NOT NULL,
    image_path VARCHAR(512) NOT NULL,
    position INT NOT NULL,
    CONSTRAINT pk_letter_images PRIMARY_KEY (letter_id, position),
    CONSTRAINT fk_letter_images_letter FOREIGN KEY (letter_id) REFERENCES letter(id) ON DELETE CASCADE
);

CREATE INDEX idx_image_path ON letter_images(image_path);