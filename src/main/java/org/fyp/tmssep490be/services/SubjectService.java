package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.subject.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.repositories.*;
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
                log.debug("Getting all subjects with filters - curriculumId: {}, levelId: {}", curriculumId, levelId);

                List<Subject> subjects;

                if (curriculumId != null && levelId != null) {
                        subjects = subjectRepository.findByCurriculumIdAndLevelIdOrderByUpdatedAtDesc(curriculumId,
                                        levelId);
                } else if (curriculumId != null) {
                        subjects = subjectRepository.findByCurriculumIdOrderByUpdatedAtDesc(curriculumId);
                } else if (levelId != null) {
                        subjects = subjectRepository.findByLevelIdOrderByUpdatedAtDesc(levelId);
                } else {
                        subjects = subjectRepository.findAll();
                }

                return subjects.stream()
                                .map(subject -> SubjectDTO.builder()
                                                .id(subject.getId())
                                                .code(subject.getCode())
                                                .name(subject.getName())
                                                .subjectName(subject.getCurriculum() != null
                                                                ? subject.getCurriculum().getName()
                                                                : null)
                                                .levelName(subject.getLevel() != null ? subject.getLevel().getName()
                                                                : null)
                                                .status(subject.getStatus() != null ? subject.getStatus().name() : null)
                                                .approvalStatus(subject.getApprovalStatus() != null
                                                                ? subject.getApprovalStatus().name()
                                                                : null)
                                                .rejectionReason(subject.getRejectionReason())
                                                .submittedAt(subject.getSubmittedAt())
                                                .decidedAt(subject.getDecidedAt())
                                                .effectiveDate(subject.getEffectiveDate())
                                                .createdAt(subject.getCreatedAt())
                                                .updatedAt(subject.getUpdatedAt())
                                                .build())
                                .collect(Collectors.toList());
        }

        public SubjectDetailDTO getSubjectDetails(Long id) {
                throw new UnsupportedOperationException("Will be implemented in next endpoint");
        }


        @Transactional
        public SubjectDetailDTO createSubject(CreateSubjectRequestDTO request, Long userId) {
                throw new UnsupportedOperationException("Will be implemented in next endpoint");
        }


        public SubjectDetailDTO getSubjectSyllabus(Long subjectId) {
                // For now, delegate to getSubjectDetails
                return getSubjectDetails(subjectId);
        }


    public MaterialHierarchyDTO getSubjectMaterials(Long subjectId, Long studentId) {
        throw new UnsupportedOperationException("Will be implemented in next endpoint");
    }
}
