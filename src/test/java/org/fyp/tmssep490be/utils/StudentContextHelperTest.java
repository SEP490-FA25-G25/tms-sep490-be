package org.fyp.tmssep490be.utils;

import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StudentContextHelper utility class.
 * Uses pure unit testing with Mockito to avoid Spring context loading issues.
 * Tests helper methods for extracting student information from authenticated UserPrincipal.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentContextHelper Unit Tests")
class StudentContextHelperTest {

    @Mock
    private StudentRepository studentRepository;

    private StudentContextHelper studentContextHelper;

    private Student testStudent;
    private UserAccount testUserAccount;
    private UserPrincipal testUserPrincipal;

    @BeforeEach
    void setUp() {
        // Initialize StudentContextHelper with mock repository
        studentContextHelper = new StudentContextHelper(studentRepository);

        // Create test data
        testUserAccount = UserAccount.builder()
                .id(1L)
                .email("student@example.com")
                .fullName("Test Student")
                .build();

        testStudent = Student.builder()
                .id(100L)
                .studentCode("ST100")
                .userAccount(testUserAccount)
                .build();

        // Create a simple UserPrincipal for testing
        testUserPrincipal = new UserPrincipal(1L, "student@example.com", "encodedPassword", "Test Student", org.fyp.tmssep490be.entities.enums.UserStatus.ACTIVE, java.util.Collections.emptyList());
    }

    @Test
    @DisplayName("Should get student from principal successfully")
    void shouldGetStudentFromPrincipalSuccessfully() {
        // Arrange
        when(studentRepository.findByUserAccountId(1L)).thenReturn(Optional.of(testStudent));

        // Act
        Student result = studentContextHelper.getStudentFromPrincipal(testUserPrincipal);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStudentCode()).isEqualTo("ST100");
        assertThat(result.getUserAccount().getEmail()).isEqualTo("student@example.com");
        assertThat(result.getUserAccount().getFullName()).isEqualTo("Test Student");

        verify(studentRepository).findByUserAccountId(1L);
    }

    @Test
    @DisplayName("Should throw exception when UserPrincipal is null")
    void shouldThrowExceptionWhenUserPrincipalIsNull() {
        // Arrange
        UserPrincipal nullPrincipal = null;

        // Act & Assert
        assertThatThrownBy(() -> studentContextHelper.getStudentFromPrincipal(nullPrincipal))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED.getMessage());

        verify(studentRepository, never()).findByUserAccountId(any());
    }

    @Test
    @DisplayName("Should throw exception when student not found for user")
    void shouldThrowExceptionWhenStudentNotFoundForUser() {
        // Arrange
        when(studentRepository.findByUserAccountId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> studentContextHelper.getStudentFromPrincipal(testUserPrincipal))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.STUDENT_NOT_FOUND.getMessage());

        verify(studentRepository).findByUserAccountId(1L);
    }

    @Test
    @DisplayName("Should get student ID from principal successfully")
    void shouldGetStudentIdFromPrincipalSuccessfully() {
        // Arrange
        when(studentRepository.findByUserAccountId(1L)).thenReturn(Optional.of(testStudent));

        // Act
        Long result = studentContextHelper.getStudentId(testUserPrincipal);

        // Assert
        assertThat(result).isEqualTo(100L);

        verify(studentRepository).findByUserAccountId(1L);
    }

    @Test
    @DisplayName("Should get verified student when IDs match")
    void shouldGetVerifiedStudentWhenIdsMatch() {
        // Arrange
        when(studentRepository.findByUserAccountId(1L)).thenReturn(Optional.of(testStudent));

        // Act
        Student result = studentContextHelper.getVerifiedStudent(testUserPrincipal, 100L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);

        verify(studentRepository).findByUserAccountId(1L);
    }

    @Test
    @DisplayName("Should throw exception when student ID doesn't match authenticated user")
    void shouldThrowExceptionWhenStudentIdDoesNotMatch() {
        // Arrange
        Long wrongStudentId = 999L;
        when(studentRepository.findByUserAccountId(1L)).thenReturn(Optional.of(testStudent));

        // Act & Assert
        assertThatThrownBy(() -> studentContextHelper.getVerifiedStudent(testUserPrincipal, wrongStudentId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.STUDENT_ACCESS_DENIED.getMessage());

        verify(studentRepository).findByUserAccountId(1L);
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void shouldHandleRepositoryExceptionsGracefully() {
        // Arrange
        when(studentRepository.findByUserAccountId(1L))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> studentContextHelper.getStudentFromPrincipal(testUserPrincipal))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection failed");

        verify(studentRepository).findByUserAccountId(1L);
    }
}