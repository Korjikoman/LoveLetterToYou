#LoveLetterToYou.

LoveLetterToYou — это сервис, который позволяет пользователю создавать и быстро отправлять любовные письма своей второй половинке. Проект ориентирован на простоту и удобство: вы создаёте письмо, получаете уникальный URL и делитесь им, чтобы получатель мог прочитать ваше послание.

##Функционал.

 - Регистрация и авторизация пользователей.
 - Создание любовного письма и получение уникального URL для его просмотра.
 - Просмотр всех ранее созданных писем.
 - Редактирование профиля пользователя.

##Технологический стек.

 - Backend: Spring Boot
 - Frontend: Pure JavaScript + Thymeleaf
 - База данных: PostgreSQL, Redis
 - Load Balancer: Nginx
 - Контейнеризация: Docker + Docker Compose

##Запуск проекта:

 ###1. Клонируйте репозиторий:
```
git clone https://github.com/your-username/LoveLetterToYou.git
cd LoveLetterToYou
```
###2. Создайте и Настройте application.yml с параметрами вашей БД и Redis:
   Пример:
   ```
   spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-gmail@gmail.com
    password: your_password
    protocol: smtp
    properties:
      mail:
        debug: true
        smtp:
          auth: true
          starttls:
            enable: true
  datasource:
    url: jdbc:postgresql://localhost:5432/registration
    username: postgres_username
    password: yuor_password
  data:
    redis:
      host: localhost
      port: 6379

  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect 
    show-sql: true

app:
  public-url: http://localhost:8080
```

###3. Соберите и запустите сервис с Docker Compose:
```
docker-compose up --build
```
##Скриншоты.

<img width="745" height="794" alt="image" src="https://github.com/user-attachments/assets/2ea55b8f-c137-4b23-ab70-56a339e1eb20" />
<img width="956" height="946" alt="image" src="https://github.com/user-attachments/assets/9c260756-6fae-417e-8892-8224c2609321" />
<img width="825" height="948" alt="image" src="https://github.com/user-attachments/assets/f9783b55-5edd-4be3-b06d-9c57321d9a46" />
<img width="637" height="419" alt="image" src="https://github.com/user-attachments/assets/e263842a-bae6-429f-a294-788b245814fc" />
<img width="1392" height="859" alt="image" src="https://github.com/user-attachments/assets/7c101ca4-27f4-4661-9b0f-3cf91d2204a8" />
<img width="1896" height="518" alt="image" src="https://github.com/user-attachments/assets/f267b2fc-2382-4adf-8a02-be0ad6378936" />




