# Build stage - Maven pre-installed (Debian-based, better DNS resolution)
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application (downloads dependencies + compiles + packages)
RUN mvn package -DskipTests -B

# Runtime stage - lightweight
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
