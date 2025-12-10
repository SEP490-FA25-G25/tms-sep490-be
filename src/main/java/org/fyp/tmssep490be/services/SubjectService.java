package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.subject.*;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.SubjectPhase;
import org.fyp.tmssep490be.entities.SubjectSession;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.CurriculumRepository;
import org.fyp.tmssep490be.repositories.LevelRepository;
import org.fyp.tmssep490be.repositories.SubjectRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CurriculumRepository curriculumRepository;
    private final LevelRepository levelRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;
    // Thêm các repositories khác nếu cần

    public List<SubjectDTO> getAllSubjects(Long curriculumId, Long levelId) {
        log.debug("Fetching subjects with filters - curriculumId: {}, levelId: {}", curriculumId, levelId);

        List<Subject> subjects;
        if (curriculumId != null && levelId != null) {
            subjects = subjectRepository.findByCurriculumIdAndLevelIdOrderByUpdatedAtDesc(curriculumId, levelId);
        } else if (curriculumId != null) {
            subjects = subjectRepository.findByCurriculumIdOrderByUpdatedAtDesc(curriculumId);
        } else if (levelId != null) {
            subjects = subjectRepository.findByLevelIdOrderByUpdatedAtDesc(levelId);
        } else {
            subjects = subjectRepository.findAll();
        }

        return subjects.stream()
                .map(this::toSubjectDTO)
                .collect(Collectors.toList());
    }

    private SubjectDTO toSubjectDTO(Subject subject) {
        return SubjectDTO.builder()
                .id(subject.getId())
                .name(subject.getName())
                .code(subject.getCode())
                .status(subject.getStatus() != null ? subject.getStatus().name() : null)
                .build();
    }
}
