package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceRecordRepository
        extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findTopByEmployeeEmployeeIdOrderByEventTimeDesc(Long employeeId);
}