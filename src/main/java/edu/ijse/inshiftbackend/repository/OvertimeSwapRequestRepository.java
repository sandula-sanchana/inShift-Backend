package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.OvertimeAssignment;
import edu.ijse.inshiftbackend.entity.OvertimeSwapRequest;
import edu.ijse.inshiftbackend.entity.enums.OvertimeSwapStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OvertimeSwapRequestRepository extends JpaRepository<OvertimeSwapRequest, Long> {

    List<OvertimeSwapRequest> findByToEmployeeAndStatusOrderByCreatedAtDesc(Employee toEmployee, OvertimeSwapStatus status);

    Optional<OvertimeSwapRequest> findTopByOvertimeAssignmentAndStatusOrderByCreatedAtDesc(
            OvertimeAssignment overtimeAssignment,
            OvertimeSwapStatus status
    );
}