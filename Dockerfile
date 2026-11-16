# Build stage: compile WAR with Maven
FROM maven:3.8.6-openjdk-8-slim AS builder
WORKDIR /workspace

# Download Tomcat 9 for the runtime stage
RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && wget -q https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.79/bin/apache-tomcat-9.0.79.tar.gz -O /tmp/tomcat.tar.gz \
    && tar -xzf /tmp/tomcat.tar.gz -C /opt \
    && rm /tmp/tomcat.tar.gz \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Copy pom and resolve dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src/ src/
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM amazoncorretto:8
ENV TZ=UTC \
    LANG=en_US.utf8 \
    LC_ALL=en_US.utf8 \
    JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Create non-root user
RUN groupadd -r tomcat && useradd -r -g tomcat -d /opt/tomcat -s /sbin/nologin tomcat

# Copy Tomcat from builder
COPY --from=builder /opt/apache-tomcat-9.0.79 /opt/tomcat
# Copy the WAR as ROOT context
COPY --from=builder /workspace/target/*.war /opt/tomcat/webapps/ROOT.war

# Set ownership and switch to non-root
RUN chown -R tomcat:tomcat /opt/tomcat \
    && chmod +x /opt/tomcat/bin/catalina.sh

USER tomcat

EXPOSE 8080

ENTRYPOINT ["/opt/tomcat/bin/catalina.sh", "run"]
