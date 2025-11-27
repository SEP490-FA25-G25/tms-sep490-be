package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.center.CenterRequestDTO;
import org.fyp.tmssep490be.dtos.center.CenterResponseDTO;
import org.fyp.tmssep490be.entities.Center;
import org.fyp.tmssep490be.repositories.CenterRepository;
import org.fyp.tmssep490be.services.CenterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CenterServiceImpl implements CenterService {

    private final CenterRepository centerRepository;

    @Override
    @Transactional
    public CenterResponseDTO createCenter(CenterRequestDTO request) {
        log.info("Creating center with code: {}", request.getCode());

        Center center = Center.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .build();

        Center savedCenter = centerRepository.save(center);
        log.info("Center created successfully with id: {}", savedCenter.getId());

        return mapToResponse(savedCenter);
    }

    @Override
    @Transactional(readOnly = true)
    public CenterResponseDTO getCenterById(Long id) {
        log.debug("Fetching center with id: {}", id);
        Center center = centerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Center not found with id: " + id));
        return mapToResponse(center);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CenterResponseDTO> getAllCenters(Pageable pageable) {
        log.debug("Fetching all centers with pagination: {}", pageable);
        return centerRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CenterResponseDTO updateCenter(Long id, CenterRequestDTO request) {
        log.info("Updating center with id: {}", id);

        Center center = centerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Center not found with id: " + id));

        center.setCode(request.getCode());
        center.setName(request.getName());
        center.setDescription(request.getDescription());
        center.setPhone(request.getPhone());
        center.setEmail(request.getEmail());
        center.setAddress(request.getAddress());

        Center updatedCenter = centerRepository.save(center);
        log.info("Center updated successfully with id: {}", updatedCenter.getId());

        return mapToResponse(updatedCenter);
    }

    @Override
    @Transactional
    public void deleteCenter(Long id) {
        log.info("Deleting center with id: {}", id);

        if (!centerRepository.existsById(id)) {
            throw new IllegalArgumentException("Center not found with id: " + id);
        }

        centerRepository.deleteById(id);
        log.info("Center deleted successfully with id: {}", id);
    }

    private CenterResponseDTO mapToResponse(Center center) {
        return CenterResponseDTO.builder()
                .id(center.getId())
                .code(center.getCode())
                .name(center.getName())
                .description(center.getDescription())
                .phone(center.getPhone())
                .email(center.getEmail())
                .address(center.getAddress())
                .createdAt(center.getCreatedAt())
                .updatedAt(center.getUpdatedAt())
                .build();
    }
}
