package org.fyp.tmssep490be.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service to check Vietnam public holidays.
 * 
 * Holidays are based on Vietnamese government regulations (Nghị định
 * 145/2018/NĐ-CP).
 * 
 * Fixed holidays (same date every year):
 * - New Year's Day: January 1
 * - Liberation Day: April 30
 * - International Labor Day: May 1
 * - National Day: September 2
 * 
 * Lunar calendar holidays (pre-calculated for each year):
 * - Tết Nguyên đán (Vietnamese New Year): ~7 days around Lunar New Year
 * - Giỗ tổ Hùng Vương (Hung Kings Commemoration): 10th day of 3rd lunar month
 * 
 * Note: Lunar dates are pre-calculated for years 2025-2030 to avoid external
 * dependencies.
 */
@Service
@Slf4j
public class VietnamHolidayService {

    // Fixed holidays (same date every year)
    private static final Set<MonthDay> FIXED_HOLIDAYS = Set.of(
            MonthDay.of(1, 1), // Tết Dương lịch
            MonthDay.of(4, 30), // Giải phóng miền Nam
            MonthDay.of(5, 1), // Quốc tế Lao động
            MonthDay.of(9, 2) // Quốc khánh
    );

    // Lunar holidays pre-calculated for each year (Tết Nguyên đán)
    // Format: Year -> Set of dates for Tết (usually 7 days: from 29/12 âm to mùng
    // 5/1 âm)
    private static final Map<Integer, Set<LocalDate>> TET_HOLIDAYS = new HashMap<>();

    // Giỗ tổ Hùng Vương (10/3 âm lịch) pre-calculated
    private static final Map<Integer, LocalDate> HUNG_KINGS_DAY = new HashMap<>();

    static {
        // Tết Nguyên đán 2025: 29/01/2025 (Mùng 1)
        // Nghỉ từ 28/01 đến 02/02/2025 (29 Tết đến Mùng 5)
        TET_HOLIDAYS.put(2025, Set.of(
                LocalDate.of(2025, 1, 28),
                LocalDate.of(2025, 1, 29),
                LocalDate.of(2025, 1, 30),
                LocalDate.of(2025, 1, 31),
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 2, 2)));
        HUNG_KINGS_DAY.put(2025, LocalDate.of(2025, 4, 6)); // 10/3 âm lịch

        // Tết Nguyên đán 2026: 17/02/2026 (Mùng 1)
        // Nghỉ từ 16/02 đến 22/02/2026
        TET_HOLIDAYS.put(2026, Set.of(
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 17),
                LocalDate.of(2026, 2, 18),
                LocalDate.of(2026, 2, 19),
                LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 2, 21),
                LocalDate.of(2026, 2, 22)));
        HUNG_KINGS_DAY.put(2026, LocalDate.of(2026, 4, 26)); // 10/3 âm lịch

        // Tết Nguyên đán 2027: 06/02/2027 (Mùng 1)
        TET_HOLIDAYS.put(2027, Set.of(
                LocalDate.of(2027, 2, 5),
                LocalDate.of(2027, 2, 6),
                LocalDate.of(2027, 2, 7),
                LocalDate.of(2027, 2, 8),
                LocalDate.of(2027, 2, 9),
                LocalDate.of(2027, 2, 10),
                LocalDate.of(2027, 2, 11)));
        HUNG_KINGS_DAY.put(2027, LocalDate.of(2027, 4, 15)); // 10/3 âm lịch

        // Tết Nguyên đán 2028: 26/01/2028 (Mùng 1)
        TET_HOLIDAYS.put(2028, Set.of(
                LocalDate.of(2028, 1, 25),
                LocalDate.of(2028, 1, 26),
                LocalDate.of(2028, 1, 27),
                LocalDate.of(2028, 1, 28),
                LocalDate.of(2028, 1, 29),
                LocalDate.of(2028, 1, 30),
                LocalDate.of(2028, 1, 31)));
        HUNG_KINGS_DAY.put(2028, LocalDate.of(2028, 4, 4)); // 10/3 âm lịch

        // Tết Nguyên đán 2029: 13/02/2029 (Mùng 1)
        TET_HOLIDAYS.put(2029, Set.of(
                LocalDate.of(2029, 2, 12),
                LocalDate.of(2029, 2, 13),
                LocalDate.of(2029, 2, 14),
                LocalDate.of(2029, 2, 15),
                LocalDate.of(2029, 2, 16),
                LocalDate.of(2029, 2, 17),
                LocalDate.of(2029, 2, 18)));
        HUNG_KINGS_DAY.put(2029, LocalDate.of(2029, 4, 23)); // 10/3 âm lịch

        // Tết Nguyên đán 2030: 03/02/2030 (Mùng 1)
        TET_HOLIDAYS.put(2030, Set.of(
                LocalDate.of(2030, 2, 2),
                LocalDate.of(2030, 2, 3),
                LocalDate.of(2030, 2, 4),
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 6),
                LocalDate.of(2030, 2, 7),
                LocalDate.of(2030, 2, 8)));
        HUNG_KINGS_DAY.put(2030, LocalDate.of(2030, 4, 12)); // 10/3 âm lịch
    }

    /**
     * Check if a given date is a Vietnam public holiday.
     *
     * @param date the date to check
     * @return true if it's a holiday, false otherwise
     */
    public boolean isHoliday(LocalDate date) {
        if (date == null) {
            return false;
        }

        // Check fixed holidays
        MonthDay monthDay = MonthDay.from(date);
        if (FIXED_HOLIDAYS.contains(monthDay)) {
            log.debug("Date {} is a fixed holiday", date);
            return true;
        }

        int year = date.getYear();

        // Check Tết Nguyên đán
        Set<LocalDate> tetDates = TET_HOLIDAYS.get(year);
        if (tetDates != null && tetDates.contains(date)) {
            log.debug("Date {} is Tết Nguyên đán", date);
            return true;
        }

        // Check Giỗ tổ Hùng Vương
        LocalDate hungKingsDay = HUNG_KINGS_DAY.get(year);
        if (hungKingsDay != null && hungKingsDay.equals(date)) {
            log.debug("Date {} is Giỗ tổ Hùng Vương", date);
            return true;
        }

        return false;
    }

    /**
     * Get the name of the holiday for a given date.
     *
     * @param date the date to check
     * @return the holiday name, or null if not a holiday
     */
    public String getHolidayName(LocalDate date) {
        if (date == null) {
            return null;
        }

        MonthDay monthDay = MonthDay.from(date);

        // Check fixed holidays
        if (monthDay.equals(MonthDay.of(1, 1))) {
            return "Tết Dương lịch";
        }
        if (monthDay.equals(MonthDay.of(4, 30))) {
            return "Ngày Giải phóng miền Nam";
        }
        if (monthDay.equals(MonthDay.of(5, 1))) {
            return "Ngày Quốc tế Lao động";
        }
        if (monthDay.equals(MonthDay.of(9, 2))) {
            return "Ngày Quốc khánh";
        }

        int year = date.getYear();

        // Check Tết
        Set<LocalDate> tetDates = TET_HOLIDAYS.get(year);
        if (tetDates != null && tetDates.contains(date)) {
            return "Tết Nguyên đán";
        }

        // Check Giỗ tổ Hùng Vương
        LocalDate hungKingsDay = HUNG_KINGS_DAY.get(year);
        if (hungKingsDay != null && hungKingsDay.equals(date)) {
            return "Giỗ tổ Hùng Vương";
        }

        return null;
    }
}
