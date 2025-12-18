package org.fyp.tmssep490be.dtos.publicapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for public schedule information (upcoming classes)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicScheduleDTO {

    private Long id;

    private String code; // Mã lớp

    private String courseName; // Tên môn học

    private String timeSlot; // Khung giờ (e.g., "19h-21h")

    private LocalDate startDate; // Ngày khai giảng

    private String status; // Còn chỗ / Gần hết chỗ / Hết chỗ

    private String statusColor; // bg-emerald-500 / bg-yellow-500 / bg-red-500

    private Integer maxCapacity; // Sĩ số tối đa

    private Integer enrolledCount; // Số đã đăng ký

    private String branchCode; // Mã cơ sở

    private String branchName; // Tên cơ sở

    private String modality; // OFFLINE / ONLINE / KAIWA
}
