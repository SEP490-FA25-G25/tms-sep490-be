package org.fyp.tmssep490be.dtos.branch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

// DTO cho Manager xem danh sách chi nhánh với thông tin tổng quan
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerBranchOverviewDTO {
    private Long id;
    private Long centerId;
    private String centerName;
    private String code;
    private String name;
    private String address;
    private String city;
    private String district;
    private String phone;
    private String email;
    private String status;
    private LocalDate openingDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Center Head info
    private CenterHeadInfo centerHead;

    // Status counts
    private StatusCount classStatus;
    private StatusCount teacherStatus;
    private StatusCount resourceStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CenterHeadInfo {
        private Long userId;
        private String fullName;
        private String email;
        private String phone;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCount {
        private int total;
        private int active;
        private int inactive;
    }
}
