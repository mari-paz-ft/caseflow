# Multi-stage build for Kotlin Spring Boot
FROM gradle:8.10-jdk21 AS builder
WORKDIR /app
COPY . .
RUN tr -d '\r' < gradlew > gradlew.lf \
    && mv gradlew.lf gradlew \
    && chmod +x gradlew \
    && ./gradlew build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
