package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedAARequestResponseDTO {

    private List<AARequestResponseDTO> content;
    private Pageable pageable;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private RequestSummaryDTO summary;
}
