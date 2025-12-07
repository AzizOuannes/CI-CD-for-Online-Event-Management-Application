FROM eclipse-temurin:17-jdk-jammy

# Create app directory
WORKDIR /app

# Copy the fat jar produced by Maven into the image
# The build stage (Jenkinsfile) archives target/*.jar, ensure the JAR is present
COPY target/*.jar /app/app.jar

# Expose default Spring Boot port
EXPOSE 8080

# Run the application
ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar"]
