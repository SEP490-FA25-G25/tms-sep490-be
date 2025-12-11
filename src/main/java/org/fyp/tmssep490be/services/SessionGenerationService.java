package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.SubjectSession;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.SessionType;
import org.fyp.tmssep490be.repositories.SubjectSessionRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class SessionGenerationService {

    private final SubjectSessionRepository subjectSessionRepository;

    // Sinh sessions cho lớp dựa trên subject template
    public List<Session> generateSessionsForClass(ClassEntity classEntity, Subject subject) {
        log.info("Sinh sessions cho lớp {} từ subject {}", classEntity.getCode(), subject.getCode());

        List<SubjectSession> subjectSessions = subjectSessionRepository
                .findByPhase_Subject_IdOrderByPhaseAscSequenceNoAsc(subject.getId());

        if (subjectSessions.isEmpty()) {
            log.warn("Không có subject sessions cho subject {}", subject.getCode());
            return new ArrayList<>();
        }

        LocalDate currentDate = classEntity.getStartDate();
        Short[] scheduleDays = classEntity.getScheduleDays();
        List<Session> sessions = new ArrayList<>();
        int sessionIndex = 0;
        OffsetDateTime now = OffsetDateTime.now();

        for (SubjectSession subjectSession : subjectSessions) {
            Short targetDayOfWeek = scheduleDays[sessionIndex % scheduleDays.length];
            LocalDate sessionDate = findNextDateForDayOfWeek(currentDate, targetDayOfWeek);

            Session session = Session.builder()
                    .classEntity(classEntity)
                    .subjectSession(subjectSession)
                    .date(sessionDate)
                    .type(SessionType.CLASS)
                    .status(SessionStatus.PLANNED)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            sessions.add(session);
            sessionIndex++;
            currentDate = sessionDate.plusDays(1);
        }

        log.info("Đã sinh {} sessions cho lớp {}", sessions.size(), classEntity.getCode());
        return sessions;
    }

    // Tính ngày kết thúc dự kiến
    public LocalDate calculateEndDate(List<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return null;
        }
        return sessions.stream()
                .map(Session::getDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    // Kiểm tra schedule hợp lệ
    public boolean isValidSchedule(ClassEntity classEntity) {
        if (classEntity.getStartDate() == null) {
            return false;
        }
        if (classEntity.getScheduleDays() == null || classEntity.getScheduleDays().length == 0) {
            return false;
        }
        for (Short day : classEntity.getScheduleDays()) {
            if (day < 1 || day > 7) {
                return false;
            }
        }
        return true;
    }

    // Tìm ngày tiếp theo khớp với ngày trong tuần
    private LocalDate findNextDateForDayOfWeek(LocalDate fromDate, Short dayOfWeek) {
        LocalDate currentDate = fromDate;
        DayOfWeek targetDay = DayOfWeek.of(dayOfWeek);
        while (currentDate.getDayOfWeek() != targetDay) {
            currentDate = currentDate.plusDays(1);
        }
        return currentDate;
    }
}
