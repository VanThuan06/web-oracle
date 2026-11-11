# ==============================================================================
# 1. BUILD STAGE (Maven + JDK 21)
# ==============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build executable jar
COPY src ./src
RUN mvn clean package -DskipTests

# ==============================================================================
# 2. RUNTIME STAGE (Lightweight JRE 21 Alpine)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install wget for Docker healthcheck
RUN apk add --no-cache wget

# Create non-root user
RUN addgroup -S newmen && adduser -S newmen -G newmen

# Copy built application jar
COPY --from=builder /build/target/*.jar app.jar

# Copy initial media folder
COPY media ./media

# Create uploads directory with appropriate permissions
RUN mkdir -p uploads && chown -R newmen:newmen /app

USER newmen

EXPOSE 8188

ENV SPRING_PROFILES_ACTIVE=dev \
    PORT=8188 \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

HEALTHCHECK --interval=15s --timeout=5s --start-period=25s --retries=5 \
  CMD wget -q -O - http://localhost:8188/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
