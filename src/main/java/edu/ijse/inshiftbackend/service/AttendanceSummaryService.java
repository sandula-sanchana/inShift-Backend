package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.response.AttendanceDailySummaryResponseDTO;

import java.time.LocalDate;

public interface AttendanceSummaryService {
    public AttendanceDailySummaryResponseDTO generateDailySummary(Long employeeId, LocalDate date);

    AttendanceDailySummaryResponseDTO getTodaySummaryByEmail(String email);

    AttendanceDailySummaryResponseDTO getSummaryByEmailAndDate(String email, LocalDate date);
}
