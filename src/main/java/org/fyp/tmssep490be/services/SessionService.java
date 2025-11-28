package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.qa.SessionDetailDTO;

public interface SessionService {
    SessionDetailDTO getSessionDetail(Long sessionId, Long userId);
}
