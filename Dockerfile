# Multi-stage build: Frontend + Backend in one image
# Build context: parent directory containing both FE and BE folders
# Usage: 
#   docker compose build (uses docker-compose.yml settings)
#   docker build --build-arg FE_DIR=tms-sep490-fe --build-arg BE_DIR=tms-sep490-be -f tms-sep490-be/Dockerfile .

# Build arguments for directory names
ARG FE_DIR=tms-sep490-fe
ARG BE_DIR=tms-sep490-be

# Stage 1: Build Frontend
FROM node:20-alpine AS frontend-builder

ARG FE_DIR

WORKDIR /app/frontend

# Copy frontend source
COPY ${FE_DIR}/package.json ${FE_DIR}/pnpm-lock.yaml ./
COPY ${FE_DIR}/ ./

# Install dependencies và build
ENV CI=true
RUN npm install -g pnpm && \
    pnpm install --frozen-lockfile && \
    pnpm exec vite build

# Stage 2: Build Backend
FROM eclipse-temurin:21-jdk-alpine AS backend-builder

ARG BE_DIR

WORKDIR /app/backend

# Copy Maven wrapper và pom.xml
COPY ${BE_DIR}/mvnw .
COPY ${BE_DIR}/.mvn .mvn
COPY ${BE_DIR}/pom.xml .

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY ${BE_DIR}/src src

# Build application
RUN ./mvnw clean package -DskipTests -B

# Stage 3: Production - Combined Frontend + Backend
FROM eclipse-temurin:21-jre-alpine

ARG BE_DIR

WORKDIR /app

# Install nginx
RUN apk add --no-cache nginx

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy frontend build
COPY --from=frontend-builder /app/frontend/dist /usr/share/nginx/html

# Copy backend JAR
COPY --from=backend-builder /app/backend/target/tms-sep490-be-0.0.1-SNAPSHOT.jar app.jar

# Copy nginx config
COPY ${BE_DIR}/nginx-combined.conf /etc/nginx/http.d/default.conf

# Create nginx directories với proper permissions
RUN mkdir -p /var/log/nginx /var/lib/nginx/tmp /run/nginx && \
    chown -R appuser:appgroup /var/log/nginx /var/lib/nginx /run/nginx /usr/share/nginx/html /app

# Expose ports
EXPOSE 80 8080

# Create startup script
RUN echo '#!/bin/sh' > /app/start.sh && \
    echo 'nginx' >> /app/start.sh && \
    echo 'exec java -jar /app/app.jar' >> /app/start.sh && \
    chmod +x /app/start.sh

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Start both nginx và Spring Boot
CMD ["/app/start.sh"]
