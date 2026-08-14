# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
RUN mvn -B -DskipTests dependency:go-offline

COPY src src
RUN mvn -B -DskipTests package && \
    cp "$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -print -quit)" /workspace/application.jar

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system spring && \
    useradd --system --gid spring spring && \
    mkdir -p /app/logs/audit && \
    chown -R spring:spring /app/logs
COPY --from=build --chown=spring:spring /workspace/application.jar application.jar

# Provide working upstreams in the image so it can be deployed without requiring
# users to configure the external document services first. These values can still
# be overridden with `docker run -e ...` or through an orchestrator.
ENV SALES_SYSTEM_BASE_URL="https://6a7a6b608c69b3eb4a173191.mockapi.io/api/external/sales-service" \
    SERVICE_SYSTEM_BASE_URL="https://6a7a6b608c69b3eb4a173191.mockapi.io/api/external/services-document"

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
