FROM eclipse-temurin:17.0.19_10-jre-alpine

RUN addgroup -S -g 10001 medicine && \
    adduser -S -D -H -u 10001 -G medicine medicine && \
    mkdir -p /app/uploads && chown -R medicine:medicine /app

WORKDIR /app
COPY --chown=medicine:medicine app.jar /app/app.jar
COPY --chown=medicine:medicine --chmod=755 docker-entrypoint.sh /app/docker-entrypoint.sh

ENV SERVER_PORT=8082 \
    APP_UPLOAD_DIRECTORY=/app/uploads \
    SERVER_SHUTDOWN=graceful \
    SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=30s \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true -Duser.timezone=Asia/Shanghai"

EXPOSE 8082
USER 10001:10001
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=6 \
  CMD wget -q -O - http://127.0.0.1:8082/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD ["java", "-jar", "/app/app.jar"]
