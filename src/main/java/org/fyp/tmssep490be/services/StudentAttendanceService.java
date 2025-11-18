package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceOverviewResponseDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceReportResponseDTO;

public interface StudentAttendanceService {
    StudentAttendanceOverviewResponseDTO getOverview(Long studentId);
    StudentAttendanceReportResponseDTO getReport(Long studentId, Long classId);
}




