FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup \
    && adduser -S appuser -G appgroup \
    && mkdir -p /app/logs \
    && chown -R appuser:appgroup /app

COPY app.jar /app/app.jar

USER appuser

EXPOSE 35698

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:35698/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
