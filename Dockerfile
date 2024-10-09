# Use an official Gradle image as a parent image
FROM gradle:8.10.2-jdk23 AS builder

# Set the working directory in the container
WORKDIR /app

# Copy the Gradle build files and wrapper files
# Ensure the `gradlew` script and `gradle-wrapper.jar` are copied to the correct locations
COPY build.gradle.kts settings.gradle.kts gradlew gradlew.bat /app/
COPY gradle/wrapper/ /app/gradle/wrapper/

# Run dependency resolution first
RUN ./gradlew dependencies --no-daemon

# Copy the entire project source to the container
COPY . /app/

# Build the project using Gradle (you can specify more tasks if needed)
RUN ./gradlew build -x test --no-daemon

# Use a lightweight OpenJDK image to run the application
FROM openjdk:23-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the built application from the previous image
COPY --from=builder /app/build/libs/*.jar /app/api_gateway.jar

# Expose the application port
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "/app/api_gateway.jar"]
