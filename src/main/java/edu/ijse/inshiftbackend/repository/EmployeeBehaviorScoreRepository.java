package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.EmployeeBehaviorScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeBehaviorScoreRepository extends JpaRepository<EmployeeBehaviorScore, Long> {

    Optional<EmployeeBehaviorScore> findByEmployeeEmployeeId(Long employeeId);
}