-- Подтверждение email в приложении не используется.
ALTER TABLE my_app_user
    DROP COLUMN IF EXISTS verification_token,
    DROP COLUMN IF EXISTS is_verified;
