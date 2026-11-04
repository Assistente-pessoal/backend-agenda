# ---------- Etapa 1: build ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

COPY src ./src
RUN ./gradlew bootJar --no-daemon

RUN cp "$(ls build/libs/*.jar | grep -v plain | head -n 1)" /workspace/app.jar

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]