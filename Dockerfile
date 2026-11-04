# Build stage
FROM maven:3.9.4-eclipse-temurin-8 AS builder
WORKDIR /workspace
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Download and extract Tomcat in build stage
RUN curl -L -o /tmp/tomcat.tar.gz https://archive.apache.org/dist/tomcat/tomcat-8.5.92/bin/apache-tomcat-8.5.92.tar.gz && \
    tar -xzf /tmp/tomcat.tar.gz -C /opt/ && \
    mv /opt/apache-tomcat-8.5.92 /opt/tomcat && \
    rm -rf /opt/tomcat/webapps/* && \
    rm -f /tmp/tomcat.tar.gz

# Runtime stage
FROM amazoncorretto:8
WORKDIR /opt/tomcat
COPY --from=builder /opt/tomcat /opt/tomcat
COPY --from=builder /workspace/target/*.war /opt/tomcat/webapps/ROOT.war

# Remove the Tomcat manager and host-manager apps for security
RUN rm -rf /opt/tomcat/webapps/manager /opt/tomcat/webapps/host-manager

# Non-root user
RUN groupadd -r -g 1000 tomcat && \
    useradd -r -u 1000 -g tomcat -G root tomcat && \
    chown -R tomcat:tomcat /opt/tomcat && \
    chmod +x /opt/tomcat/bin/*.sh
USER 1000

EXPOSE 8080

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV TZ=UTC
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# SPRING_PROFILES_ACTIVE not applicable for this non-Spring-Boot app
# Health check endpoint: /health (served by HealthCheckServlet)

ENTRYPOINT ["/opt/tomcat/bin/catalina.sh", "run"]
