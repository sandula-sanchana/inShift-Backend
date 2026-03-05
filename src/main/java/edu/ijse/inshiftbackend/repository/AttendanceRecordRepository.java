package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.AttendanceRecord;
import edu.ijse.inshiftbackend.entity.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findTopByEmployeeEmployeeIdOrderByEventTimeDesc(Long employeeId);

    //last record that is "accepted" (VALID) - used to decide active session
    Optional<AttendanceRecord> findTopByEmployeeEmployeeIdAndStatusOrderByEventTimeDesc(
            Long employeeId,
            AttendanceStatus status
    );


    //to load all pending attendance for admin
    List<AttendanceRecord> findAllByStatusOrderByEventTimeDesc(AttendanceStatus status);
}