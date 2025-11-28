package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.course.CoursePhaseDTO;
import org.fyp.tmssep490be.entities.CoursePhase;
import org.fyp.tmssep490be.repositories.CoursePhaseRepository;
import org.fyp.tmssep490be.services.CoursePhaseService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoursePhaseServiceImpl implements CoursePhaseService {

    private final CoursePhaseRepository coursePhaseRepository;

    @Override
    public List<CoursePhaseDTO> getPhasesByCourseId(Long courseId) {
        List<CoursePhase> phases = coursePhaseRepository.findByCourseIdOrderByPhaseNumber(courseId);

        return phases.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CoursePhaseDTO> getAllPhases() {
        List<CoursePhase> phases = coursePhaseRepository.findAll();

        return phases.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private CoursePhaseDTO convertToDTO(CoursePhase phase) {
        return CoursePhaseDTO.builder()
                .id(phase.getId())
                .courseId(phase.getCourse() != null ? phase.getCourse().getId() : null)
                .courseName(phase.getCourse() != null ? phase.getCourse().getName() : null)
                .phaseNumber(phase.getPhaseNumber())
                .name(phase.getName())
                .durationWeeks(phase.getDurationWeeks())
                .learningFocus(phase.getLearningFocus())
                .description(phase.getLearningFocus()) // Map learningFocus to description for compatibility
                .build();
    }
}
