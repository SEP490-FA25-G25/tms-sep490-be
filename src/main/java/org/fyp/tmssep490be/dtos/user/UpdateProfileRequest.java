package org.fyp.tmssep490be.dtos.user;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.Gender;

import java.time.LocalDate;

/**
 * DTO cho request cập nhật profile của user hiện tại
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateProfileRequest {
    private String phone;
    private String facebookUrl;
    private LocalDate dob;
    private Gender gender;
    private String address;
    private String avatarUrl;
}
