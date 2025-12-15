package org.fyp.tmssep490be.dtos.teacherregistration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassNeedingTeacherDTO {
    private Long classId;
    private String classCode;
    private String className;
    private String subjectName;
    private String branchName;
    private String modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private int[] scheduleDays;
    private Integer maxCapacity;

    // Registration dates (null if not opened yet)
    private OffsetDateTime registrationOpenDate;
    private OffsetDateTime registrationCloseDate;

    // Status flags
    private boolean registrationOpened; // true if registrationOpenDate is set
    private int pendingRegistrations; // count of pending teacher registrations
}
