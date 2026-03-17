package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.DeviceEnrollmentRequest;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.enums.DeviceEnrollmentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeviceEnrollmentRequestRepository extends JpaRepository<DeviceEnrollmentRequest, Long> {

    boolean existsByEmployeeAndStatusIn(Employee employee, List<DeviceEnrollmentRequestStatus> statuses);

    Optional<DeviceEnrollmentRequest> findTopByEmployeeAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Employee employee,
            DeviceEnrollmentRequestStatus status,
            LocalDateTime now
    );

    List<DeviceEnrollmentRequest> findByStatusOrderByCreatedAtDesc(DeviceEnrollmentRequestStatus status);

    List<DeviceEnrollmentRequest> findByEmployeeOrderByCreatedAtDesc(Employee employee);
}