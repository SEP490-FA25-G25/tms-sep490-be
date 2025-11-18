# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Java Spring Boot 3.5.7** application using **Java 21** for a Training Management System (TMS). Clean layered architecture with JWT-based authentication and PostgreSQL database.

**Key Technologies:**
- Spring Boot 3.5.7 with Java 21
- PostgreSQL 16 with Hibernate JPA
- JWT authentication with role-based authorization
- SpringDoc OpenAPI (Swagger UI)
- Testcontainers for integration testing
- Maven wrapper for build management
- Docker for containerized deployment

## Implementation Plan: Core Principles

**1. Code Quality & Structure:**
- **Clean Implementation:** Avoid unnecessary code, complexity, and "code smells." Adhere to SOLID, DRY principles.
- **No Redundancy:** Actively prevent code duplication. Abstract and reuse components, functions, and logic.
- **Logical Soundness:** Ensure all logic is correct and algorithms are efficient.

**2. System Integrity & Performance:**
- **Prevent Race Conditions:** Ensure data integrity in concurrent operations.
- **Avoid Over-engineering:** Implement what is necessary without speculative features.

**3. Development Approach:**
- **Maintain Holistic View:** Consider overall architecture and impact on the entire system.
- **Focus on MVP Scope:** Deliver the user story at hand within defined scope. Primary goal is functional, demonstrable features.

## Acknowledging Correct Feedback

When feedback IS correct:
- ✅ "Fixed. [Brief description of what changed]"
- ✅ "Good catch – [specific issue]. Fixed in [location]."
- ✅ Just fix it and show in the code

When feedback is correct, DO NOT use:
- ❌ "You're absolutely right!", "Great point!", "Thanks for catching that!"
- ❌ ANY gratitude expression

**Why:** Actions speak. The code itself shows you heard the feedback.

## Gracefully Correcting Your Pushback

If you pushed back and were wrong:
- ✅ "You were right – I checked [X] and it does [Y]. Implementing now."
- ✅ "Verified this and you're correct. My initial understanding was wrong because [reason]. Fixing."

Avoid long apologies, defending why you pushed back, or over-explaining. State the correction factually and move on.

## Development Commands

### Environment Setup (REQUIRED FIRST)

**PowerShell (Windows - recommended):**
```powershell
$env:JAVA_HOME = "C:\Users\tmtmt\.jdks\openjdk-21.0.1"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Verify
java -version
./mvnw.cmd -version
```

**Git Bash / WSL:**
```bash
export JAVA_HOME="/c/Users/tmtmt/.jdks/openjdk-21.0.1"
export PATH="$JAVA_HOME/bin:$PATH"

# Verify
java -version
./mvnw -version
```

### Build and Run

```bash
# Build
./mvnw clean compile

# Run application (port 8080)
./mvnw spring-boot:run

# Build JAR
./mvnw clean package -DskipTests

# Run JAR
java -jar target/tms-sep490-be-0.0.1-SNAPSHOT.jar
```

### Testing

```bash
# All tests with coverage
./mvnw clean verify

# Specific test class
./mvnw test -Dtest=CenterServiceImplTest

# Specific test method
./mvnw test -Dtest=CenterServiceImplTest#shouldFindCenterById

# Coverage report (after verify)
# Open: target/site/jacoco/index.html
```

### Database Setup (Local Development)

```bash
# Start PostgreSQL container
docker run --name tms-postgres -e POSTGRES_PASSWORD=979712 -p 5432:5432 -d postgres:16

# Create database
docker exec -it tms-postgres psql -U postgres -c "CREATE DATABASE tms;"

# Load schema and seed data (PowerShell)
Get-Content "src/main/resources/schema.sql", "src/main/resources/seed-data.sql" | docker exec -i tms-postgres psql -U postgres -d tms

# Load schema and seed data (Git Bash)
cat src/main/resources/schema.sql src/main/resources/seed-data.sql | docker exec -i tms-postgres psql -U postgres -d tms
```

### Docker Deployment (Full Stack)

```bash
# Start backend + database
docker-compose up -d

# View logs
docker-compose logs -f backend

# Stop all
docker-compose down

# Rebuild after code changes
docker-compose up -d --build
```

The `docker-compose.yml` automatically:
- Starts PostgreSQL 16 with health checks
- Loads schema.sql and seed-data.sql
- Builds and runs the Spring Boot app
- Configures networking between services

## Architecture Overview

### Package Structure (14 Controllers, 39 Entities)

```
org.fyp.tmssep490be/
├── config/              # Security, OpenAPI, JPA configurations
├── controllers/         # REST API endpoints (/api/v1/*)
├── services/           # Business logic interfaces
├── services/impl/      # Service implementations
├── repositories/       # Data access layer (JPA)
├── entities/           # JPA domain models (39 entities)
├── entities/enums/     # Type-safe enums (23 enums)
├── dtos/               # Data transfer objects by domain
├── security/           # JWT authentication (JwtTokenProvider, filters)
├── exceptions/         # Global exception handling
└── utils/              # Test utilities and builders
```

### Key Controllers

- **AuthController** - Login, token refresh, logout
- **StudentController** - Student CRUD operations
- **ClassController** - Class management
- **EnrollmentController** - Student enrollments
- **StudentRequestController** - Transfer/makeup requests
- **AcademicAffairsRequestController** - Request approvals
- **AttendanceController** - Session attendance tracking
- **CurriculumController** - Course and curriculum management
- **TeacherRequestController** - Teacher-related requests

### Layered Architecture

```
Controllers (HTTP) → Services (Business Logic) → Repositories (Data Access) → Entities (Domain)
```

- **Controllers**: Request validation, response formatting, authorization
- **Services**: Transaction management, business rules, orchestration
- **Repositories**: JPA queries, custom JPQL with `@Query`
- **Entities**: JPA models with automatic auditing (`createdAt`, `updatedAt`)

## Database Configuration

**Current Settings (application.yml):**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none  # Uses existing schema (manual setup required)
  sql:
    init:
      mode: never  # Schema loaded manually, not auto-initialized
```

**Important:** Schema must be loaded manually using the database setup commands above. The application does NOT auto-create tables.

**Connection:**
- URL: `jdbc:postgresql://localhost:5432/tms`
- Credentials: postgres/979712 (development only)

## API Standards

### Response Format

All endpoints return `ResponseObject<T>`:
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... }
}
```

### REST Conventions

- Base path: `/api/v1/`
- Pagination: Spring Data `Pageable`
- Validation: `@Valid` on request bodies
- Security: `@PreAuthorize` for role-based access
- Documentation: Swagger UI at `http://localhost:8080/swagger-ui.html`

## Security & Authentication

### JWT Configuration

- **Access tokens**: 15 minutes (900000ms)
- **Refresh tokens**: 7 days (604800000ms)
- **Secret**: Configured via `JWT_SECRET` environment variable

### Authentication Flow

1. POST `/api/v1/auth/login` → Returns JWT tokens
2. Include `Authorization: Bearer <access_token>` in requests
3. Use refresh token endpoint when access token expires

### Roles (8 total)

ADMIN, MANAGER, CENTER_HEAD, SUBJECT_LEADER, ACADEMIC_AFFAIR, QA, TEACHER, STUDENT

## Testing Strategy (Spring Boot 3.5.7)

### CRITICAL: Modern Testing Standards

**USE:**
```java
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ServiceTest {
    @Autowired private YourService service;
    @MockitoBean private YourRepository repository;
}
```

**DO NOT USE:**
```java
// ❌ DEPRECATED - Bypasses Spring DI
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock private YourRepository repository;
    @InjectMocks private YourService service;
}
```

See `src/test/README.md` for complete testing guidelines.

## Feature Development Workflow

1. Create/update entity in `entities/`
2. Add repository in `repositories/`
3. Define service interface in `services/`
4. Implement in `services/impl/`
5. Create DTOs in `dtos/[domain]/`
6. Add controller in `controllers/`
7. Write tests (unit + integration)

## Serena MCP Server Integration

This project uses **Serena MCP Server** for semantic code navigation and editing.

### Available Memory Files

- `project_overview` - Tech stack, domain model, architecture
- `codebase_structure` - Package layout and statistics
- `code_style_conventions` - Naming patterns, Lombok usage, API responses
- `testing_guidelines` - Modern Spring Boot 3.4+ testing patterns
- `task_completion_checklist` - Quality checks before completing tasks
- `suggested_commands` - Build, test, database commands

### Key Tools

**Code Navigation (Prefer over reading entire files)**
- `get_symbols_overview(file)` - Get top-level symbols in a file
- `find_symbol(name_path, include_body=true)` - Find specific symbol with source
- `find_referencing_symbols(name, file)` - Find all references to a symbol
- `search_for_pattern(pattern)` - Regex search across codebase

**Code Editing**
- `replace_symbol_body(name, file, new_body)` - Replace entire method/class
- `insert_after_symbol(name, file, content)` - Add code after a symbol
- `insert_before_symbol(name, file, content)` - Add imports or code before symbol
- `rename_symbol(old_name, file, new_name)` - Rename throughout codebase

**Thinking Tools (Call before important actions)**
- `think_about_collected_information()` - After gathering context
- `think_about_task_adherence()` - Before making changes
- `think_about_whether_you_are_done()` - When task seems complete

### Best Practices

1. Use `get_symbols_overview` before reading entire files
2. Prefer symbol-based editing over line-based edits
3. Read relevant memory files before complex tasks
4. Always pass `relative_path` to narrow searches
