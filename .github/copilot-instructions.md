# Copilot Instructions for TMS-SEP490-BE

## Project Overview

Java 21 Spring Boot 3.5.7 application for a multi-branch Training Management System (TMS). Manages curriculum design, class scheduling, student enrollment, attendance, and assessments for language training centers.

**Key Stack:** Spring Boot 3.5.7, Java 21, PostgreSQL, JWT auth, Testcontainers, JaCoCo

## Architecture Patterns

### Layered Architecture (Strict Separation)

```
Controller → Service → Repository → Entity
```

- **Controllers** (`/api/v1/*`): REST endpoints only, no business logic
- **Services** (`services/impl/`): Business logic, transaction boundaries
- **Repositories**: Spring Data JPA, custom queries with `@Query`
- **Entities**: JPA models with auditing (auto `createdAt`/`updatedAt`)

### Standardized Response Format

**ALWAYS** use `ResponseObject<T>` from `dtos/common` for API responses:

```java
ResponseObject.<YourDTO>builder()
    .success(true)
    .message("Operation successful")
    .data(yourData)
    .build();
```

### PostgreSQL Enums Pattern

- Java enums in `entities/enums/` (e.g., `ClassStatus`, `Modality`)
- Use `@Enumerated(EnumType.STRING)` for VARCHAR storage
- CHECK constraints in `schema.sql` enforce valid values
- PostgreSQL enum types exist in `enum-init.sql` but prefer VARCHAR approach

## Critical Testing Standards (Spring Boot 3.5.7+)

### ⚠️ MANDATORY: Modern Spring Boot Testing (2025)

**Unit Tests (Service Layer)**

```java
// ✅ CORRECT - Spring Boot 3.4+
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ServiceImplTest {
    @Autowired private YourService service;
    @MockitoBean private YourRepository repository;  // NOT @MockBean

    @Test
    void shouldHandleBusinessLogic() {
        // Given - When - Then (AAA pattern)
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        var result = service.operation(1L);
        assertThat(result).isNotNull();
    }
}
```

**❌ NEVER USE:**

```java
@ExtendWith(MockitoExtension.class)  // ❌ No Spring context
@Mock / @InjectMocks                  // ❌ Bypasses Spring DI
```

**Integration Tests (Repository Layer)**

```java
@DataJpaTest
@Testcontainers
class RepositoryTest extends AbstractRepositoryTest {
    @Autowired private Repository repository;

    @Test
    void shouldPersistEntity() {
        Entity entity = TestDataBuilder.buildEntity().name("Test").build();
        Entity saved = repository.save(entity);
        assertThat(saved.getId()).isNotNull();
    }
}
```

**API Tests (E2E)**

```java
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class ControllerIT {
    @Autowired private MockMvc mockMvc;

    @Test
    void shouldHandleRequest() throws Exception {
        mockMvc.perform(post("/api/v1/resource")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
```

### Test Commands

```bash
mvn clean verify                           # All tests + coverage
mvn test                                   # Unit tests only
mvn verify -DskipUnitTests                 # Integration tests only
mvn test -Dtest=ClassServiceImplTest       # Specific test
mvn clean verify jacoco:report             # Coverage → target/site/jacoco/
```

### Test Data Builder Pattern

Use `TestDataBuilder` for all test data creation:

```java
Center center = TestDataBuilder.buildCenter()
    .code("TC001")
    .name("Test Center")
    .build();
```

## Security Configuration

### JWT Authentication

- Access tokens: 15 min expiration
- Refresh tokens: 7 days expiration
- Secret configured via `JWT_SECRET` env variable
- Stateless sessions (no server-side state)

### Authorization Patterns

```java
// Method-level security (when enabled)
@PreAuthorize("hasRole('ADMIN')")
public void adminOnlyMethod() { }

// Test with roles
@WithMockUser(username = "teacher", roles = {"TEACHER"})
```

**⚠️ Current State:** Authentication disabled for testing (see `SecurityConfiguration`)

- TODO: Re-enable by changing `.anyRequest().permitAll()` to `.anyRequest().authenticated()`

## Development Workflows

### Adding New Feature (Full Stack)

1. **Entity**: Create in `entities/`, extend `BaseEntity` (if applicable)
2. **Repository**: Interface extending `JpaRepository`
3. **DTOs**: Create request/response DTOs in appropriate `dtos/` subfolder
4. **Service**: Interface in `services/`, implementation in `services/impl/`
5. **Controller**: REST endpoints in `controllers/` with `@RequestMapping("/api/v1/*")`
6. **Tests**: Write unit tests (service), integration tests (repository), E2E tests (controller)

### Database Changes

- Schema changes in `schema.sql`
- Enums in `enum-init.sql`
- Seed data in `seed-data.sql`
- Local PostgreSQL: `docker run --name tms-postgres -e POSTGRES_PASSWORD=979712 -p 5432:5432 -d postgres`

### Running the App

```bash
# Build and run
mvn spring-boot:run

# Build JAR
mvn clean package

# Run with JAR
java -jar target/tms-sep490-be-0.0.1-SNAPSHOT.jar

# Skip tests during build
mvn clean package -DskipTests
```

## Code Quality Standards

### Lombok Usage

- Use `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` on DTOs
- Use `@RequiredArgsConstructor` with `final` fields for constructor injection
- Entities: Use specific annotations (`@Getter`, `@Setter`, `@ToString`)

### Exception Handling

- `GlobalExceptionHandler` in `exceptions/` handles all errors
- Use `CustomException` with `ErrorCode` for business logic errors
- `EntityNotFoundException` for 404 scenarios
- Validation errors return field-level error map

### Validation

- Use `@Valid` on controller methods
- Bean Validation annotations (`@NotNull`, `@Size`, etc.) on DTOs
- Custom validators in `validators/` package for complex logic

### Transaction Management

- Service layer methods are transactional by default
- Use `@Transactional(readOnly = true)` for read operations
- Avoid long-running transactions

## Documentation & API

### Swagger/OpenAPI

- Accessible at: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`
- Use `@Operation`, `@Parameter`, `@ApiResponse` for documentation

### Code Documentation

- Javadoc on public interfaces and complex methods
- README files in subdirectories for complex features (see `docs/`)

## Business Domain

### Key Entities

- **Center/Branch**: Multi-branch organizational structure
- **Course/Subject**: Curriculum design with PLOs/CLOs
- **Class/Session**: Class scheduling with auto-generated sessions
- **Student/Teacher**: User accounts with role-based access
- **Enrollment/Attendance**: Student class participation tracking
- **Request**: Workflow for absences, makeups, transfers, reschedules

### User Roles

- `ADMIN`: System-wide admin
- `MANAGER`: Multi-branch oversight, course approval
- `CENTER_HEAD`: Branch-level admin, class approval
- `SUBJECT_LEADER`: Curriculum design
- `ACADEMIC_AFFAIR`: Class creation, enrollment, resource assignment
- `TEACHER`: Teaching, attendance, grading
- `STUDENT`: View schedule, submit requests
- `QA`: Quality monitoring

## Common Pitfalls

1. **Don't** use `@MockBean` - Use `@MockitoBean` (Spring Boot 3.4+)
2. **Don't** bypass `ResponseObject<T>` - All APIs must return this format
3. **Don't** add business logic to controllers - Keep in service layer
4. **Don't** use PostgreSQL enum types directly - Use VARCHAR with CHECK constraints
5. **Don't** forget `@ActiveProfiles("test")` on test classes
6. **Don't** skip test coverage - Aim for 80%+ overall

## Related Documentation

- Comprehensive guide: `AGENTS.md`, `CLAUDE.md`
- Testing details: `src/test/README.md`
- Business flows: `docs/business-flow-usecase.md`
- Feature workflows: `docs/create-class/`, `docs/enrollment/`, `docs/teacher/`
