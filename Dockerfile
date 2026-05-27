# Stage 1: build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install Python, Chromium, and ChromeDriver (Alpine packages)
RUN apk add --no-cache \
    python3 \
    py3-pip \
    chromium \
    chromium-chromedriver \
  && python3 -m pip install --no-cache-dir selenium --break-system-packages

COPY --from=build /app/target/tidaroBot-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/login.py src/main/resources/login.py

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]