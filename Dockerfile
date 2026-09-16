# Multi-stage Dockerfile for Quarkus Traffic Light Controller
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy dependencies first for layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and package
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Minimal, secure runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user (UID 1000) for security & cloud compatibility
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -s /bin/sh -D appuser && \
    chown -R appuser:appgroup /app

# Copy the Quarkus fast-jar artifacts
COPY --from=build --chown=appuser:appgroup /workspace/target/quarkus-app/lib/ /app/lib/
COPY --from=build --chown=appuser:appgroup /workspace/target/quarkus-app/*.jar /app/
COPY --from=build --chown=appuser:appgroup /workspace/target/quarkus-app/app/ /app/app/
COPY --from=build --chown=appuser:appgroup /workspace/target/quarkus-app/quarkus/ /app/quarkus/

USER 1000
EXPOSE 8080 7860
ENV PORT=8080

ENTRYPOINT ["java", "-Dquarkus.http.host=0.0.0.0", "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", "-jar", "/app/quarkus-run.jar"]
