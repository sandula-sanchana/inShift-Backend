package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.DeviceEnrollRequestDTO;
import edu.ijse.inshiftbackend.dto.response.DeviceEnrollResponseDTO;
import edu.ijse.inshiftbackend.entity.DeviceEnrollmentRequest;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDevice;
import edu.ijse.inshiftbackend.entity.enums.DeviceApprovalStatus;
import edu.ijse.inshiftbackend.entity.enums.DeviceEnrollmentRequestStatus;
import edu.ijse.inshiftbackend.entity.enums.DeviceEnrollmentRequestType;
import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.DeviceEnrollmentRequestRepository;
import edu.ijse.inshiftbackend.repository.EmployeeDeviceRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.EmployeeDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeDeviceServiceImpl implements EmployeeDeviceService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeDeviceRepository employeeDeviceRepository;
    private final DeviceEnrollmentRequestRepository deviceEnrollmentRequestRepository;

    @Override
    @Transactional
    public DeviceEnrollResponseDTO enrollMyDevice(DeviceEnrollRequestDTO dto, String employeeEmail) {

        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        EmployeeDevice existingDevice = employeeDeviceRepository
                .findByEmployeeAndDeviceFingerprintAndActiveTrue(employee, dto.getDeviceFingerprint())
                .orElse(null);

        if (existingDevice != null) {
            existingDevice.setDeviceName(dto.getDeviceName());
            existingDevice.setUserAgent(dto.getUserAgent());
            existingDevice.setRequestedTrustType(dto.getRequestedTrustType());

            EmployeeDevice saved = employeeDeviceRepository.save(existingDevice);

            return DeviceEnrollResponseDTO.builder()
                    .deviceId(saved.getId())
                    .deviceFingerprint(saved.getDeviceFingerprint())
                    .approvalStatus(saved.getApprovalStatus())
                    .approvedTrustType(saved.getApprovedTrustType())
                    .message("Device already enrolled")
                    .build();
        }

        boolean hasApprovedMobile = employeeDeviceRepository
                .existsByEmployeeAndApprovalStatusAndApprovedTrustTypeAndActiveTrue(
                        employee,
                        DeviceApprovalStatus.APPROVED,
                        DeviceTrustType.MOBILE
                );

        boolean hasApprovedCompanyPc = employeeDeviceRepository
                .existsByEmployeeAndApprovalStatusAndApprovedTrustTypeAndActiveTrue(
                        employee,
                        DeviceApprovalStatus.APPROVED,
                        DeviceTrustType.COMPANY_PC
                );

        boolean hasAnyApprovedDevice = hasApprovedMobile || hasApprovedCompanyPc;

        DeviceApprovalStatus approvalStatus;
        DeviceTrustType approvedTrustType;
        String message;

        if (!hasAnyApprovedDevice && dto.getRequestedTrustType() == DeviceTrustType.MOBILE) {
            approvalStatus = DeviceApprovalStatus.APPROVED;
            approvedTrustType = DeviceTrustType.MOBILE;
            message = "First mobile device auto-approved";
        } else {
            approvalStatus = DeviceApprovalStatus.PENDING;
            approvedTrustType = null;
            message = "Device enrolled and pending admin approval";
        }

        LocalDateTime now = LocalDateTime.now();

        EmployeeDevice device = EmployeeDevice.builder()
                .employee(employee)
                .deviceFingerprint(dto.getDeviceFingerprint())
                .deviceName(dto.getDeviceName())
                .userAgent(dto.getUserAgent())
                .requestedTrustType(dto.getRequestedTrustType())
                .approvedTrustType(approvedTrustType)
                .approvalStatus(approvalStatus)
                .active(true)
                .createdAt(now)
                .reviewedAt(approvalStatus == DeviceApprovalStatus.APPROVED ? now : null)
                .build();

        EmployeeDevice saved = employeeDeviceRepository.save(device);

        if (approvalStatus == DeviceApprovalStatus.PENDING) {
            boolean alreadyPendingRequest = deviceEnrollmentRequestRepository.existsByEmployeeAndStatusIn(
                    employee,
                    List.of(DeviceEnrollmentRequestStatus.PENDING, DeviceEnrollmentRequestStatus.APPROVED)
            );

            if (!alreadyPendingRequest) {
                DeviceEnrollmentRequest request = DeviceEnrollmentRequest.builder()
                        .employee(employee)
                        .employeeDevice(saved)
                        .requestType(DeviceEnrollmentRequestType.NEW_DEVICE_REPLACEMENT)
                        .status(DeviceEnrollmentRequestStatus.PENDING)
                        .requestedTrustType(dto.getRequestedTrustType())
                        .approvedTrustType(null)
                        .requestedDeviceName(dto.getDeviceName())
                        .requestedUserAgent(dto.getUserAgent())
                        .requestReason("New device enrolled and waiting for admin approval")
                        .riskScoreImpact(20)
                        .createdAt(now)
                        .expiresAt(now.plusHours(24))
                        .completedAt(null)
                        .build();

                deviceEnrollmentRequestRepository.save(request);
            }
        }

        return DeviceEnrollResponseDTO.builder()
                .deviceId(saved.getId())
                .deviceFingerprint(saved.getDeviceFingerprint())
                .approvalStatus(saved.getApprovalStatus())
                .approvedTrustType(saved.getApprovedTrustType())
                .message(message)
                .build();
    }
}