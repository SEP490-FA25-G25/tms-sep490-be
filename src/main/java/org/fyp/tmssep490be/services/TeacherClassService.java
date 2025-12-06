package org.fyp.tmssep490be.services;

import java.util.List;

import org.fyp.tmssep490be.dtos.teacherclass.TeacherClassListItemDTO;

public interface TeacherClassService {

    //Get all classes assigned to a teacher
    List<TeacherClassListItemDTO> getTeacherClasses(Long teacherId);
}