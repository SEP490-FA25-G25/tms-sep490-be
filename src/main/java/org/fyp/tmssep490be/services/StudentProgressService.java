package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.course.CourseProgressDTO;

public interface StudentProgressService {
    CourseProgressDTO calculateProgress(Long studentId, Long courseId);
}