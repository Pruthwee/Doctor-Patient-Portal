# Build stage: compile WAR and fetch Tomcat
FROM maven:3.8.6-openjdk-8-slim AS builder
WORKDIR /workspace

# Copy pom.xml first and resolve dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build WAR
COPY src ./src
RUN mvn clean package -DskipTests

# Download and extract Apache Tomcat 8.5
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget ca-certificates && \
    wget -q https://archive.apache.org/dist/tomcat/tomcat-8/v8.5.81/bin/apache-tomcat-8.5.81.tar.gz -O /tmp/tomcat.tar.gz && \
    tar -xzf /tmp/tomcat.tar.gz -C /opt && \
    mv /opt/apache-tomcat-8.5.81 /opt/tomcat && \
    rm /tmp/tomcat.tar.gz && \
    apt-get purge -y wget && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Runtime stage
FROM eclipse-temurin:8-jdk-alpine
WORKDIR /opt/tomcat

# Copy Tomcat from builder
COPY --from=builder /opt/tomcat /opt/tomcat

# Copy the WAR file as ROOT.war (deployed at root context)
COPY --from=builder /workspace/target/*.war /opt/tomcat/webapps/ROOT.war

# Create non-root user
RUN addgroup -S tomcat && adduser -S -G tomcat tomcat && \
    chown -R tomcat:tomcat /opt/tomcat

USER tomcat

# Expose Tomcat's default HTTP port
EXPOSE 8080

# Environment variables for JVM tuning
ENV CATALINA_HOME=/opt/tomcat
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV TZ=UTC
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.timezone=UTC"

# Spring Session / Redis configuration via environment variables
ENV REDIS_HOST=""
ENV REDIS_PORT=""
ENV REDIS_PASSWORD=""

# Database configuration via environment variables
ENV DB_HOST=""
ENV DB_PORT=""
ENV DB_NAME=""
ENV DB_USER=""
ENV DB_PASSWORD=""

# Start Tomcat with proper signal handling
ENTRYPOINT ["sh", "-c", "exec $CATALINA_HOME/bin/catalina.sh run"]
