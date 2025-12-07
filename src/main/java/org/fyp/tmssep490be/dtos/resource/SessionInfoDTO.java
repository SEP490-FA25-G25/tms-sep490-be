package org.fyp.tmssep490be.dtos.resource;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SessionInfoDTO {
    private Long id;
    private Long classId;
    private String classCode;
    private String className;
    private String date;
    private String startTime;
    private String endTime;
    private String status;
    private String type;
}