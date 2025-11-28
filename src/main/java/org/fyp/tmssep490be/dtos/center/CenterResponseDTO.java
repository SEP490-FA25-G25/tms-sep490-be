package org.fyp.tmssep490be.dtos.center;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterResponseDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String phone;
    private String email;
    private String address;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

