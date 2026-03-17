package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.AttendanceRiskScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AttendanceRiskScoreRepository extends JpaRepository<AttendanceRiskScore, Long> {

    Optional<AttendanceRiskScore> findByEmployeeEmployeeIdAndAttendanceDate(
            Long employeeId, LocalDate attendanceDate
    );
}