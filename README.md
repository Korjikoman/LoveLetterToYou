# LoveLetterToYou.

LoveLetterToYou — это сервис, который позволяет пользователю создавать и быстро отправлять любовные письма своей второй половинке. Проект ориентирован на простоту и удобство: вы создаёте письмо, получаете уникальный URL и делитесь им, чтобы получатель мог прочитать ваше послание.

## Функционал.

 - Регистрация и авторизация пользователей.
 - Создание любовного письма и получение уникального URL для его просмотра.
 - Просмотр всех ранее созданных писем.
 - Редактирование профиля пользователя.

## Технологический стек.

 - Backend: Spring Boot
 - Frontend: Pure JavaScript + Thymeleaf
 - База данных: PostgreSQL, Redis
 - Proxy: Nginx
 - Контейнеризация: Docker + Docker Compose

## Запуск проекта:

 ### 1. Клонируйте репозиторий:
```
git clone https://github.com/your-username/LoveLetterToYou.git
cd LoveLetterToYou
```
### 2. Добавьте .env файл в корень проекта.

  Пример:
  ```
  NGINX_HOST=localhost
  NGINX_PORT=80


  MAIN_HOST=127.0.0.1
  MAIN_PORT=8080

  OUTBOX_POLL_DELAY_MS=2000

  POSTGRES_HOST=localhost
  POSTGRES_PORT=5432
  POSTGRES_DB=registration
  POSTGRES_USER=loveletter_dev
  POSTGRES_PASSWORD=loveletter_dev_123

  REDIS_HOST=localhost
  REDIS_PORT=6379

```

### 3. Соберите и запустите сервис с Docker Compose:
```
docker-compose up --build
```
## Скриншоты.

<img width="745" height="794" alt="image" src="https://github.com/user-attachments/assets/2ea55b8f-c137-4b23-ab70-56a339e1eb20" />
<img width="956" height="946" alt="image" src="https://github.com/user-attachments/assets/9c260756-6fae-417e-8892-8224c2609321" />
<img width="825" height="948" alt="image" src="https://github.com/user-attachments/assets/f9783b55-5edd-4be3-b06d-9c57321d9a46" />
<img width="637" height="419" alt="image" src="https://github.com/user-attachments/assets/e263842a-bae6-429f-a294-788b245814fc" />
<img width="1392" height="859" alt="image" src="https://github.com/user-attachments/assets/7c101ca4-27f4-4661-9b0f-3cf91d2204a8" />
<img width="1896" height="518" alt="image" src="https://github.com/user-attachments/assets/f267b2fc-2382-4adf-8a02-be0ad6378936" />




