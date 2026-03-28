package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.OvertimeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OvertimeAssignmentRepository extends JpaRepository<OvertimeAssignment, Long> {

    List<OvertimeAssignment> findByEmployeeOrderByAssignedAtDesc(Employee employee);

    List<OvertimeAssignment> findAllByOrderByAssignedAtDesc();
}