package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDevice;
import edu.ijse.inshiftbackend.entity.enums.DeviceApprovalStatus;
import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
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
        if (employee == null) {
            throw new BadRequestException("Employee is required");
        }

        if (deviceFingerprint == null || deviceFingerprint.isBlank()) {
            throw new BadRequestException("Device fingerprint is required");
        }

        return employeeDeviceRepository
                .findByEmployeeAndDeviceFingerprintAndApprovalStatusAndActiveTrue(
                        employee,
                        deviceFingerprint,
                        DeviceApprovalStatus.APPROVED
                )
                .orElseThrow(() ->
                        new BadRequestException("This device is not approved for trusted operations"));
    }

    @Override
    public boolean hasApprovedCompanyPc(Employee employee) {
        if (employee == null) {
            return false;
        }

        return employeeDeviceRepository.existsByEmployeeAndApprovalStatusAndApprovedTrustTypeAndActiveTrue(
                employee,
                DeviceApprovalStatus.APPROVED,
                DeviceTrustType.COMPANY_PC
        );
    }

    @Override
    public boolean hasApprovedMobile(Employee employee) {
        if (employee == null) {
            return false;
        }

        return employeeDeviceRepository.existsByEmployeeAndApprovalStatusAndApprovedTrustTypeAndActiveTrue(
                employee,
                DeviceApprovalStatus.APPROVED,
                DeviceTrustType.MOBILE
        );
    }
}