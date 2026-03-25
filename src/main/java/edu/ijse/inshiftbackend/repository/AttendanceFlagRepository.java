package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.AttendanceFlag;
import edu.ijse.inshiftbackend.entity.enums.AttendanceFlagType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceFlagRepository extends JpaRepository<AttendanceFlag, Long> {

//    List<AttendanceFlag> findAllByEmployeeEmployeeIdAndAttendanceDateOrderByDetectedAtDesc(
//            Long employeeId, LocalDate attendanceDate
//    );
//
//    boolean existsByEmployeeEmployeeIdAndAttendanceDateAndFlagTypeAndResolvedFalse(
//            Long employeeId, LocalDate attendanceDate, AttendanceFlagType flagType
//    );
//
//    long countByEmployeeEmployeeIdAndDetectedAtAfter(
//            Long employeeId, LocalDateTime after
//    );
//
//    List<AttendanceFlag> findAllByAttendanceDateOrderByDetectedAtDesc(LocalDate attendanceDate);

    void deleteByEmployeeEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    List<AttendanceFlag> findByEmployeeEmployeeIdAndAttendanceDateAndResolvedFalse(
            Long employeeId,
            LocalDate attendanceDate
    );

    List<AttendanceFlag> findByEmployeeEmployeeIdAndAttendanceDateOrderByDetectedAtDesc(Long employeeId, LocalDate attendanceDate);

    List<AttendanceFlag> findByEmployeeEmployeeIdAndAttendanceDateBetweenOrderByDetectedAtDesc(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );
}