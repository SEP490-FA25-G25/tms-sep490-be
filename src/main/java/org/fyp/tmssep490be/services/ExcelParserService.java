package org.fyp.tmssep490be.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fyp.tmssep490be.dtos.enrollment.StudentEnrollmentData;
import org.fyp.tmssep490be.dtos.enrollment.StudentResolutionStatus;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ExcelParserService {

    private static final int COLUMN_FULL_NAME = 0;
    private static final int COLUMN_EMAIL = 1;
    private static final int COLUMN_PHONE = 2;
    private static final int COLUMN_FACEBOOK_URL = 3;
    private static final int COLUMN_ADDRESS = 4;
    private static final int COLUMN_GENDER = 5;
    private static final int COLUMN_DOB = 6;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };

    public List<StudentEnrollmentData> parseStudentEnrollment(MultipartFile file) {
        List<StudentEnrollmentData> students = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // For generic templates: skip header row only (start from row 1)
            // For class-specific templates: skip class info row (row 0) and header row (row
            // 1), start from row 2
            // Detect if this is a class-specific template by checking first cell content
            Row firstRow = sheet.getRow(0);
            boolean isClassSpecificTemplate = false;

            if (firstRow != null && firstRow.getCell(0) != null) {
                String firstCellValue = firstRow.getCell(0).getStringCellValue();
                if (firstCellValue != null && firstCellValue.startsWith("Class:")) {
                    isClassSpecificTemplate = true;
                }
            }

            // Start parsing from appropriate row
            int startRow = isClassSpecificTemplate ? 2 : 1;
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                try {
                    StudentEnrollmentData data = parseRow(row, i);
                    students.add(data);
                } catch (Exception e) {
                    log.warn("Error parsing row {}: {}", i + 1, e.getMessage());
                    // Mark row có lỗi
                    StudentEnrollmentData errorData = StudentEnrollmentData.builder()
                            .status(StudentResolutionStatus.ERROR)
                            .errorMessage("Row " + (i + 1) + ": " + e.getMessage())
                            .build();
                    students.add(errorData);
                }
            }

            if (students.isEmpty()) {
                throw new CustomException(ErrorCode.EXCEL_FILE_EMPTY);
            }

        } catch (IOException e) {
            log.error("Failed to parse Excel file", e);
            throw new CustomException(ErrorCode.EXCEL_PARSE_FAILED);
        }

        return students;
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> {
                String value = cell.getStringCellValue().trim();
                yield value.isEmpty() ? null : value;
            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Date cell
                    LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
                    yield date.toString();
                } else {
                    // Numeric cell - convert to string without decimal
                    yield String.valueOf((long) cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case BLANK -> null;
            default -> cell.toString().trim();
        };
    }

    private StudentEnrollmentData parseRow(Row row, int rowIndex) {
        return StudentEnrollmentData.builder()
                .fullName(getCellValueAsString(row.getCell(COLUMN_FULL_NAME)))
                .email(getCellValueAsString(row.getCell(COLUMN_EMAIL)))
                .phone(getCellValueAsString(row.getCell(COLUMN_PHONE)))
                .facebookUrl(getCellValueAsString(row.getCell(COLUMN_FACEBOOK_URL)))
                .address(getCellValueAsString(row.getCell(COLUMN_ADDRESS)))
                .gender(parseGender(getCellValueAsString(row.getCell(COLUMN_GENDER))))
                .dob(parseDob(getCellValueAsString(row.getCell(COLUMN_DOB))))
                .build();
    }

    private Gender parseGender(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            // Support: "male", "MALE", "Male", "m", "M"
            String normalized = value.toLowerCase().trim();
            return switch (normalized) {
                case "male", "m" -> Gender.MALE;
                case "female", "f" -> Gender.FEMALE;
                case "other", "o" -> Gender.OTHER;
                default -> throw new IllegalArgumentException("Giá trị giới tính không hợp lệ: " + value);
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Giới tính không hợp lệ: " + value);
        }
    }

    private LocalDate parseDob(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        throw new IllegalArgumentException("Định dạng ngày không hợp lệ: " + value
                + ". Định dạng mong đợi: yyyy-MM-dd, dd/MM/yyyy, hoặc MM/dd/yyyy");
    }

    private org.fyp.tmssep490be.dtos.studentmanagement.StudentImportData parseStudentImportRow(Row row, int rowIndex) {
        return org.fyp.tmssep490be.dtos.studentmanagement.StudentImportData.builder()
                .fullName(getCellValueAsString(row.getCell(COLUMN_FULL_NAME)))
                .email(getCellValueAsString(row.getCell(COLUMN_EMAIL)))
                .phone(getCellValueAsString(row.getCell(COLUMN_PHONE)))
                .facebookUrl(getCellValueAsString(row.getCell(COLUMN_FACEBOOK_URL)))
                .address(getCellValueAsString(row.getCell(COLUMN_ADDRESS)))
                .gender(parseGender(getCellValueAsString(row.getCell(COLUMN_GENDER))))
                .dob(parseDob(getCellValueAsString(row.getCell(COLUMN_DOB))))
                .build();
    }

    public List<org.fyp.tmssep490be.dtos.studentmanagement.StudentImportData> parseStudentImport(MultipartFile file) {
        List<org.fyp.tmssep490be.dtos.studentmanagement.StudentImportData> students = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row (row 0), start from row 1
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                try {
                    org.fyp.tmssep490be.dtos.studentmanagement.StudentImportData data = parseStudentImportRow(row, i);
                    students.add(data);
                } catch (Exception e) {
                    log.warn("Error parsing row {}: {}", i + 1, e.getMessage());
                    // Mark row có lỗi
                    org.fyp.tmssep490be.dtos.studentmanagement.StudentImportData errorData =
                            org.fyp.tmssep490be.dtos.studentmanagement.StudentImportData.builder()
                                    .status(org.fyp.tmssep490be.dtos.studentmanagement.StudentImportData.StudentImportStatus.ERROR)
                                    .errorMessage("Hàng " + (i + 1) + ": " + e.getMessage())
                                    .build();
                    students.add(errorData);
                }
            }

            if (students.isEmpty()) {
                throw new CustomException(ErrorCode.EXCEL_FILE_EMPTY);
            }

        } catch (IOException e) {
            log.error("Failed to parse Excel file", e);
            throw new CustomException(ErrorCode.EXCEL_PARSE_FAILED);
        }

        return students;
    }


}
