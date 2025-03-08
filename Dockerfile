# Step 1: Build the Spring Boot app using Maven
FROM maven:3.8.6-openjdk-17-slim AS build

# Set the working directory for Maven
WORKDIR /app

# Copy the pom.xml and the src folder to the container
COPY pom.xml .
COPY src ./src

# Build the application (creates the .jar file)
RUN mvn clean package -DskipTests

# Step 2: Create a minimal image to run the application
FROM openjdk:17-jdk-slim

# Set the working directory for the application
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port the application will run on
EXPOSE 8080

# Run the Spring Boot application
CMD ["java", "-jar", "app.jar"]
