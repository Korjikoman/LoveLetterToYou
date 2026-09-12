-- Имя столбца должно совпадать с @Column в Letter.authorEmail.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'letter'
          AND column_name = 'user_email'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'letter'
          AND column_name = 'author_email'
    ) THEN
        ALTER TABLE letter RENAME COLUMN user_email TO author_email;
    END IF;
END $$;
