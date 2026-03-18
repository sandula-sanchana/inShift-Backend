package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDeviceToken;
import edu.ijse.inshiftbackend.entity.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeDeviceFCMTokenRepository extends JpaRepository<EmployeeDeviceToken, Long> {

    Optional<EmployeeDeviceToken> findByFcmToken(String fcmToken);

    List<EmployeeDeviceToken> findAllByEmployeeAndActiveTrue(Employee employee);

    List<EmployeeDeviceToken> findAllByEmployeeAndDeviceTypeAndActiveTrue(Employee employee, DeviceType deviceType);

    Optional<EmployeeDeviceToken> findByEmployeeAndDeviceTypeAndDeviceNameAndUserAgent(
            Employee employee,
            DeviceType deviceType,
            String deviceName,
            String userAgent
    );
}