package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.AttendanceRecord;
import edu.ijse.inshiftbackend.entity.enums.AttendanceStatus;
import edu.ijse.inshiftbackend.entity.enums.AttendanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findTopByEmployeeEmployeeIdOrderByEventTimeDesc(Long employeeId);

    //last record that is "accepted" (VALID) - used to decide active session
    Optional<AttendanceRecord> findTopByEmployeeEmployeeIdAndStatusOrderByEventTimeDesc(
            Long employeeId,
            AttendanceStatus status
    );


    //to load all pending attendance for admin
    List<AttendanceRecord> findAllByStatusOrderByEventTimeDesc(AttendanceStatus status);

    List<AttendanceRecord> findAllByEmployeeEmployeeIdAndEventTimeBetweenOrderByEventTimeAsc(
            Long employeeId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<AttendanceRecord> findAllByEmployeeEmployeeIdAndTypeAndStatusAndEventTimeBetweenOrderByEventTimeAsc(
            Long employeeId,
            AttendanceType type,
            AttendanceStatus status,
            LocalDateTime start,
            LocalDateTime end
    );


    long countByStatusAndEventTimeBetween(AttendanceStatus status, LocalDateTime start, LocalDateTime end);

    boolean existsByEmployeeEmployeeIdAndTypeAndStatusAndEventTimeBetween(
            Long employeeId,
            AttendanceType type,
            AttendanceStatus status,
            LocalDateTime start,
            LocalDateTime end
    );


}