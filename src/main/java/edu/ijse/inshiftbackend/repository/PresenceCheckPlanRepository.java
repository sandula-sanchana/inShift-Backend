package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.PresenceCheckPlan;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    boolean existsByEmployeeEmployeeIdAndAttendanceDateAndStatus(
            Long employeeId,
            LocalDate attendanceDate,
            PresenceCheckPlanStatus status
    );
}