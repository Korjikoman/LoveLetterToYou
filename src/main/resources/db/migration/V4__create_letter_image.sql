CREATE TABLE IF NOT EXISTS letter_image (
    id BIGSERIAL PRIMARY KEY,
    letter_id BIGINT NOT NULL,
    image_id UUID NOT NULL,
    position INTEGER NOT NULL,

    CONSTRAINT fk_letter_image_letter
        FOREIGN KEY (letter_id) REFERENCES letter(id) ON DELETE CASCADE,
    CONSTRAINT fk_letter_image_image
        FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE RESTRICT,
    CONSTRAINT uk_letter_image_image UNIQUE (image_id),
    CONSTRAINT uk_letter_image_position
        UNIQUE (letter_id, position) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT check_letter_image_position CHECK (position >= 0)
);

CREATE INDEX IF NOT EXISTS idx_letter_image_order ON letter_image(letter_id, position);

ALTER TABLE my_app_user
    ADD COLUMN IF NOT EXISTS avatar_image_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_avatar_image
    ON my_app_user(avatar_image_id)
    WHERE avatar_image_id IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_avatar_image'
    ) THEN
        ALTER TABLE my_app_user
            ADD CONSTRAINT fk_user_avatar_image
            FOREIGN KEY (avatar_image_id) REFERENCES image(id) ON DELETE SET NULL;
    END IF;
END $$;
