# Development Commands - TMS SEP490 Backend

## Environment Setup (Windows - REQUIRED FIRST)
```bash
# MUST run this before any Maven commands
export JAVA_HOME="/c/Users/YourUsername/.jdks/openjdk-21.0.1"
export PATH="$JAVA_HOME/bin:$PATH"

# Verify setup
java -version
./mvnw -version
```

## Build & Run (Use Maven Wrapper)
```bash
# Clean and compile
./mvnw clean compile

# Run application (port 8080)
./mvnw spring-boot:run

# Build JAR (skip tests)
./mvnw clean package -DskipTests

# Run JAR
java -jar target/tms-sep490-be-0.0.1-SNAPSHOT.jar
```

## Testing Commands (Use Maven Wrapper)
```bash
# Run all tests with coverage
./mvnw clean verify

# Run unit tests only
./mvnw test

# Run specific test class
./mvnw test -Dtest=CenterServiceImplTest

# Run specific test method
./mvnw test -Dtest=CenterServiceImplTest#shouldFindCenterById

# Run tests in parallel (faster)
./mvnw -T 1C clean verify

# View coverage report after ./mvnw verify
# Open: target/site/jacoco/index.html
```

## Database Setup (Docker)
```bash
# Start PostgreSQL container
docker run --name tms-postgres -e POSTGRES_PASSWORD=979712 -p 5432:5432 -d postgres:16

# Create database
docker exec -it tms-postgres psql -U postgres -c "CREATE DATABASE tms;"

# Load schema (PowerShell)
Get-Content "src/main/resources/schema.sql" | docker exec -i tms-postgres psql -U postgres -d tms

# Load seed data
Get-Content "src/main/resources/seed-data.sql" | docker exec -i tms-postgres psql -U postgres -d tms
```

## API Documentation
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI Spec: http://localhost:8080/v3/api-docs

## Git Commands
```bash
git status
git add .
git commit -m "message"
git push origin branch-name
git pull origin branch-name
git checkout -b new-branch
```

## Quick Start (Copy-Paste Ready)
```bash
# Setup environment + run application
export JAVA_HOME="/c/Users/YourUsername/.jdks/openjdk-21.0.1" && export PATH="$JAVA_HOME/bin:$PATH" && ./mvnw spring-boot:run

# Setup environment + run tests
export JAVA_HOME="/c/Users/YourUsername/.jdks/openjdk-21.0.1" && export PATH="$JAVA_HOME/bin:$PATH" && ./mvnw clean verify
```

## Windows Specific
```bash
# List files
dir /s /b
Get-ChildItem -Recurse

# Find text in files (PowerShell)
Select-String -Path "*.java" -Pattern "searchTerm"

# Check port usage
netstat -ano | findstr :8080
```
