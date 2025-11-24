package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.teacher.TeacherProfileDTO;
import org.fyp.tmssep490be.entities.Teacher;

public interface TeacherService {
    TeacherProfileDTO getMyProfile(Long userAccountId);
}
