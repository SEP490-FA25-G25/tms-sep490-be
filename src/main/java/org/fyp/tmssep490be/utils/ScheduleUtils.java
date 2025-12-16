package org.fyp.tmssep490be.utils;

import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Session;

import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScheduleUtils {

    public static String getDayNameVietnamese(int isoDayOfWeek) {
        return switch (DayOfWeek.of(isoDayOfWeek)) {
            case MONDAY -> "Thứ 2";
            case TUESDAY -> "Thứ 3";
            case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5";
            case FRIDAY -> "Thứ 6";
            case SATURDAY -> "Thứ 7";
            case SUNDAY -> "Chủ nhật";
        };
    }

    public static String getDayAbbreviationVietnamese(int zeroBasedDayOfWeek) {
        String[] dayNames = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        if (zeroBasedDayOfWeek >= 0 && zeroBasedDayOfWeek < dayNames.length) {
            return dayNames[zeroBasedDayOfWeek];
        }
        return "—";
    }

    public static Map<Integer, String> extractScheduleFromSessions(List<Session> sessions) {
        Map<Integer, String> dayTimeSlots = new LinkedHashMap<>();
        
        for (Session session : sessions) {
            if (session.getTimeSlotTemplate() != null && session.getDate() != null) {
                int isoDayOfWeek = session.getDate().getDayOfWeek().getValue(); // Monday=1, Sunday=7
                String timeSlot = String.format("%s-%s",
                    session.getTimeSlotTemplate().getStartTime(),
                    session.getTimeSlotTemplate().getEndTime());
                dayTimeSlots.putIfAbsent(isoDayOfWeek, timeSlot);
            }
        }
        
        return dayTimeSlots;
    }

    public static String generateScheduleSummary(List<Session> sessions) {
        if (sessions.isEmpty()) {
            return "Chưa có lịch";
        }

        Map<Integer, String> dayTimeSlots = extractScheduleFromSessions(sessions);
        if (dayTimeSlots.isEmpty()) {
            return "Chưa có lịch";
        }

        // Build full schedule with each day's time slot
        return dayTimeSlots.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> String.format("%s %s", getDayNameVietnamese(entry.getKey()), entry.getValue()))
            .collect(Collectors.joining(", "));
    }

    public static String generateScheduleDisplayFromMetadata(ClassEntity classEntity) {
        if (classEntity.getScheduleDays() == null || classEntity.getScheduleDays().length == 0) {
            return "Chưa có lịch cụ thể";
        }

        StringBuilder schedule = new StringBuilder();
        for (int i = 0; i < classEntity.getScheduleDays().length; i++) {
            if (i > 0) {
                schedule.append(", ");
            }
            int dayIndex = classEntity.getScheduleDays()[i];
            schedule.append(getDayAbbreviationVietnamese(dayIndex));
        }

        return schedule.toString();
    }

    public static String[] parseTimeSlot(String timeSlot) {
        if (timeSlot == null || timeSlot.isEmpty()) {
            return new String[]{"", ""};
        }
        
        String[] parts = timeSlot.split("-");
        if (parts.length >= 2) {
            return new String[]{parts[0].trim(), parts[1].trim()};
        }
        
        return new String[]{"", ""};
    }

    private ScheduleUtils() {
    }
}
