package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.AttendanceDailySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceDailySummaryRepository extends JpaRepository<AttendanceDailySummary, Long> {

    Optional<AttendanceDailySummary> findByEmployeeEmployeeIdAndSummaryDate(Long employeeId, LocalDate summaryDate);

    List<AttendanceDailySummary> findAllBySummaryDate(LocalDate summaryDate);

    List<AttendanceDailySummary> findAllByEmployeeEmployeeIdOrderBySummaryDateDesc(Long employeeId);
}