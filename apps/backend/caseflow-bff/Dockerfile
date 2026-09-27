FROM gradle:8.10-jdk21 AS builder
WORKDIR /app
COPY . .
RUN tr -d '\r' < gradlew > gradlew.lf \
    && mv gradlew.lf gradlew \
    && chmod +x gradlew \
    && ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
