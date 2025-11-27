package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.center.CenterRequestDTO;
import org.fyp.tmssep490be.dtos.center.CenterResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CenterService {

    CenterResponseDTO createCenter(CenterRequestDTO request);

    CenterResponseDTO getCenterById(Long id);

    Page<CenterResponseDTO> getAllCenters(Pageable pageable);

    CenterResponseDTO updateCenter(Long id, CenterRequestDTO request);

    void deleteCenter(Long id);
}
