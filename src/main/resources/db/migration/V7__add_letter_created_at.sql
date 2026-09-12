-- Дата создания является частью Letter и заполняется приложением для новых писем.
ALTER TABLE letter
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;

-- Старая схема использовала timestamp без часового пояса.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'letter'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE letter
            ALTER COLUMN created_at TYPE TIMESTAMPTZ
            USING created_at AT TIME ZONE 'UTC';
    END IF;
END $$;

-- Страхует данные из промежуточных версий схемы, где столбец мог быть nullable.
UPDATE letter
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

ALTER TABLE letter
    ALTER COLUMN created_at SET NOT NULL;
