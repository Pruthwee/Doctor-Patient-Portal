# syntax=docker/dockerfile:1

# ---- Stage 1: Tomcat 9 (javax.servlet 4.0) distribution, copied into the runtime stage ----
FROM tomcat:9.0-jdk8-corretto AS tomcat

# ---- Stage 2: build the WAR with Maven on Java 8 ----
FROM maven:3.8.6-openjdk-8-slim AS builder
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Stage 3: runtime on Amazon Corretto 8 ----
FROM amazoncorretto:8

ENV CATALINA_HOME=/opt/tomcat \
    JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Duser.timezone=UTC -Djava.security.egd=file:/dev/./urandom" \
    TZ=UTC \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    REDIS_HOST=localhost \
    REDIS_PORT=6379 \
    REDIS_SSL=false

COPY --from=tomcat /usr/local/tomcat /opt/tomcat
RUN rm -rf /opt/tomcat/webapps/* \
    && groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid app --home-dir /opt/tomcat --shell /sbin/nologin app

# The portal is served at the root context path (/), health check at /health
COPY --from=builder /workspace/target/Doctor-Patient-Portal.war /opt/tomcat/webapps/ROOT.war
RUN chown -R app:app /opt/tomcat

USER 1001:1001
WORKDIR /opt/tomcat

EXPOSE 8080

# exec: java (started by catalina.sh run) becomes the first process and receives SIGTERM for a graceful shutdown
ENTRYPOINT ["sh", "-c", "exec /opt/tomcat/bin/catalina.sh run"]
