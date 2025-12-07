package org.fyp.tmssep490be.services;

import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApprovalService {

    private String buildScheduleDisplay(ClassEntity classEntity) {
        if (classEntity.getScheduleDays() == null || classEntity.getScheduleDays().length == 0) {
            return "Chưa có lịch cụ thể";
        }

        String[] dayNames = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        StringBuilder schedule = new StringBuilder();

        for (int i = 0; i < classEntity.getScheduleDays().length; i++) {
            if (i > 0) schedule.append(", ");
            int dayIndex = classEntity.getScheduleDays()[i];
            if (dayIndex >= 0 && dayIndex < dayNames.length) {
                schedule.append(dayNames[dayIndex]);
            }
        }

        return schedule.toString();
    }

}
