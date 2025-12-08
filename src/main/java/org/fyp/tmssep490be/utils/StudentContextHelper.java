package org.fyp.tmssep490be.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentContextHelper {

    private final StudentRepository studentRepository;

    public Student getStudentFromPrincipal(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            log.error("UserPrincipal is null");
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = userPrincipal.getId();
        log.debug("Extracting student for user ID: {}", userId);

        return studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> {
                    log.warn("Student profile not found for user ID: {}", userId);
                    return new CustomException(ErrorCode.STUDENT_NOT_FOUND);
                });
    }

    public Long getStudentId(UserPrincipal userPrincipal) {
        return getStudentFromPrincipal(userPrincipal).getId();
    }
}
