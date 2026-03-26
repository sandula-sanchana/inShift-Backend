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
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeDeviceServiceImpl implements EmployeeDeviceService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeDeviceRepository employeeDeviceRepository;
    private final DeviceEnrollmentRequestRepository deviceEnrollmentRequestRepository;

    @Override
    @Transactional
    public DeviceEnrollResponseDTO enrollMyDevice(DeviceEnrollRequestDTO dto, String employeeEmail) {
        validateEnrollRequest(dto);

        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        LocalDateTime now = LocalDateTime.now();

        EmployeeDevice existingDevice = employeeDeviceRepository
                .findByEmployeeAndDeviceFingerprintAndActiveTrue(employee, dto.getDeviceFingerprint())
                .orElse(null);

        if (existingDevice != null) {
            return handleExistingDevice(employee, existingDevice, dto, now);
        }

        return handleNewDevice(employee, dto, now);
    }

    private DeviceEnrollResponseDTO handleExistingDevice(
            Employee employee,
            EmployeeDevice existingDevice,
            DeviceEnrollRequestDTO dto,
            LocalDateTime now
    ) {
        existingDevice.setDeviceName(dto.getDeviceName());
        existingDevice.setUserAgent(dto.getUserAgent());
        existingDevice.setRequestedTrustType(dto.getRequestedTrustType());

        EmployeeDevice saved = employeeDeviceRepository.save(existingDevice);

        // If the device is pending, make sure exactly one pending request exists.
        if (saved.getApprovalStatus() == DeviceApprovalStatus.PENDING) {
            ensurePendingRequestExists(employee, saved, dto, now);
        }

        String message = switch (saved.getApprovalStatus()) {
            case APPROVED -> "Device already enrolled and approved";
            case PENDING -> "Device already enrolled and pending admin approval";
            case REJECTED -> "Device was previously rejected. Please contact admin";
            case REVOKED -> "Device access has been revoked. Please contact admin";
        };

        return buildResponse(saved, message);
    }

    private DeviceEnrollResponseDTO handleNewDevice(
            Employee employee,
            DeviceEnrollRequestDTO dto,
            LocalDateTime now
    ) {
        boolean hasApprovedMobile = hasApprovedDevice(employee, DeviceTrustType.MOBILE);

        //MY BUSINESS RULE:
        // Only the first MOBILE device can be auto-approved.
        // COMPANY_PC must always go through admin approval.
        boolean shouldAutoApproveFirstMobile =
                dto.getRequestedTrustType() == DeviceTrustType.MOBILE && !hasApprovedMobile;

        DeviceApprovalStatus approvalStatus = shouldAutoApproveFirstMobile
                ? DeviceApprovalStatus.APPROVED
                : DeviceApprovalStatus.PENDING;

        DeviceTrustType approvedTrustType = shouldAutoApproveFirstMobile
                ? DeviceTrustType.MOBILE
                : null;

        String message = shouldAutoApproveFirstMobile
                ? "First mobile device auto-approved"
                : "Device enrolled and pending admin approval";

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

        if (saved.getApprovalStatus() == DeviceApprovalStatus.PENDING) {
            ensurePendingRequestExists(employee, saved, dto, now);
        }

        return buildResponse(saved, message);
    }

    private void ensurePendingRequestExists(
            Employee employee,
            EmployeeDevice device,
            DeviceEnrollRequestDTO dto,
            LocalDateTime now
    ) {
        expireOldRequestsIfNeeded(employee, now);


        // Only check for an existing PENDING request for the same device.
        Optional<DeviceEnrollmentRequest> sameDevicePendingRequest =
                deviceEnrollmentRequestRepository
                        .findTopByEmployeeDeviceAndStatusInOrderByCreatedAtDesc(
                                device,
                                List.of(DeviceEnrollmentRequestStatus.PENDING)
                        );

        if (sameDevicePendingRequest.isPresent()) {
            return;
        }

        // Allow only one employee-level pending request at a time.
        boolean employeeAlreadyHasPendingRequest =
                deviceEnrollmentRequestRepository.existsByEmployeeAndStatusIn(
                        employee,
                        List.of(DeviceEnrollmentRequestStatus.PENDING)
                );

        if (employeeAlreadyHasPendingRequest) {
            throw new BadRequestException(
                    "You already have another device enrollment request pending admin approval"
            );
        }

        DeviceEnrollmentRequest request = DeviceEnrollmentRequest.builder()
                .employee(employee)
                .employeeDevice(device)
                .requestType(resolveRequestType(employee, dto.getRequestedTrustType()))
                .status(DeviceEnrollmentRequestStatus.PENDING)
                .requestedTrustType(dto.getRequestedTrustType())
                .approvedTrustType(null)
                .requestedDeviceName(dto.getDeviceName())
                .requestedUserAgent(dto.getUserAgent())
                .requestReason(buildRequestReason(employee, dto.getRequestedTrustType()))
                .riskScoreImpact(resolveRiskImpact(dto.getRequestedTrustType()))
                .createdAt(now)
                .expiresAt(now.plusHours(24))
                .completedAt(null)
                .build();

        deviceEnrollmentRequestRepository.save(request);
    }

    private void expireOldRequestsIfNeeded(Employee employee, LocalDateTime now) {
        List<DeviceEnrollmentRequest> requests =
                deviceEnrollmentRequestRepository.findByEmployeeOrderByCreatedAtDesc(employee);

        for (DeviceEnrollmentRequest req : requests) {
            if ((req.getStatus() == DeviceEnrollmentRequestStatus.PENDING
                    || req.getStatus() == DeviceEnrollmentRequestStatus.APPROVED)
                    && req.getExpiresAt() != null
                    && req.getExpiresAt().isBefore(now)) {

                req.setStatus(DeviceEnrollmentRequestStatus.EXPIRED);
                req.setCompletedAt(now);
                deviceEnrollmentRequestRepository.save(req);
            }
        }
    }

    private boolean hasApprovedDevice(Employee employee, DeviceTrustType trustType) {
        return employeeDeviceRepository.existsByEmployeeAndApprovalStatusAndApprovedTrustTypeAndActiveTrue(
                employee,
                DeviceApprovalStatus.APPROVED,
                trustType
        );
    }

    private DeviceEnrollmentRequestType resolveRequestType(Employee employee, DeviceTrustType requestedTrustType) {
        boolean hasApprovedSameTrustType = hasApprovedDevice(employee, requestedTrustType);

        if (!hasApprovedSameTrustType) {
            return DeviceEnrollmentRequestType.FIRST_ENROLLMENT;
        }

        return DeviceEnrollmentRequestType.NEW_DEVICE_REPLACEMENT;
    }

    private int resolveRiskImpact(DeviceTrustType requestedTrustType) {
        return requestedTrustType == DeviceTrustType.COMPANY_PC ? 15 : 20;
    }

    private String buildRequestReason(Employee employee, DeviceTrustType requestedTrustType) {
        boolean hasApprovedSameTrustType = hasApprovedDevice(employee, requestedTrustType);

        if (requestedTrustType == DeviceTrustType.COMPANY_PC) {
            return hasApprovedSameTrustType
                    ? "Replacement company PC enrolled and waiting for admin approval"
                    : "First company PC enrolled and waiting for admin approval";
        }

        return hasApprovedSameTrustType
                ? "Replacement mobile device enrolled and waiting for admin approval"
                : "Additional mobile device enrolled and waiting for admin approval";
    }

    private DeviceEnrollResponseDTO buildResponse(EmployeeDevice device, String message) {
        return DeviceEnrollResponseDTO.builder()
                .deviceId(device.getId())
                .deviceFingerprint(device.getDeviceFingerprint())
                .approvalStatus(device.getApprovalStatus())
                .approvedTrustType(device.getApprovedTrustType())
                .message(message)
                .build();
    }

    private void validateEnrollRequest(DeviceEnrollRequestDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Device enrollment request is required");
        }

        if (dto.getDeviceFingerprint() == null || dto.getDeviceFingerprint().isBlank()) {
            throw new BadRequestException("Device fingerprint is required");
        }

        if (dto.getDeviceName() == null || dto.getDeviceName().isBlank()) {
            throw new BadRequestException("Device name is required");
        }

        if (dto.getRequestedTrustType() == null) {
            throw new BadRequestException("Requested trust type is required");
        }
    }
}