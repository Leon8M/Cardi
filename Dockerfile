# Use a build stage with Maven and JDK to build the application
FROM maven:3.8.5-openjdk-17 AS build

# Copy the source code
WORKDIR /app
COPY . .

# Build the application, skipping tests for faster deployment
RUN mvn clean install -DskipTests

# Use a runtime stage with just the JRE for a smaller image
FROM eclipse-temurin:17-jre-jammy

# Copy the built JAR from the build stage
WORKDIR /app
COPY --from=build /app/target/cardi-0.0.1-SNAPSHOT.jar ./cardi.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "./cardi.jar"]
