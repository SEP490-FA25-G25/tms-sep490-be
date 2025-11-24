package org.fyp.tmssep490be.dtos.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseMaterialDTO {
    private Long id;
    private String name; // HEAD uses name, Main uses title. Keeping both or mapping.
    private String title;
    private String description;

    // HEAD fields
    private String type;
    private String scope;
    private String url;

    // Main fields
    private String materialType;
    private String fileName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String level; // COURSE, PHASE, SESSION
    private Long phaseId;
    private Long sessionId;
    private Integer sequenceNo;
    private Boolean isAccessible;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
