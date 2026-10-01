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

<img width="850" height="849" alt="2026-10-01_19-39_2" src="https://github.com/user-attachments/assets/ac5220ea-a77b-4d9a-8c5b-abdb84431458" />
<img width="926" height="893" alt="2026-10-01_19-39_1" src="https://github.com/user-attachments/assets/ef08820b-ecac-4712-9ca2-c3de7f733284" />
<img width="713" height="736" alt="2026-10-01_19-39" src="https://github.com/user-attachments/assets/f2a4ab0d-e7b8-4472-b020-b5751f582d08" />
<img width="910" height="960" alt="2026-10-01_19-38" src="https://github.com/user-attachments/assets/55bde22d-ff04-4195-9d52-81385c623f00" />
<img width="1915" height="959" alt="2026-10-01_19-36_1" src="https://github.com/user-attachments/assets/0de2c6c0-f10d-4995-8894-928d9c185602" />
<img width="1907" height="999" alt="2026-10-01_19-36" src="https://github.com/user-attachments/assets/85d8f3de-0bf3-4008-8289-64dfd897515d" />
<img width="1921" height="1002" alt="2026-10-01_19-35" src="https://github.com/user-attachments/assets/a030570a-c2c7-40e5-8f57-2b3f7c4a34d2" />


