# Stage 1: Build stage
FROM maven:3.9.4-eclipse-temurin-8 AS builder
WORKDIR /workspace

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:8-jdk
WORKDIR /app

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy the WAR file from the builder stage
COPY --from=builder /workspace/target/Doctor-Patient-Portal.war app.war

# Set JVM memory settings and container awareness
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV TZ=UTC

# Expose the application port (Default for Tomcat/WAR is 8080)
EXPOSE 8080

# Use a lightweight server to run the WAR (since it's a WAR project)
# In a real production scenario, we would use a Tomcat base image or Spring Boot executable JAR.
# For this transformation, we'll use a simple approach to run the WAR.
RUN apt-get update && apt-get install -y tomcat9 && apt-get clean

# Copy WAR to Tomcat webapps
COPY app.war /var/lib/tomcat9/webapps/ROOT.war

USER appuser

CMD ["catalina.sh", "run"]
