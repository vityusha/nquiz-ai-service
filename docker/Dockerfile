# syntax=docker/dockerfile:1

#
# To build JAR during docker build
#
# FROM eclipse-temurin:21-jdk-alpine AS build
#
# WORKDIR /workspace
# 
# COPY gradle gradle
# COPY gradlew gradlew.bat build.gradle settings.gradle gradle.properties ./
# RUN chmod +x gradlew

# COPY src src
# RUN ./gradlew shadowJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup \
    && adduser -S appuser -G appgroup \
    && apk add --no-cache curl \
    && mkdir -p /var/lib/nquiz-ai-service/data \
    && chown -R appuser:appgroup /var/lib/nquiz-ai-service

COPY build/libs/*-all.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

VOLUME ["/var/lib/nquiz-ai-service/data"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
