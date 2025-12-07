package org.fyp.tmssep490be.dtos.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequestDTO {
    private Long branchId;
    private String resourceType;           // "ROOM" hoặc "VIRTUAL"
    private String code;                   // Mã tài nguyên (VD: "ROOM-101")
    private String name;                   // Tên tài nguyên
    private String description;            // Mô tả (tối thiểu 10 ký tự nếu có)
    private Integer capacity;              // Sức chứa (ROOM: max 40, VIRTUAL: max 100)
    private Integer capacityOverride;      // Ghi đè sức chứa
    private String equipment;              // Thiết bị (chỉ cho ROOM)

    // Các trường cho VIRTUAL (Zoom)
    private String meetingUrl;             // URL phòng Zoom
    private String meetingId;              // Meeting ID
    private String meetingPasscode;        // Passcode
    private String accountEmail;           // Email tài khoản Zoom
    private String accountPassword;        // Password (không trả về trong response)
    private String licenseType;            // Loại license: "Basic", "Pro", etc.
    private String startDate;              // Ngày bắt đầu (YYYY-MM-DD)
    private String expiryDate;             // Ngày hết hạn (YYYY-MM-DD)
    private String renewalDate;            // Ngày gia hạn (YYYY-MM-DD)
}