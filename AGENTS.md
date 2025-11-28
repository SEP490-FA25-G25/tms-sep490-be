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

## Implementation Philosophy: Student Capstone Project

### Project Context
This is a **student capstone project** (đồ án tốt nghiệp), NOT a high-enterprise production system. The focus is on delivering functional, demonstrable features that serve real user needs within academic scope.

### Core Principles

**1. User-Role-Centric Thinking**
- Always think from the **role's perspective**: "If I am a STUDENT, what do I actually need?", "If I am a QA, what tasks do I need to complete?"
- Example: A student needs to see their classes, schedule, and grades - NOT complex analytics or AI-powered recommendations.
- Example: Academic Affairs needs to approve/reject student requests - NOT a multi-level approval workflow with delegation features.

**2. Scope-Appropriate Complexity**
- ✅ **Build what users actually use**: Core CRUD operations, simple workflows, clear data presentation
- ❌ **Avoid speculative features**: "What if we need...", "In the future we might...", "Enterprise systems have..."
- ❌ **Don't over-engineer**: Complex caching strategies, microservices, event-driven architecture, advanced design patterns when simple solutions work

**3. Practical Implementation Guidelines**

**Clean & Simple Code:**
- Follow SOLID and DRY principles, but don't force design patterns where they add complexity
- A straightforward service method is better than a complex factory-strategy-observer chain
- Copy-paste 3 lines of code is sometimes better than premature abstraction

**Real Business Logic:**
- Understand the actual workflow: How does a student request a makeup class? What does Academic Affairs need to decide?
- Don't add fields "just in case" - only what's needed NOW for the feature to work
- Example: Don't build "priority levels" for requests if all requests are treated equally

**Avoid Unnecessary Complexity:**
- ❌ Don't implement multi-tenant architecture if there's only one center
- ❌ Don't add message queues if synchronous operations work fine
- ❌ Don't create 5-layer abstractions if 3 layers (Controller → Service → Repository) suffice
- ❌ Don't add caching if query performance is already acceptable

**Focus on MVP Delivery:**
- Each feature should be **demonstrable and functional**
- Student can log in → see classes → view schedule → submit request (DONE)
- Don't add: notification preferences, request templates, bulk operations, export to 5 formats

**4. Code Quality ≠ Complexity**
- **Clean code** means readable, maintainable, correct - NOT necessarily "enterprise-grade"
- **Best practices** for capstone projects: Clear naming, proper error handling, basic validation, working tests
- **NOT required**: Perfect abstraction, hexagonal architecture, CQRS, domain-driven design, event sourcing

**5. When in Doubt, Ask:**
- "Do users (Student/Teacher/QA/Academic Affairs) actually need this?"
- "Is this solving a real problem or adding 'nice-to-have' complexity?"
- "Can this be simpler and still meet the requirement?"
```

### Remember
- **This is a capstone project to demonstrate learning**, not a production system for 10,000 users
- **Focus on core features working correctly**, not on scalability for future growth
- **Deliver working software that can be demo'd**, not perfect architecture for unknown requirements
- **Think like a user of the system**, not a software architect designing for Google-scale

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