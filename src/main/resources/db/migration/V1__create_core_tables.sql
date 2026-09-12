CREATE SEQUENCE IF NOT EXISTS my_app_user_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS letter_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS my_app_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(100),
    email VARCHAR(320) NOT NULL,
    password VARCHAR(255) NOT NULL,
    verification_token VARCHAR(1000),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    reset_token VARCHAR(255)
);

-- Обновляет старую таблицу пользователя без удаления существующих строк.
ALTER TABLE my_app_user ADD COLUMN IF NOT EXISTS username VARCHAR(100);
ALTER TABLE my_app_user ADD COLUMN IF NOT EXISTS email VARCHAR(320);
ALTER TABLE my_app_user ADD COLUMN IF NOT EXISTS password VARCHAR(255);
ALTER TABLE my_app_user ADD COLUMN IF NOT EXISTS verification_token VARCHAR(1000);
ALTER TABLE my_app_user ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE my_app_user ADD COLUMN IF NOT EXISTS reset_token VARCHAR(255);

ALTER TABLE my_app_user ALTER COLUMN username TYPE VARCHAR(100);
ALTER TABLE my_app_user ALTER COLUMN email TYPE VARCHAR(320);
ALTER TABLE my_app_user ALTER COLUMN password TYPE VARCHAR(255);
ALTER TABLE my_app_user ALTER COLUMN verification_token TYPE VARCHAR(1000);
ALTER TABLE my_app_user ALTER COLUMN reset_token TYPE VARCHAR(255);
UPDATE my_app_user SET password = '' WHERE password IS NULL;
UPDATE my_app_user SET is_verified = FALSE WHERE is_verified IS NULL;
ALTER TABLE my_app_user ALTER COLUMN email SET NOT NULL;
ALTER TABLE my_app_user ALTER COLUMN password SET NOT NULL;
ALTER TABLE my_app_user ALTER COLUMN is_verified SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_my_app_user_email ON my_app_user(email);

CREATE TABLE IF NOT EXISTS letter (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    user_email VARCHAR(320),
    public_token VARCHAR(64),
    security_key VARCHAR(128),
    version BIGINT DEFAULT 0,
    title VARCHAR(200),
    text VARCHAR(10000),
    expires_at TIMESTAMPTZ,
    burn_after_opening BOOLEAN DEFAULT FALSE,
    images_revision BIGINT DEFAULT 0,
    font_bold BOOLEAN,
    font_cursive BOOLEAN,
    font_underlined BOOLEAN,
    font_family VARCHAR(100),
    font_name VARCHAR(100),
    reaction_code VARCHAR(255)[]
);

-- В старой схеме это поле называлось author_email.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'letter'
          AND column_name = 'author_email'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'letter'
          AND column_name = 'user_email'
    ) THEN
        ALTER TABLE letter RENAME COLUMN author_email TO user_email;
    END IF;
END $$;

-- Сохраняет старое имя настройки шрифта.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'letter'
          AND column_name = 'letter_font_name'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'letter'
          AND column_name = 'font_name'
    ) THEN
        ALTER TABLE letter RENAME COLUMN letter_font_name TO font_name;
    END IF;
END $$;

ALTER TABLE letter ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE letter ADD COLUMN IF NOT EXISTS user_email VARCHAR(320);
ALTER TABLE letter ADD COLUMN IF NOT EXISTS public_token VARCHAR(64);
ALTER TABLE letter ADD COLUMN IF NOT EXISTS security_key VARCHAR(128);
ALTER TABLE letter ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE letter ADD COLUMN IF NOT EXISTS title VARCHAR(200);
ALTER TABLE letter ADD COLUMN IF NOT EXISTS text VARCHAR(10000);
ALTER TABLE letter ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;
ALTER TABLE letter ADD COLUMN IF NOT EXISTS burn_after_opening BOOLEAN DEFAULT FALSE;
ALTER TABLE letter ADD COLUMN IF NOT EXISTS images_revision BIGINT DEFAULT 0;
ALTER TABLE letter ADD COLUMN IF NOT EXISTS font_bold BOOLEAN;
ALTER TABLE letter ADD COLUMN IF NOT EXISTS font_cursive BOOLEAN;
ALTER TABLE letter ADD COLUMN IF NOT EXISTS font_underlined BOOLEAN;
ALTER TABLE letter ADD COLUMN IF NOT EXISTS font_family VARCHAR(100);
ALTER TABLE letter ADD COLUMN IF NOT EXISTS font_name VARCHAR(100);
ALTER TABLE letter ADD COLUMN IF NOT EXISTS reaction_code VARCHAR(255)[];

ALTER TABLE letter ALTER COLUMN user_email TYPE VARCHAR(320);
ALTER TABLE letter ALTER COLUMN public_token TYPE VARCHAR(64);
ALTER TABLE letter ALTER COLUMN security_key TYPE VARCHAR(128);
ALTER TABLE letter ALTER COLUMN title TYPE VARCHAR(200);
ALTER TABLE letter ALTER COLUMN text TYPE VARCHAR(10000);
ALTER TABLE letter ALTER COLUMN font_family TYPE VARCHAR(100);
ALTER TABLE letter ALTER COLUMN font_name TYPE VARCHAR(100);

UPDATE letter SET version = 0 WHERE version IS NULL;
UPDATE letter SET burn_after_opening = FALSE WHERE burn_after_opening IS NULL;
UPDATE letter SET images_revision = 0 WHERE images_revision IS NULL;
UPDATE letter
SET security_key = md5(random()::text || clock_timestamp()::text || id::text)
WHERE security_key IS NULL OR security_key = '';
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'letter'
          AND column_name = 'created_at'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'letter'
          AND column_name = 'ttl'
    ) THEN
        EXECUTE $sql$
            UPDATE letter
            SET expires_at = COALESCE(
                created_at + make_interval(mins => COALESCE(ttl, 1440)),
                CURRENT_TIMESTAMP + INTERVAL '24 hours'
            )
            WHERE expires_at IS NULL
        $sql$;
    ELSE
        UPDATE letter
        SET expires_at = CURRENT_TIMESTAMP + INTERVAL '24 hours'
        WHERE expires_at IS NULL;
    END IF;
END $$;
UPDATE letter SET title = '' WHERE title IS NULL;
UPDATE letter SET text = '' WHERE text IS NULL;

UPDATE letter AS l
SET user_id = u.id,
    user_email = u.email
FROM my_app_user AS u
WHERE l.user_id IS NULL
  AND l.user_email IS NOT NULL
  AND lower(u.email) = lower(l.user_email);

-- Не удаляет письма-сироты, а останавливает миграцию с понятной ошибкой.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM letter
        WHERE user_id IS NULL OR user_email IS NULL
    ) THEN
        RAISE EXCEPTION
            'Cannot migrate letter rows without an existing owner';
    END IF;
END $$;

ALTER TABLE letter ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE letter ALTER COLUMN user_email SET NOT NULL;
ALTER TABLE letter ALTER COLUMN public_token SET NOT NULL;
ALTER TABLE letter ALTER COLUMN security_key SET NOT NULL;
ALTER TABLE letter ALTER COLUMN version SET NOT NULL;
ALTER TABLE letter ALTER COLUMN title SET NOT NULL;
ALTER TABLE letter ALTER COLUMN text SET NOT NULL;
ALTER TABLE letter ALTER COLUMN expires_at SET NOT NULL;
ALTER TABLE letter ALTER COLUMN burn_after_opening SET NOT NULL;
ALTER TABLE letter ALTER COLUMN images_revision SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_letter_public_token ON letter(public_token);
CREATE UNIQUE INDEX IF NOT EXISTS uk_letter_security_key ON letter(security_key);
CREATE INDEX IF NOT EXISTS idx_letter_user_active
    ON letter(user_id, expires_at, id DESC);
CREATE INDEX IF NOT EXISTS idx_letter_author_active
    ON letter(lower(user_email), expires_at, id DESC);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_letter_user'
    ) THEN
        ALTER TABLE letter
            ADD CONSTRAINT fk_letter_user
            FOREIGN KEY (user_id) REFERENCES my_app_user(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Новые последовательности начинаются выше существующих идентификаторов.
SELECT setval(
    'my_app_user_seq',
    GREATEST((SELECT COALESCE(MAX(id), 0) + 50 FROM my_app_user), 1),
    FALSE
);
SELECT setval(
    'letter_seq',
    GREATEST((SELECT COALESCE(MAX(id), 0) + 50 FROM letter), 1),
    FALSE
);
