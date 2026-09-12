-- Наличие аватара теперь определяется связью my_app_user.avatar_image_id -> image.id.
-- Старый обязательный флаг больше не отображается в MyAppUser и мешает создавать пользователей.
ALTER TABLE my_app_user
    DROP COLUMN IF EXISTS user_has_avatar;
