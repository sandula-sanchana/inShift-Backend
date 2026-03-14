package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.AttendanceDailySummary;
import edu.ijse.inshiftbackend.entity.enums.AttendanceDayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceDailySummaryRepository extends JpaRepository<AttendanceDailySummary, Long> {

    Optional<AttendanceDailySummary> findByEmployeeEmployeeIdAndSummaryDate(Long employeeId, LocalDate summaryDate);

    List<AttendanceDailySummary> findAllBySummaryDate(LocalDate summaryDate);

    List<AttendanceDailySummary> findAllByEmployeeEmployeeIdOrderBySummaryDateDesc(Long employeeId);

    long countBySummaryDateAndPresentTrue(LocalDate summaryDate);

    long countBySummaryDateAndDayStatus(LocalDate summaryDate, AttendanceDayStatus dayStatus);

    long countBySummaryDateAndOvertimeMinutesGreaterThan(LocalDate summaryDate, Integer overtimeMinutes);
}