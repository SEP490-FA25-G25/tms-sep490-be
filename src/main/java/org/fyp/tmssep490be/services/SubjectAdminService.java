package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.subject.SubjectDetailDTO;
import org.fyp.tmssep490be.dtos.subject.SubjectSummaryDTO;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;

import java.util.List;

public interface SubjectAdminService {

    List<SubjectSummaryDTO> getSubjectSummaries(SubjectStatus status, String search);

    SubjectDetailDTO getSubjectDetail(Long subjectId);
}

