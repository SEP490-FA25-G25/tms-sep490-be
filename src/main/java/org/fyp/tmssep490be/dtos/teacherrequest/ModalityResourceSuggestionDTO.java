package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO gợi ý resource khả dụng cho yêu cầu đổi phương thức
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModalityResourceSuggestionDTO {
    private Long resourceId;
    private String name;
    private String resourceType;
    private Integer capacity;
    private Long branchId;
    private boolean currentResource;
}

