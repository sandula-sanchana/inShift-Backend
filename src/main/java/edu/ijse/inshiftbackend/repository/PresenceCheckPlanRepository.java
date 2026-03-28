package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.PresenceCheckPlan;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PresenceCheckPlanRepository extends JpaRepository<PresenceCheckPlan, Long> {

    List<PresenceCheckPlan> findByEmployeeEmployeeIdAndAttendanceDateOrderByPlannedAtAsc(
            Long employeeId,
            LocalDate attendanceDate
    );

    List<PresenceCheckPlan> findByAttendanceDateAndStatusOrderByPlannedAtAsc(
            LocalDate attendanceDate,
            PresenceCheckPlanStatus status
    );

    List<PresenceCheckPlan> findByStatusAndPlannedAtBeforeOrderByPlannedAtAsc(
            PresenceCheckPlanStatus status,
            LocalDateTime dateTime
    );

    List<PresenceCheckPlan> findByStatusInAndPlannedAtBeforeOrderByPlannedAtAsc(
            Collection<PresenceCheckPlanStatus> statuses,
            LocalDateTime dateTime
    );

    boolean existsByEmployeeEmployeeIdAndAttendanceDateAndStatus(
            Long employeeId,
            LocalDate attendanceDate,
            PresenceCheckPlanStatus status
    );

    boolean existsByEmployeeEmployeeIdAndAttendanceDateAndStatusIn(
            Long employeeId,
            LocalDate attendanceDate,
            Collection<PresenceCheckPlanStatus> statuses
    );

    Optional<PresenceCheckPlan> findFirstByEmployeeEmployeeIdAndAttendanceDateAndStatusOrderByPlannedAtAsc(
            Long employeeId,
            LocalDate attendanceDate,
            PresenceCheckPlanStatus status
    );

    long countByEmployeeEmployeeIdAndAttendanceDateAndStatusIn(
            Long employeeId,
            LocalDate attendanceDate,
            Collection<PresenceCheckPlanStatus> statuses
    );
}