package org.fyp.tmssep490be.utils;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeacherContextHelper {

    private final TeacherRepository teacherRepository;

    public Long getTeacherId(UserPrincipal principal) {
        return teacherRepository.findByUserAccountId(principal.getId())
                .map(teacher -> teacher.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for current user"));
    }
}

