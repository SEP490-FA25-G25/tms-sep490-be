package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.CourseDTO;
import org.fyp.tmssep490be.repositories.CourseRepository;
import org.fyp.tmssep490be.services.CourseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of CourseService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    @Override
    public List<CourseDTO> getAllCourses() {
        log.debug("Getting all courses for dropdown");
        
        return courseRepository.findAll().stream()
                .map(course -> CourseDTO.builder()
                        .id(course.getId())
                        .name(course.getName())
                        .code(course.getCode())
                        .build())
                .collect(Collectors.toList());
    }
}
