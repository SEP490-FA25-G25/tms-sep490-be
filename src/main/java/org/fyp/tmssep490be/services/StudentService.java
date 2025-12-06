package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentmanagement.CreateStudentRequest;
import org.fyp.tmssep490be.dtos.studentmanagement.CreateStudentResponse;

public interface StudentService {

    CreateStudentResponse createStudent(CreateStudentRequest request, Long currentUserId);

}
