# Multi-stage build: Frontend + Backend in one image
# Build context: .. (CAPSTONE folder)
# Stage 1: Build Frontend (bỏ qua TypeScript check và lint)
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend

# Copy frontend source (from CAPSTONE/tms-sep490-fe/)
COPY tms-sep490-fe/package.json tms-sep490-fe/pnpm-lock.yaml ./
COPY tms-sep490-fe/ ./

# Install dependencies và build (bỏ qua tsc và lint)
ENV CI=true
RUN npm install -g pnpm && \
    pnpm install --frozen-lockfile && \
    pnpm exec vite build

# Stage 2: Build Backend
FROM eclipse-temurin:21-jdk-alpine AS backend-builder

WORKDIR /app/backend

# Copy Maven wrapper và pom.xml (from CAPSTONE/tms-sep490-be/)
COPY tms-sep490-be/mvnw .
COPY tms-sep490-be/.mvn .mvn
COPY tms-sep490-be/pom.xml .

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies (cached nếu pom.xml không đổi)
RUN ./mvnw dependency:go-offline -B

# Copy source code (from CAPSTONE/tms-sep490-be/src)
COPY tms-sep490-be/src src

# Build application (skip tests)
RUN ./mvnw clean package -DskipTests -B

# Stage 3: Production - Combined Frontend + Backend
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install nginx để serve frontend
RUN apk add --no-cache nginx

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy frontend build từ stage 1
COPY --from=frontend-builder /app/frontend/dist /usr/share/nginx/html

# Copy backend JAR từ stage 2
COPY --from=backend-builder /app/backend/target/tms-sep490-be-0.0.1-SNAPSHOT.jar app.jar

# Copy nginx config (from CAPSTONE/tms-sep490-be/)
COPY tms-sep490-be/nginx-combined.conf /etc/nginx/http.d/default.conf

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
