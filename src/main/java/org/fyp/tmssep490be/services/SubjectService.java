package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.subject.SubjectDTO;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.repositories.SubjectRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public List<SubjectDTO> getAllSubjects(Long curriculumId, Long levelId) {
        log.debug("Getting all subjects for dropdown with filters - curriculumId: {}, levelId: {}", curriculumId, levelId);

        List<Subject> subjects;

        if (curriculumId != null && levelId != null) {
            subjects = subjectRepository.findByCurriculumIdAndLevelIdOrderByUpdatedAtDesc(curriculumId, levelId);
        } else if (curriculumId != null) {
            subjects = subjectRepository.findByCurriculumIdOrderByUpdatedAtDesc(curriculumId);
        } else if (levelId != null) {
            subjects = subjectRepository.findByLevelIdOrderByUpdatedAtDesc(levelId);
        } else {
            subjects = subjectRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
        }

        return subjects.stream()
                .map(subject -> SubjectDTO.builder()
                        .id(subject.getId())
                        .name(subject.getName())
                        .code(subject.getCode())
                        .status(subject.getStatus().name())
                        .approvalStatus(subject.getApprovalStatus() != null ? subject.getApprovalStatus().name() : null)
                        .rejectionReason(subject.getRejectionReason())
                        .submittedAt(subject.getSubmittedAt())
                        .decidedAt(subject.getDecidedAt())
                        .effectiveDate(subject.getEffectiveDate())
                        .createdAt(subject.getCreatedAt())
                        .updatedAt(subject.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
