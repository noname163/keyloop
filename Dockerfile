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

RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build --chown=spring:spring /workspace/application.jar application.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
