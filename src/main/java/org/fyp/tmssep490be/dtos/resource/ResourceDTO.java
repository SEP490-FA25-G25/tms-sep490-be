package org.fyp.tmssep490be.dtos.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDTO {
    private Long id;
    private Long branchId;
    private String branchName;
    private String resourceType;          // "ROOM" hoặc "VIRTUAL"
    private String code;                   // Mã: "HN-ROOM-101"
    private String name;                   // Tên: "Phòng 101"
    private String description;
    private Integer capacity;              // Sức chứa
    private Integer capacityOverride;      // Ghi đè sức chứa (nếu cần)
    private String equipment;              // Thiết bị (cho ROOM)

    // Các trường cho VIRTUAL (Zoom)
    private String meetingUrl;
    private String meetingId;
    private String meetingPasscode;
    private String accountEmail;
    private String licenseType;
    private String startDate;              // Ngày bắt đầu license
    private String expiryDate;             // Ngày hết hạn
    private String renewalDate;            // Ngày gia hạn

    private String createdAt;
    private String updatedAt;
    private String status;                 // "ACTIVE" hoặc "INACTIVE"

    // Thống kê
    private Long activeClassesCount;
    private Long totalSessionsCount;
    private String nextSessionInfo;
    private Boolean hasAnySessions;
    private Boolean hasFutureSessions;
}