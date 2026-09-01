FROM eclipse-temurin:25-jdk-alpine

WORKDIR /

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]