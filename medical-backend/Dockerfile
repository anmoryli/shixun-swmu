# syntax=docker/dockerfile:1.7

FROM maven:3.9.16-eclipse-temurin-17-noble AS build
WORKDIR /workspace

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp clean verify && \
    JAR_PATH="$(find target -maxdepth 1 -type f -name 'medical-backend-*.jar' \
      ! -name '*.original' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1)" && \
    test -n "$JAR_PATH" && cp "$JAR_PATH" /workspace/app.jar

FROM eclipse-temurin:17.0.19_10-jre-alpine AS runtime

RUN addgroup -S -g 10001 medicine && \
    adduser -S -D -H -u 10001 -G medicine medicine && \
    mkdir -p /app/uploads && \
    chown -R medicine:medicine /app

WORKDIR /app
COPY --from=build --chown=medicine:medicine /workspace/app.jar /app/app.jar
COPY --chown=medicine:medicine --chmod=755 docker-entrypoint.sh /app/docker-entrypoint.sh

ENV SERVER_PORT=8082 \
    APP_UPLOAD_DIRECTORY=/app/uploads \
    SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=2MB \
    SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=3MB \
    SERVER_SHUTDOWN=graceful \
    SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=30s \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true -Duser.timezone=Asia/Shanghai"

EXPOSE 8082
VOLUME ["/app/uploads"]

USER 10001:10001
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O - http://127.0.0.1:8082/actuator/health | grep -q '^\{"status":"UP"\}$' || exit 1
STOPSIGNAL SIGTERM
ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD ["java", "-jar", "/app/app.jar"]
