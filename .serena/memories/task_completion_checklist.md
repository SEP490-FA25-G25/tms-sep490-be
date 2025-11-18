# Task Completion Checklist - TMS SEP490 Backend

## Before Considering a Task Complete

### 1. Code Quality
- [ ] Code follows project conventions (see code_style_conventions.md)
- [ ] No code duplication (DRY principle)
- [ ] Proper error handling with domain-specific exceptions
- [ ] Logging added for key operations
- [ ] No unused imports or dead code

### 2. Testing Requirements
- [ ] Unit tests written for service layer (90%+ coverage goal)
- [ ] Use `@SpringBootTest` + `@MockitoBean` (NOT @ExtendWith + @Mock)
- [ ] Tests follow AAA pattern (Arrange-Act-Assert)
- [ ] Tests use descriptive `@DisplayName`
- [ ] Run: `mvn test -Dtest=YourServiceTest`

### 3. API Endpoints (if applicable)
- [ ] Returns `ResponseObject<T>` wrapper
- [ ] Proper HTTP status codes (200, 201, 400, 404, etc.)
- [ ] Input validation with `@Valid`
- [ ] Role-based security with `@PreAuthorize`
- [ ] OpenAPI annotations (@Operation, @Parameter, @Tag)
- [ ] Verify in Swagger UI: http://localhost:8080/swagger-ui.html

### 4. Database Changes (if applicable)
- [ ] Entity follows conventions (@Entity, Lombok annotations)
- [ ] Proper JPA relationships defined
- [ ] Repository with custom queries if needed
- [ ] Schema changes documented/scripted

### 5. Build Verification
```bash
# MUST PASS before task is complete
mvn clean compile           # No compilation errors
mvn test                    # All unit tests pass
mvn verify                  # All integration tests pass (if applicable)
```

### 6. Documentation
- [ ] Method-level JavaDoc for complex logic
- [ ] README updated if adding new features
- [ ] API changes documented in Swagger annotations

### 7. Security Considerations
- [ ] No secrets/passwords hardcoded
- [ ] Proper authorization checks
- [ ] Input sanitization for user data
- [ ] SQL injection prevention (use JPA repositories)

## Common Pitfalls to Avoid
- ❌ Using deprecated `@MockBean` instead of `@MockitoBean`
- ❌ Using `@ExtendWith(MockitoExtension.class)` for Spring tests
- ❌ Forgetting `@Transactional` for write operations
- ❌ Not wrapping response in `ResponseObject<T>`
- ❌ Hardcoding user IDs instead of using `@AuthenticationPrincipal`
- ❌ Missing validation on request DTOs
- ❌ Skipping tests to save time

## Quick Verification Commands
```bash
# Compile check
mvn clean compile

# Run specific test
mvn test -Dtest=YourNewServiceTest

# Full test suite
mvn clean verify

# Check coverage
mvn clean verify jacoco:report
# Open: target/site/jacoco/index.html
```

## When Task is Complete
1. All checklist items verified ✅
2. Code committed with descriptive message
3. Build passes: `mvn clean verify`
4. No TODOs or FIXMEs left in code
5. Ready for code review
