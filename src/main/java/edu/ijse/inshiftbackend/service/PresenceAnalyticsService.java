package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.response.PresenceAnalyticsResponseDTO;

public interface PresenceAnalyticsService {
    PresenceAnalyticsResponseDTO getEmployeePresenceAnalytics(Long employeeId);
}