package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmpCode(String empCode);
    Optional<Employee> findByEmail(String email);
    boolean existsByEmpCode(String empCode);
    boolean existsByEmail(String email);
    long countByActiveTrue();
    List<Employee> findAllByActiveTrue();

    Optional<Employee> findTopByOrderByEmployeeIdDesc();
}