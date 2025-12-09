package org.fyp.tmssep490be.dtos.studentrequest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Makeup request submission payload")
public class MakeupRequestDTO {

    @Schema(description = "Student ID (required for AA on-behalf, omit for student self-service)")
    private Long studentId;

    @NotNull(message = "Current class ID is required")
    @Schema(description = "Current class ID", example = "101", required = true)
    private Long currentClassId;

    @NotNull(message = "Target session ID is required")
    @Schema(description = "Target session ID (the missed session)", example = "1012", required = true)
    private Long targetSessionId;

    @NotNull(message = "Makeup session ID is required")
    @Schema(description = "Makeup session ID (the session to makeup)", example = "1023", required = true)
    private Long makeupSessionId;

    @NotBlank(message = "Request reason is required")
    @Size(min = 10, message = "Reason must be at least 10 characters")
    @Schema(description = "Reason for makeup request", example = "Tôi bị ốm và không thể tham gia buổi học ngày 3/11. Tôi muốn học bù để không bỏ lỡ nội dung quan trọng.", required = true)
    private String requestReason;

    @Schema(description = "Additional note (for AA on-behalf)")
    private String note;
}
