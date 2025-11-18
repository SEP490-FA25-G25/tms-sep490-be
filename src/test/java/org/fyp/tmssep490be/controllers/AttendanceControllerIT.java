package org.fyp.tmssep490be.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fyp.tmssep490be.dtos.attendance.AttendanceSaveRequestDTO;
import org.fyp.tmssep490be.dtos.attendance.AttendanceRecordDTO;
import org.fyp.tmssep490be.dtos.attendance.MarkAllResponseDTO;
import org.fyp.tmssep490be.dtos.attendance.SessionTodayDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.AttendanceService;
import org.fyp.tmssep490be.utils.TeacherContextHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttendanceControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AttendanceService attendanceService;

    @MockitoBean
    private TeacherContextHelper teacherContextHelper;

    @Test
    void getTodaySessions_returnsOk() throws Exception {
        when(teacherContextHelper.getTeacherId(any(UserPrincipal.class))).thenReturn(1L);
        when(attendanceService.getSessionsForDate(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(SessionTodayDTO.builder()
                        .sessionId(1L)
                        .classId(100L)
                        .classCode("CLS-100")
                        .courseCode("COURSE-1")
                        .courseName("Course 1")
                        .date(LocalDate.now())
                        .startTime(java.time.LocalTime.of(8, 0))
                        .endTime(java.time.LocalTime.of(10, 0))
                        .status("PLANNED")
                        .attendanceSubmitted(false)
                        .totalStudents(10)
                        .presentCount(0)
                        .absentCount(10)
                        .build()));

        mockMvc.perform(get("/api/v1/attendance/sessions/today")
                        .header("Authorization", "Bearer dummy"))
                .andExpect(status().isOk());
    }

    @Test
    void markAllPresent_preview_returnsOk() throws Exception {
        when(teacherContextHelper.getTeacherId(any(UserPrincipal.class))).thenReturn(1L);
        when(attendanceService.markAllPresent(anyLong(), anyLong()))
                .thenReturn(MarkAllResponseDTO.builder()
                        .sessionId(1L)
                        .summary(org.fyp.tmssep490be.dtos.attendance.AttendanceSummaryDTO.builder()
                                .totalStudents(5).presentCount(5).absentCount(0).build())
                        .build());

        mockMvc.perform(post("/api/v1/attendance/sessions/1/mark-all-present")
                        .header("Authorization", "Bearer dummy"))
                .andExpect(status().isOk());
    }

    @Test
    void saveAttendance_requiresBody_returnsOkWhenProvided() throws Exception {
        when(teacherContextHelper.getTeacherId(any(UserPrincipal.class))).thenReturn(1L);
        when(attendanceService.saveAttendance(anyLong(), anyLong(), any(AttendanceSaveRequestDTO.class)))
                .thenReturn(org.fyp.tmssep490be.dtos.attendance.AttendanceSaveResponseDTO.builder()
                        .sessionId(1L)
                        .summary(org.fyp.tmssep490be.dtos.attendance.AttendanceSummaryDTO.builder()
                                .totalStudents(2).presentCount(2).absentCount(0).build())
                        .build());

        var body = AttendanceSaveRequestDTO.builder()
                .records(List.of(
                        AttendanceRecordDTO.builder().studentId(1L).attendanceStatus(org.fyp.tmssep490be.entities.enums.AttendanceStatus.PRESENT).build(),
                        AttendanceRecordDTO.builder().studentId(2L).attendanceStatus(org.fyp.tmssep490be.entities.enums.AttendanceStatus.PRESENT).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/attendance/sessions/1/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .header("Authorization", "Bearer dummy"))
                .andExpect(status().isOk());
    }
}


