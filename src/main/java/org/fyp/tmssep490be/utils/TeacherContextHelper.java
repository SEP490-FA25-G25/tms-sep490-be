package org.fyp.tmssep490be.utils;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeacherContextHelper {

    private final TeacherRepository teacherRepository;

    //Lấy ID giáo viên từ JWT token
    public Long getTeacherId(UserPrincipal principal) {
        return teacherRepository.findByUserAccountId(principal.getId())
                .map(teacher -> teacher.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND, "Teacher profile not found for current user"));
    }
}

