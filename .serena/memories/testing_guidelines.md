# Testing Guidelines - TMS SEP490 Backend (Spring Boot 3.5.7)

## ⚠️ CRITICAL: Modern Testing Standards

### CORRECT Pattern (Spring Boot 3.4+)
```java
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class YourServiceTest {
    @Autowired
    private YourService service;
    
    @MockitoBean  // ✅ CORRECT
    private YourRepository repository;
}
```

### WRONG Pattern (Never Use)
```java
@ExtendWith(MockitoExtension.class)  // ❌ NO Spring context
class YourServiceTest {
    @Mock  // ❌ No Spring DI
    private YourRepository repository;
    
    @InjectMocks  // ❌ Bypasses Spring
    private YourService service;
}
```

## Test Types & Patterns

### 1. Service Unit Tests (*Test.java)
```java
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
class StudentServiceImplTest {

    @Autowired
    private StudentService studentService;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @Test
    @DisplayName("Should create student successfully")
    @Order(1)
    void shouldCreateStudentSuccessfully() {
        // Given (Arrange)
        CreateStudentRequest request = CreateStudentRequest.builder()
                .email("student@test.com")
                .fullName("Test Student")
                .build();
        
        when(userAccountRepository.save(any())).thenReturn(mockUserAccount);
        when(studentRepository.save(any())).thenReturn(mockStudent);

        // When (Act)
        CreateStudentResponse response = studentService.createStudent(request, 1L);

        // Then (Assert)
        assertThat(response).isNotNull();
        assertThat(response.getStudentCode()).isNotBlank();
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    @DisplayName("Should throw exception when student not found")
    void shouldThrowWhenStudentNotFound() {
        // Given
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> studentService.getStudentDetail(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student");
    }
}
```

### 2. Repository Integration Tests (*RepositoryTest.java)
```java
@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@Import(JpaAuditingConfiguration.class)
class StudentRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    @DisplayName("Should find student by user account ID")
    void shouldFindByUserAccountId() {
        // Given
        UserAccount userAccount = TestDataBuilder.buildUserAccount()
                .email("student@test.com")
                .build();
        userAccountRepository.save(userAccount);

        Student student = TestDataBuilder.buildStudent()
                .userAccount(userAccount)
                .studentCode("ST001")
                .build();
        studentRepository.save(student);

        // When
        Optional<Student> found = studentRepository.findByUserAccountId(userAccount.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getStudentCode()).isEqualTo("ST001");
    }
}
```

### 3. Controller Integration Tests (*IT.java)
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentService studentService;

    @Test
    @DisplayName("Should get students list with valid token")
    @WithMockUser(username = "aa@test.com", roles = "ACADEMIC_AFFAIR")
    void shouldGetStudentsList() throws Exception {
        // Given
        Page<StudentListItemDTO> mockPage = new PageImpl<>(List.of(
                StudentListItemDTO.builder().id(1L).fullName("Student 1").build()
        ));
        when(studentService.getStudents(any(), any(), any(), any(), any(), anyLong()))
                .thenReturn(mockPage);

        // When/Then
        mockMvc.perform(get("/api/v1/students")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Student 1"));
    }

    @Test
    @DisplayName("Should reject unauthorized access")
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void shouldRejectUnauthorizedAccess() throws Exception {
        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
```

## Test Utilities

### TestDataBuilder Usage
```java
// Build entities with sensible defaults
Center center = TestDataBuilder.buildCenter()
        .code("CTR001")
        .name("Test Center")
        .build();

Branch branch = TestDataBuilder.buildBranch()
        .center(center)
        .code("BR001")
        .build();

ClassEntity classEntity = TestDataBuilder.buildClassEntity()
        .branch(branch)
        .modality(Modality.ONLINE)
        .build();

Student student = TestDataBuilder.buildStudent()
        .userAccount(userAccount)
        .studentCode("ST001")
        .build();
```

## AssertJ Best Practices
```java
// Use AssertJ fluent assertions
assertThat(result).isNotNull();
assertThat(result.getName()).isEqualTo("Expected");
assertThat(result.getList()).hasSize(3).contains(item1, item2);
assertThat(result.getOptional()).isPresent();

// Exception assertions
assertThatThrownBy(() -> service.doSomething())
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("expected text");
```

## Mockito Patterns
```java
// Basic stubbing
when(repository.findById(1L)).thenReturn(Optional.of(entity));
when(repository.save(any(Entity.class))).thenAnswer(inv -> inv.getArgument(0));

// Verification
verify(repository).save(any(Entity.class));
verify(repository, times(1)).findById(1L);
verify(repository, never()).delete(any());

// Argument capture
ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
verify(repository).save(captor.capture());
Entity savedEntity = captor.getValue();
assertThat(savedEntity.getName()).isEqualTo("Expected");
```

## Running Tests
```bash
# All tests
mvn clean verify

# Single test class
mvn test -Dtest=StudentServiceImplTest

# Single test method
mvn test -Dtest=StudentServiceImplTest#shouldCreateStudentSuccessfully

# With coverage
mvn clean verify jacoco:report
# View: target/site/jacoco/index.html
```

## Coverage Goals
- Overall: 80% minimum
- Service Layer: 90%+ (critical business logic)
- Repository Layer: 70%+
- Controller Layer: 80%+
- Entity Layer: 60%+
