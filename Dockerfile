FROM gradle:8.10.2-jdk23 AS builder

RUN gradle --version && java -version

WORKDIR /app

# Only copy dependency-related files
COPY build.gradle.kts settings.gradle.kts /app/

# Only download dependencies
# Eat the expected build failure since no source code has been copied yet
RUN gradle clean build --no-daemon > /dev/null 2>&1 || true

# Copy all files
COPY ./ /app/

# Do the actual build
RUN gradle clean build -x test --no-daemon

# Use a lightweight OpenJDK image to run the application
FROM openjdk:23-jdk-slim

# Copy the built application from the previous image
COPY --from=builder /app/build/libs/*.jar /app/app.jar

CMD ["java", "-jar", "/app/app.jar"]