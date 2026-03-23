package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDevice;
import edu.ijse.inshiftbackend.entity.enums.DeviceApprovalStatus;
import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;
import edu.ijse.inshiftbackend.repository.EmployeeDeviceRepository;
import edu.ijse.inshiftbackend.service.TrustedDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrustedDeviceServiceImpl implements TrustedDeviceService {

    private final EmployeeDeviceRepository employeeDeviceRepository;

    @Override
    public EmployeeDevice requireApprovedDevice(Employee employee, String deviceFingerprint) {
        return employeeDeviceRepository
                .findByEmployeeAndDeviceFingerprintAndApprovalStatusAndActiveTrue(
                        employee,
                        deviceFingerprint,
                        DeviceApprovalStatus.APPROVED
                )
                .orElseThrow(() -> new IllegalStateException("This device is not approved for presence verification"));
    }

    @Override
    public boolean hasApprovedCompanyPc(Employee employee) {
        return employeeDeviceRepository.existsByEmployeeAndApprovalStatusAndApprovedTrustTypeAndActiveTrue(
                employee,
                DeviceApprovalStatus.APPROVED,
                DeviceTrustType.COMPANY_PC
        );
    }

    @Override
    public boolean hasApprovedMobile(Employee employee) {
        return employeeDeviceRepository.existsByEmployeeAndApprovalStatusAndApprovedTrustTypeAndActiveTrue(
                employee,
                DeviceApprovalStatus.APPROVED,
                DeviceTrustType.MOBILE
        );
    }
}