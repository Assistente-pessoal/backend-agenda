# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN --mount=type=secret,id=proxy_ca \
    --mount=type=secret,id=gradle_properties,target=/root/.gradle/gradle.properties \
    set -eu; \
    if [ -f /run/secrets/proxy_ca ]; then \
        truststore="$(mktemp)"; \
        trap 'rm -f "$truststore"' EXIT; \
        cp "$JAVA_HOME/lib/security/cacerts" "$truststore"; \
        keytool -importcert -noprompt -alias build-proxy-ca \
            -file /run/secrets/proxy_ca -keystore "$truststore" -storepass changeit; \
        export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djavax.net.ssl.trustStore=$truststore"; \
    fi; \
    ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
