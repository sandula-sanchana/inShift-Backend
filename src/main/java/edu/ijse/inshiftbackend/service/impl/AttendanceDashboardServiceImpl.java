package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.response.AdminAttendanceDashboardDTO;
import edu.ijse.inshiftbackend.entity.enums.AttendanceDayStatus;
import edu.ijse.inshiftbackend.entity.enums.AttendanceStatus;
import edu.ijse.inshiftbackend.repository.AttendanceDailySummaryRepository;
import edu.ijse.inshiftbackend.repository.AttendanceRecordRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.AttendanceDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AttendanceDashboardServiceImpl implements AttendanceDashboardService {

    private final AttendanceDailySummaryRepository summaryRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public AdminAttendanceDashboardDTO getTodayDashboard() {
        LocalDate today = LocalDate.now();

        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        long presentToday = summaryRepository.countBySummaryDateAndPresentTrue(today);
        long totalActiveEmployees = employeeRepository.countByActiveTrue();
        long absentToday = Math.max(0, totalActiveEmployees - presentToday);

        long lateToday = summaryRepository.countBySummaryDateAndDayStatus(today, AttendanceDayStatus.PRESENT_LATE);
        long incompleteToday = summaryRepository.countBySummaryDateAndDayStatus(today, AttendanceDayStatus.INCOMPLETE);
        long overtimeToday = summaryRepository.countBySummaryDateAndOvertimeMinutesGreaterThan(today, 0);

        long pendingApprovals = attendanceRecordRepository.countByStatusAndEventTimeBetween(
                AttendanceStatus.PENDING,
                start,
                end
        );

        return AdminAttendanceDashboardDTO.builder()
                .presentToday(presentToday)
                .absentToday(absentToday)
                .lateToday(lateToday)
                .pendingApprovals(pendingApprovals)
                .incompleteToday(incompleteToday)
                .overtimeToday(overtimeToday)
                .build();
    }
}