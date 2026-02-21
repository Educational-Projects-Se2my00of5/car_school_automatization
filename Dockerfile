FROM gradle:9.3-jdk21-alpine AS builder

WORKDIR /app

COPY build.gradle.kts settings.gradle gradlew ./
COPY gradle gradle

RUN gradle dependencies --no-daemon || true

COPY src src

RUN gradle bootJar --no-daemon


FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/car_school_automatization-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]