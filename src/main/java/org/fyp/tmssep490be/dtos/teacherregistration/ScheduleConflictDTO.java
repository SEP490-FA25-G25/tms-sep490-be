package org.fyp.tmssep490be.dtos.teacherregistration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for schedule conflict check when teacher registers for a class
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleConflictDTO {
    private boolean hasConflict;
    private List<ConflictDetail> conflicts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictDetail {
        private Long conflictingClassId;
        private String conflictingClassName;
        private String conflictingClassCode;
        private String conflictDate; // "15/01/2026"
        private String conflictDayOfWeek; // "Thứ 2"
        private String conflictTimeSlot; // "14:00 - 16:30"
        private String conflictType; // "Đang dạy" or "Đã đăng ký"
    }
}
