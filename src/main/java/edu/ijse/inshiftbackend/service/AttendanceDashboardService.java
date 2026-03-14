package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.response.AdminAttendanceDashboardDTO;

public interface AttendanceDashboardService {
    AdminAttendanceDashboardDTO getTodayDashboard();
}