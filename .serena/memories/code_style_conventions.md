# Code Style & Conventions - TMS SEP490 Backend

## Package Structure
```
org.fyp.tmssep490be/
├── config/          # Spring configurations (Security, OpenAPI, JPA)
├── controllers/     # REST API endpoints (@RestController)
├── services/        # Business logic interfaces
├── services/impl/   # Service implementations
├── repositories/    # JPA repositories
├── entities/        # Domain models (@Entity)
├── entities/enums/  # Type-safe enums (PostgreSQL enums)
├── dtos/           # Data Transfer Objects (organized by domain)
│   ├── common/     # ResponseObject<T>
│   ├── auth/       # Authentication DTOs
│   ├── classes/    # Class-related DTOs
│   └── ...
├── security/       # JWT components
├── exceptions/     # Global exception handling
└── utils/          # Utilities (TestDataBuilder, etc.)
```

## Entity Conventions
```java
@Entity
@Table(name = "snake_case_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityName {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Fields use camelCase, @Column(name = "snake_case")
    @Column(name = "field_name", nullable = false)
    private String fieldName;

    // Relationships
    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ChildEntity> children = new HashSet<>();

    // Auditing (managed by database triggers)
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;
}
```

## Controller Conventions
```java
@RestController
@RequestMapping("/api/v1/resource-name")  // plural, kebab-case
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Resource Name", description = "Description for Swagger")
@SecurityRequirement(name = "bearerAuth")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @Operation(summary = "Get resources", description = "Detailed description")
    @PreAuthorize("hasRole('ROLE_NAME')")
    public ResponseEntity<ResponseObject<List<ResourceDTO>>> getResources(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting resources", currentUser.getId());
        
        List<ResourceDTO> data = resourceService.getResources();
        
        return ResponseEntity.ok(ResponseObject.<List<ResourceDTO>>builder()
                .success(true)
                .message("Resources retrieved successfully")
                .data(data)
                .build());
    }
}
```

## Service Layer Pattern
```java
// Interface
public interface ResourceService {
    ResourceDTO getResource(Long id);
    ResourceDTO createResource(CreateResourceRequest request, Long userId);
}

// Implementation
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService {
    
    private final ResourceRepository repository;
    
    @Override
    public ResourceDTO getResource(Long id) {
        return repository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));
    }
    
    @Override
    @Transactional  // Override readOnly for write operations
    public ResourceDTO createResource(CreateResourceRequest request, Long userId) {
        // Business logic
    }
}
```

## API Response Format
All endpoints return `ResponseObject<T>`:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseObject<T> {
    private boolean success;
    private String message;
    private T data;
}
```

## Naming Conventions
- **Classes**: PascalCase (StudentController, EnrollmentService)
- **Methods/Variables**: camelCase (getStudentDetail, studentCode)
- **Constants**: UPPER_SNAKE_CASE
- **Database Tables/Columns**: snake_case
- **REST Endpoints**: /api/v1/resource-name (kebab-case, plural)
- **DTOs**: Purpose-specific (CreateStudentRequest, StudentListItemDTO, StudentDetailDTO)

## Lombok Usage
- `@Data` - For DTOs (getters, setters, equals, hashCode, toString)
- `@Getter @Setter` - For entities (separate to avoid toString issues)
- `@Builder` - For constructing objects
- `@NoArgsConstructor @AllArgsConstructor` - Required for JPA and Builder
- `@RequiredArgsConstructor` - For constructor injection in services/controllers
- `@Slf4j` - For logging
- `@Builder.Default` - For initializing collections in entities

## Exception Handling
```java
// Custom exceptions extend base
throw new BusinessRuleException("RULE_CODE", "Error message");
throw new ResourceNotFoundException("Entity", "field", value);

// Global handler in exceptions/ package catches and formats responses
```

## Security Annotations
- `@PreAuthorize("hasRole('ROLE_NAME')")` - Method-level security
- `@AuthenticationPrincipal UserPrincipal currentUser` - Get current user
- `@SecurityRequirement(name = "bearerAuth")` - Swagger security spec

## Validation
- `@Valid @RequestBody` on request DTOs
- Use Jakarta Validation annotations (@NotNull, @NotBlank, @Size, @Email, etc.)
- Validation errors automatically formatted via global exception handler

## Logging Pattern
```java
log.info("User {} performing action on resource {}", userId, resourceId);
log.debug("Details: {}", details);
log.error("Error occurred: {}", exception.getMessage(), exception);
```
