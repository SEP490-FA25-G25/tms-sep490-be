# Multi-stage build: Frontend + Backend in one image
# Build context: parent directory (~/projects/tms/)
# Hardcoded for VPS deployment with tms-fe-temp and tms-be-temp

# Stage 1: Build Frontend
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend

# Copy frontend source
COPY tms-fe-temp/package.json tms-fe-temp/pnpm-lock.yaml ./
COPY tms-fe-temp/ ./

# Install dependencies và build
ENV CI=true
RUN npm install -g pnpm && \
    pnpm install --frozen-lockfile && \
    pnpm exec vite build

# Stage 2: Build Backend
FROM eclipse-temurin:21-jdk-alpine AS backend-builder

WORKDIR /app/backend

# Copy Maven wrapper và pom.xml
COPY tms-be-temp/mvnw .
COPY tms-be-temp/.mvn .mvn
COPY tms-be-temp/pom.xml .

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY tms-be-temp/src src

# Build application (skip test compilation and execution)
RUN ./mvnw clean package -Dmaven.test.skip=true -B

# Stage 3: Production - Combined Frontend + Backend
FROM eclipse-temurin:21-jre-alpine

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
COPY tms-be-temp/nginx-combined.conf /etc/nginx/http.d/default.conf

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
