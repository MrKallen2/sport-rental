# syntax=docker/dockerfile:1

# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN --mount=type=cache,target=/root/.m2,id=maven-repo,sharing=shared \
    mvn -B -q -DskipTests package

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/target/*.jar app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
