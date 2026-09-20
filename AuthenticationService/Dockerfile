# ====================================================================
# Multi-Stage Dockerfile for AuthenticationService (Render / Cloud Container)
# Stage 1: Build JAR using Maven & OpenJDK 17
# Stage 2: Minimal Alpine JRE Runtime
# ====================================================================

FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy pom and source code to package
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root system user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy executable jar from build stage
COPY --from=builder /app/target/*.jar app.jar

# Render assigns a dynamic port via the $PORT environment variable (defaults to 8082 locally)
ENV PORT=8082
EXPOSE ${PORT}

# Run Spring Boot application
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
