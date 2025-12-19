package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.publicapi.PublicBranchDTO;
import org.fyp.tmssep490be.dtos.publicapi.PublicSubjectCatalogDTO;
import org.fyp.tmssep490be.dtos.publicapi.PublicSubjectDTO;
import org.fyp.tmssep490be.dtos.publicapi.PublicSubjectSimpleDTO;
import org.fyp.tmssep490be.dtos.publicapi.PublicScheduleDTO;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.SubjectPhase;
import org.fyp.tmssep490be.entities.CLO;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.enums.BranchStatus;
import org.fyp.tmssep490be.entities.enums.CurriculumStatus;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.CurriculumRepository;
import org.fyp.tmssep490be.repositories.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for public subject operations (landing page)
 * Only returns ACTIVE subjects for public visibility
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PublicSubjectService {

        private final CurriculumRepository curriculumRepository;
        private final SubjectRepository subjectRepository;
        private final BranchRepository branchRepository;
        private final ClassRepository classRepository;

        /**
         * Get all active branches for consultation form dropdown
         */
        public List<PublicBranchDTO> getPublicBranches() {
                log.info("Fetching public branches for consultation form");

                return branchRepository.findAll().stream()
                                .filter(b -> b.getStatus() == BranchStatus.ACTIVE)
                                .map(this::mapToPublicBranchDTO)
                                .collect(Collectors.toList());
        }

        /**
         * Get simple subject list for consultation form dropdown
         */
        public List<PublicSubjectSimpleDTO> getSimpleSubjectList() {
                log.info("Fetching simple subject list for consultation form");

                return subjectRepository.findAll().stream()
                                .filter(s -> s.getStatus() == SubjectStatus.ACTIVE)
                                .sorted(Comparator.comparing(Subject::getName))
                                .map(s -> PublicSubjectSimpleDTO.builder()
                                                .id(s.getId())
                                                .code(s.getCode())
                                                .name(s.getName())
                                                .build())
                                .collect(Collectors.toList());
        }

        /**
         * Get all subjects grouped by curriculum for landing page
         * Only returns ACTIVE curriculums and ACTIVE subjects
         */
        public PublicSubjectCatalogDTO getSubjectCatalog() {
                log.info("Fetching public subject catalog for landing page");

                // Get ACTIVE curriculums
                List<Curriculum> activeCurriculums = curriculumRepository
                                .findByStatusOrderByCode(CurriculumStatus.ACTIVE);

                List<PublicSubjectCatalogDTO.CurriculumGroup> curriculumGroups = activeCurriculums.stream()
                                .map(this::mapToCurriculumGroup)
                                .filter(group -> !group.getSubjects().isEmpty()) // Only include curriculums with subjects
                                .collect(Collectors.toList());

                return PublicSubjectCatalogDTO.builder()
                                .curriculums(curriculumGroups)
                                .build();
        }

        /**
         * Get subject detail by ID for public subject detail page
         * Only returns ACTIVE subjects
         */
        public PublicSubjectDTO getSubjectDetail(Long id) {
                log.info("Fetching public subject detail for ID: {}", id);

                Subject subject = subjectRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Subject not found with id: " + id));

                // Only return ACTIVE subjects for public display
                if (subject.getStatus() != SubjectStatus.ACTIVE) {
                        throw new RuntimeException("Subject not available");
                }

                return mapToPublicSubjectDTO(subject);
        }

        private PublicBranchDTO mapToPublicBranchDTO(Branch branch) {
                return PublicBranchDTO.builder()
                                .id(branch.getId())
                                .name(branch.getName())
                                .address(branch.getAddress())
                                .phone(branch.getPhone())
                                .email(branch.getEmail())
                                .build();
        }

        private PublicSubjectCatalogDTO.CurriculumGroup mapToCurriculumGroup(Curriculum curriculum) {
                // Get ACTIVE subjects for this curriculum
                List<Subject> activeSubjects = subjectRepository.findByCurriculumId(curriculum.getId())
                                .stream()
                                .filter(s -> s.getStatus() == SubjectStatus.ACTIVE)
                                .sorted(Comparator.comparing(
                                                s -> s.getLevel() != null ? s.getLevel().getSortOrder() : 0))
                                .collect(Collectors.toList());

                List<PublicSubjectDTO> subjects = activeSubjects.stream()
                                .map(this::mapToPublicSubjectDTO)
                                .collect(Collectors.toList());

                return PublicSubjectCatalogDTO.CurriculumGroup.builder()
                                .id(curriculum.getId())
                                .code(curriculum.getCode())
                                .name(curriculum.getName())
                                .description(curriculum.getDescription())
                                .subjects(subjects)
                                .build();
        }

        private PublicSubjectDTO mapToPublicSubjectDTO(Subject subject) {
                List<PublicSubjectDTO.PublicSubjectPhaseDTO> phases = new ArrayList<>();

                if (subject.getSubjectPhases() != null) {
                        phases = subject.getSubjectPhases().stream()
                                        .sorted(Comparator.comparing(SubjectPhase::getPhaseNumber))
                                        .map(phase -> PublicSubjectDTO.PublicSubjectPhaseDTO.builder()
                                                        .id(phase.getId())
                                                        .phaseNumber(phase.getPhaseNumber())
                                                        .name(phase.getName())
                                                        .build())
                                        .collect(Collectors.toList());
                }

                List<PublicSubjectDTO.PublicSubjectCLODTO> clos = new ArrayList<>();
                if (subject.getClos() != null) {
                        clos = subject.getClos().stream()
                                        .sorted(Comparator.comparing(CLO::getCode))
                                        .map(clo -> PublicSubjectDTO.PublicSubjectCLODTO.builder()
                                                        .code(clo.getCode())
                                                        .description(clo.getDescription())
                                                        .build())
                                        .collect(Collectors.toList());
                }

                return PublicSubjectDTO.builder()
                                .id(subject.getId())
                                .code(subject.getCode())
                                .name(subject.getName())
                                .description(subject.getDescription())
                                .thumbnailUrl(subject.getThumbnailUrl())
                                .levelName(subject.getLevel() != null ? subject.getLevel().getName() : null)
                                .levelDescription(
                                                subject.getLevel() != null ? subject.getLevel().getDescription() : null)
                                .curriculumCode(subject.getCurriculum().getCode())
                                .curriculumName(subject.getCurriculum().getName())
                                .totalHours(subject.getTotalHours())
                                .numberOfSessions(subject.getNumberOfSessions())
                                .hoursPerSession(subject.getHoursPerSession())
                                .phases(phases)
                                .clos(clos)
                                .build();
        }

        /**
         * Get upcoming scheduled classes for public schedule page
         */
        public List<PublicScheduleDTO> getUpcomingSchedules() {
                log.info("Fetching upcoming scheduled classes for public schedule page");

                List<ClassEntity> classes = classRepository.findUpcomingScheduledClasses(LocalDate.now());

                return classes.stream()
                                .map(this::mapToPublicScheduleDTO)
                                .collect(Collectors.toList());
        }

        private PublicScheduleDTO mapToPublicScheduleDTO(ClassEntity classEntity) {
                Integer enrolledCount = classRepository.countEnrolledStudents(classEntity.getId());
                if (enrolledCount == null)
                        enrolledCount = 0;

                Integer maxCapacity = classEntity.getMaxCapacity() != null ? classEntity.getMaxCapacity() : 0;

                // Calculate status based on enrollment
                String status;
                String statusColor;
                if (maxCapacity > 0 && enrolledCount >= maxCapacity) {
                        status = "Hết chỗ";
                        statusColor = "bg-red-500";
                } else if (maxCapacity > 0 && enrolledCount >= maxCapacity * 0.9) {
                        status = "Gần hết chỗ";
                        statusColor = "bg-yellow-500";
                } else {
                        status = "Còn chỗ";
                        statusColor = "bg-emerald-500";
                }

                // Get time slot from first session
                String timeSlot = "N/A";
                if (classEntity.getSessions() != null && !classEntity.getSessions().isEmpty()) {
                        Session firstSession = classEntity.getSessions().stream()
                                        .min(Comparator.comparing(Session::getDate))
                                        .orElse(null);
                        if (firstSession != null && firstSession.getTimeSlotTemplate() != null) {
                                LocalTime start = firstSession.getTimeSlotTemplate().getStartTime();
                                LocalTime end = firstSession.getTimeSlotTemplate().getEndTime();
                                if (start != null && end != null) {
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH'h'");
                                        timeSlot = start.format(formatter) + "-" + end.format(formatter);
                                }
                        }
                }

                return PublicScheduleDTO.builder()
                                .id(classEntity.getId())
                                .code(classEntity.getCode())
                                .courseName(classEntity.getSubject() != null ? classEntity.getSubject().getName()
                                                : "N/A")
                                .timeSlot(timeSlot)
                                .startDate(classEntity.getStartDate())
                                .status(status)
                                .statusColor(statusColor)
                                .maxCapacity(maxCapacity)
                                .enrolledCount(enrolledCount)
                                .branchCode(classEntity.getBranch() != null ? classEntity.getBranch().getCode() : null)
                                .branchName(classEntity.getBranch() != null ? classEntity.getBranch().getName() : null)
                                .modality(classEntity.getModality() != null ? classEntity.getModality().name() : null)
                                .build();
        }
}
