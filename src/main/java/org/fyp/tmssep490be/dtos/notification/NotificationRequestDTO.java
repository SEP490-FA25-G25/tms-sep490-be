package org.fyp.tmssep490be.dtos.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.NotificationPriority;
import org.fyp.tmssep490be.entities.enums.NotificationType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDTO {

    @NotNull(message = "Người nhận không được để trống")
    private Long recipientId;

    @NotNull(message = "Loại thông báo không được để trống")
    private NotificationType type;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String message;

    @Builder.Default
    private NotificationPriority priority = NotificationPriority.MEDIUM;

    private String actionUrl;

    private String referenceType;

    private Long referenceId;

    private String metadata;

    @Future(message = "Thời gian hết hạn phải trong tương lai")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;
}