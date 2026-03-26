package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDevice;
import edu.ijse.inshiftbackend.entity.enums.DeviceApprovalStatus;
import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeDeviceRepository extends JpaRepository<EmployeeDevice, Long> {

    Optional<EmployeeDevice> findByEmployeeAndDeviceFingerprintAndActiveTrue(Employee employee, String deviceFingerprint);

    Optional<EmployeeDevice> findByEmployeeAndDeviceFingerprintAndApprovalStatusAndActiveTrue(
            Employee employee,
            String deviceFingerprint,
            DeviceApprovalStatus approvalStatus
    );

    boolean existsByEmployeeAndApprovalStatusAndApprovedTrustTypeAndActiveTrue(
            Employee employee,
            DeviceApprovalStatus approvalStatus,
            DeviceTrustType approvedTrustType
    );

//    List<EmployeeDevice> findAllByEmployeeAndApprovalStatusAndApprovedTrustTypeAndActiveTrue(
//            Employee employee,
//            DeviceApprovalStatus approvalStatus,
//            DeviceTrustType approvedTrustType
//    );

    List<EmployeeDevice> findByEmployeeOrderByCreatedAtDesc(Employee employee);

    List<EmployeeDevice> findAllByApprovalStatusAndActiveTrue(DeviceApprovalStatus approvalStatus);
}