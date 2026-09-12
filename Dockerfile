FROM maven:3.9.11-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app /data/media \
    && chown -R app:app /app /data/media

WORKDIR /app
COPY --from=build --chown=app:app \
    /workspace/target/myproject-0.0.1-SNAPSHOT.jar app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
