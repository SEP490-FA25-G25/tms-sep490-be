package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.teacherclass.TeacherClassListItemDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.services.TeacherClassService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherClassServiceImpl implements TeacherClassService {
    
    private final TeachingSlotRepository teachingSlotRepository;

    //Get all classes assigned to a specific teacher
    @Override
    public List<TeacherClassListItemDTO> getTeacherClasses(Long teacherId) {
        // Query distinct classes where teacher has teaching slots with SCHEDULED status
        List<ClassEntity> classes = teachingSlotRepository.findDistinctClassesByTeacherId(teacherId);
        
        // Convert Entity to DTO for response
        return classes.stream()
                .map(this::mapToTeacherClassListItemDTO)
                .collect(Collectors.toList());
    }
    
    //Map ClassEntity to TeacherClassListItemDTO
    private TeacherClassListItemDTO mapToTeacherClassListItemDTO(ClassEntity classEntity) {
        return TeacherClassListItemDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                // Map subject information (subject is equivalent to course in this system)
                .courseName(classEntity.getSubject() != null ? classEntity.getSubject().getName() : null)
                .courseCode(classEntity.getSubject() != null ? classEntity.getSubject().getCode() : null)
                // Map branch information
                .branchName(classEntity.getBranch() != null ? classEntity.getBranch().getName() : null)
                .branchCode(classEntity.getBranch() != null ? classEntity.getBranch().getCode() : null)
                // Map schedule and status information
                .modality(classEntity.getModality())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .status(classEntity.getStatus())
                //Calculate total sessions from SessionRepository
                .totalSessions(null)
                .attendanceRate(null)
                .build();
    }
}