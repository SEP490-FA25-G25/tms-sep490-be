# Codebase Structure - TMS SEP490 Backend

## Root Directory
```
tms-sep490-be/
├── .claude/             # Claude Code configuration
├── .git/                # Git repository
├── .github/             # GitHub workflows
├── .idea/               # IntelliJ IDEA settings
├── .mvn/                # Maven wrapper
├── .serena/             # Serena MCP configuration
├── docs/                # Documentation
├── src/                 # Source code
├── target/              # Build output
├── pom.xml              # Maven project configuration
├── mvnw, mvnw.cmd       # Maven wrapper scripts
├── CLAUDE.md            # AI assistant instructions
├── AGENTS.md            # Agent guidelines
└── README.md            # Project readme
```

## Source Code Structure
```
src/
├── main/
│   ├── java/org/fyp/tmssep490be/
│   │   ├── TmsSep490BeApplication.java    # Main Spring Boot application
│   │   ├── config/                         # Configuration classes
│   │   │   ├── SecurityConfiguration.java
│   │   │   ├── SecurityBeanConfiguration.java
│   │   │   ├── OpenAPIConfiguration.java
│   │   │   └── JpaAuditingConfiguration.java
│   │   ├── controllers/                    # REST API endpoints (12 controllers)
│   │   │   ├── AuthController.java
│   │   │   ├── StudentController.java
│   │   │   ├── ClassController.java
│   │   │   ├── EnrollmentController.java
│   │   │   ├── CenterController.java
│   │   │   ├── CurriculumController.java
│   │   │   ├── StudentRequestController.java
│   │   │   ├── StudentRequestSubmissionController.java
│   │   │   ├── StudentScheduleController.java
│   │   │   ├── TeacherRequestController.java
│   │   │   ├── UserAccountController.java
│   │   │   └── AcademicAffairsRequestController.java
│   │   ├── services/                       # Business logic interfaces (45)
│   │   │   ├── AuthService.java
│   │   │   ├── StudentService.java
│   │   │   ├── ClassService.java
│   │   │   ├── EnrollmentService.java
│   │   │   └── ... (40+ more)
│   │   ├── services/impl/                  # Service implementations
│   │   │   └── ... corresponding implementations
│   │   ├── repositories/                   # JPA repositories (28)
│   │   │   ├── StudentRepository.java
│   │   │   ├── ClassRepository.java
│   │   │   ├── UserAccountRepository.java
│   │   │   └── ...
│   │   ├── entities/                       # Domain models (39 entities)
│   │   │   ├── Student.java
│   │   │   ├── Teacher.java
│   │   │   ├── ClassEntity.java
│   │   │   ├── Enrollment.java
│   │   │   ├── UserAccount.java
│   │   │   ├── Center.java
│   │   │   ├── Branch.java
│   │   │   └── ... (30+ more)
│   │   ├── entities/enums/                 # Type-safe enums (23)
│   │   │   ├── UserStatus.java
│   │   │   ├── EnrollmentStatus.java
│   │   │   ├── Modality.java
│   │   │   ├── Skill.java
│   │   │   └── ...
│   │   ├── dtos/                           # Data Transfer Objects
│   │   │   ├── common/
│   │   │   │   └── ResponseObject.java     # Standard API response
│   │   │   ├── auth/                       # Auth DTOs
│   │   │   ├── studentmanagement/          # Student DTOs
│   │   │   ├── classes/                    # Class DTOs
│   │   │   ├── enrollment/                 # Enrollment DTOs
│   │   │   ├── curriculum/                 # Curriculum DTOs
│   │   │   ├── studentrequest/             # Student request DTOs
│   │   │   ├── teacherrequest/             # Teacher request DTOs
│   │   │   ├── schedule/                   # Schedule DTOs
│   │   │   └── user/                       # User DTOs
│   │   ├── security/                       # JWT authentication
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── CustomUserDetailsService.java
│   │   │   └── UserPrincipal.java
│   │   ├── exceptions/                     # Exception handling
│   │   │   ├── CustomException.java
│   │   │   ├── BusinessRuleException.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   └── utils/                          # Utility classes
│   └── resources/
│       ├── application.yml                  # Main configuration
│       ├── schema.sql                       # Database schema
│       ├── seed-data.sql                    # Test data
│       ├── enum-init.sql                    # PostgreSQL enum types
│       ├── static/                          # Static resources
│       └── templates/                       # Template files
└── test/
    ├── java/org/fyp/tmssep490be/
    │   ├── config/                          # Test configuration
    │   │   ├── AbstractRepositoryTest.java
    │   │   └── PostgreSQLTestContainer.java
    │   ├── utils/                           # Test utilities
    │   │   └── TestDataBuilder.java
    │   ├── services/                        # Service unit tests
    │   ├── repositories/                    # Repository tests
    │   └── controllers/                     # Controller integration tests
    └── resources/
        └── application-test.yml             # Test-specific config
```

## Key Files by Purpose

### Configuration
- `application.yml` - Database, JWT, Swagger settings
- `SecurityConfiguration.java` - JWT filters, CORS, endpoint security
- `OpenAPIConfiguration.java` - Swagger UI customization

### Authentication
- `JwtTokenProvider.java` - Token generation/validation
- `JwtAuthenticationFilter.java` - Request filtering
- `UserPrincipal.java` - Authenticated user details

### API Layer
- Controllers return `ResponseObject<T>` (success, message, data)
- Base path: `/api/v1/`
- Role-based access via `@PreAuthorize`

### Data Layer
- Repositories extend `JpaRepository`
- Custom queries with `@Query` (JPQL)
- Testcontainers for integration testing

### Business Logic
- Services injected via `@RequiredArgsConstructor`
- `@Transactional` for write operations
- Domain-specific exceptions

## Entity Statistics
- **Total Entities**: 39
- **Controllers**: 12
- **Services**: 45 interfaces
- **Repositories**: 28
- **Enums**: 23
- **DTO Packages**: 12
