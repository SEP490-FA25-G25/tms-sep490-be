package org.fyp.tmssep490be.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.springframework.stereotype.Component;

/**
 * Utility class to extract student information from authenticated UserPrincipal
 * This helper ensures that students can only access their own data
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StudentContextHelper {

    private final StudentRepository studentRepository;

    /**
     * Get Student entity from authenticated UserPrincipal
     * @param userPrincipal The authenticated user from JWT
     * @return Student entity
     * @throws CustomException if user is not a student or student profile not found
     */
    public Student getStudentFromPrincipal(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            log.error("UserPrincipal is null");
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = userPrincipal.getId(); // UserAccount.id from JWT
        log.debug("Extracting student for user ID: {}", userId);

        return studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> {
                    log.warn("Student profile not found for user ID: {}", userId);
                    return new CustomException(ErrorCode.STUDENT_NOT_FOUND);
                });
    }

    /**
     * Get Student ID from authenticated UserPrincipal
     * @param userPrincipal The authenticated user from JWT
     * @return Student.id
     * @throws CustomException if user is not a student or student profile not found
     */
    public Long getStudentId(UserPrincipal userPrincipal) {
        return getStudentFromPrincipal(userPrincipal).getId();
    }

    /**
     * Get Student entity with verification that the studentId matches the authenticated user
     * This method provides additional security when studentId is passed as a parameter
     * @param userPrincipal The authenticated user from JWT
     * @param studentId The student ID to verify
     * @return Student entity
     * @throws CustomException if studentId doesn't match authenticated user's student ID
     */
    public Student getVerifiedStudent(UserPrincipal userPrincipal, Long studentId) {
        Student authenticatedStudent = getStudentFromPrincipal(userPrincipal);

        if (!authenticatedStudent.getId().equals(studentId)) {
            log.warn("Student ID mismatch. Authenticated: {}, Requested: {}",
                    authenticatedStudent.getId(), studentId);
            throw new CustomException(ErrorCode.STUDENT_ACCESS_DENIED);
        }

        return authenticatedStudent;
    }
}