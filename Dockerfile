# Use an official OpenJDK runtime as the base image
FROM openjdk:22-jdk-slim as builder

# Install Maven in the builder image
RUN apt-get update && apt-get install -y maven

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download the dependencies
COPY pom.xml .

# Install dependencies
RUN mvn dependency:go-offline -B

COPY ..

# Package the application
RUN mvn clean package -DskipTests

# Second stage to reduce the size of the image
FROM openjdk:22-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the packaged JAR file from the builder stage
COPY --from=builder /app/target/devskill-api-0.0.1-SNAPSHOT.jar /app/devskill-api.jar

# Expose the port the app will run on (default Spring Boot port is 8080)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "--enable-preview", "-jar", "/app/devskill-api.jar"]
