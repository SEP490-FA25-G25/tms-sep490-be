package org.fyp.tmssep490be.dtos.branch;

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
public class BranchDTO {
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
}
