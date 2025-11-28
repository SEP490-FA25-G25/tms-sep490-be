package org.fyp.tmssep490be.dtos.qa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAExportRequest {

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate dateFrom;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate dateTo;

    @NotBlank(message = "Định dạng xuất không được để trống")
    private String format; // EXCEL, CSV (currently only EXCEL supported)

    @NotNull(message = "Các phần xuất không được để trống")
    private List<ExportSection> includeSections;

    private List<Long> branchIds; // Optional: filter by specific branches

    public enum ExportSection {
        KPI_OVERVIEW("Tổng Quan KPIs"),
        CLASSES_REQUIRING_ATTENTION("Lớp Cần Chú Ý"),
        RECENT_QA_REPORTS("Báo Cáo QA Gần Đây");

        private final String displayName;

        ExportSection(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ExportFormat {
        EXCEL("Excel"),
        CSV("CSV");

        private final String displayName;

        ExportFormat(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}