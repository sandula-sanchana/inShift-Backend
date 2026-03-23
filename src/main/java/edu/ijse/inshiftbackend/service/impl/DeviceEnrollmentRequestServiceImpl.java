package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.response.AdminDeviceEnrollmentRequestResponseDTO;
import edu.ijse.inshiftbackend.entity.DeviceEnrollmentRequest;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDevice;
import edu.ijse.inshiftbackend.entity.PasskeyCredential;
import edu.ijse.inshiftbackend.entity.enums.DeviceApprovalStatus;
import edu.ijse.inshiftbackend.entity.enums.DeviceEnrollmentRequestStatus;
import edu.ijse.inshiftbackend.entity.enums.DeviceEnrollmentRequestType;
import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.DeviceEnrollmentRequestRepository;
import edu.ijse.inshiftbackend.repository.EmployeeDeviceRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.PasskeyCredentialRepository;
import edu.ijse.inshiftbackend.service.DeviceEnrollmentRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceEnrollmentRequestServiceImpl implements DeviceEnrollmentRequestService {

    private final DeviceEnrollmentRequestRepository requestRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeDeviceRepository employeeDeviceRepository;

    @Override
    @Transactional
    public DeviceEnrollmentRequest createPendingReplacementRequest(
            Employee employee,
            String deviceFingerprint,
            String deviceName,
            String userAgent,
            String ipAddress,
            DeviceTrustType requestedTrustType
    ) {
        if (employee == null) {
            throw new BadRequestException("Employee is required");
        }

        if (deviceFingerprint == null || deviceFingerprint.isBlank()) {
            throw new BadRequestException("Device fingerprint is required");
        }

        if (deviceName == null || deviceName.isBlank()) {
            throw new BadRequestException("Device name is required");
        }

        if (requestedTrustType == null) {
            throw new BadRequestException("Requested trust type is required");
        }

        expireOldPendingRequests(employee);

        boolean alreadyPending = requestRepository.existsByEmployeeAndStatusIn(
                employee,
                List.of(DeviceEnrollmentRequestStatus.PENDING, DeviceEnrollmentRequestStatus.APPROVED)
        );

        if (alreadyPending) {
            throw new BadRequestException("A device enrollment request is already pending or approved");
        }

        EmployeeDevice employeeDevice = employeeDeviceRepository
                .findByEmployeeAndDeviceFingerprintAndActiveTrue(employee, deviceFingerprint)
                .orElseThrow(() ->
                        new BadRequestException("Device must be enrolled before requesting approval"));

        PasskeyCredential existingCredential = passkeyCredentialRepository
                .findTopByEmployeeAndActiveTrueOrderByCreatedAtDesc(employee)
                .orElse(null);

        DeviceEnrollmentRequest request = DeviceEnrollmentRequest.builder()
                .employee(employee)
                .employeeDevice(employeeDevice)
                .requestType(DeviceEnrollmentRequestType.NEW_DEVICE_REPLACEMENT)
                .status(DeviceEnrollmentRequestStatus.PENDING)
                .requestedTrustType(requestedTrustType)
                .approvedTrustType(null)
                .requestedDeviceName(deviceName)
                .requestedUserAgent(userAgent)
                .requestedIpAddress(ipAddress)
                .requestReason("New device requested while an active trusted device/passkey already exists")
                .riskScoreImpact(35)
                .existingCredentialToReplace(existingCredential)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .completedAt(null)
                .build();

        return requestRepository.save(request);
    }

    @Override
    public DeviceEnrollmentRequest getApprovedValidRequest(Employee employee) {
        if (employee == null) {
            throw new BadRequestException("Employee is required");
        }

        return requestRepository
                .findTopByEmployeeAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        employee,
                        DeviceEnrollmentRequestStatus.APPROVED,
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new BadRequestException("No approved enrollment request found"));
    }

    @Override
    @Transactional
    public void expireOldPendingRequests(Employee employee) {
        if (employee == null) {
            return;
        }

        List<DeviceEnrollmentRequest> requests = requestRepository.findByEmployeeOrderByCreatedAtDesc(employee);
        LocalDateTime now = LocalDateTime.now();

        for (DeviceEnrollmentRequest req : requests) {
            if ((req.getStatus() == DeviceEnrollmentRequestStatus.PENDING ||
                    req.getStatus() == DeviceEnrollmentRequestStatus.APPROVED) &&
                    req.getExpiresAt() != null &&
                    req.getExpiresAt().isBefore(now)) {

                req.setStatus(DeviceEnrollmentRequestStatus.EXPIRED);
                req.setCompletedAt(now);

                EmployeeDevice device = req.getEmployeeDevice();
                if (device != null && device.getApprovalStatus() == DeviceApprovalStatus.PENDING) {
                    device.setApprovalStatus(DeviceApprovalStatus.REJECTED);
                    device.setReviewedAt(now);
                    employeeDeviceRepository.save(device);
                }

                requestRepository.save(req);
            }
        }
    }

    @Override
    @Transactional
    public void approveRequest(Long requestId, DeviceTrustType approvedTrustType, String adminComment) {
        DeviceEnrollmentRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment request not found"));

        if (request.getStatus() != DeviceEnrollmentRequestStatus.PENDING) {
            throw new BadRequestException("Only pending requests can be approved");
        }

        if (approvedTrustType == null) {
            throw new BadRequestException("Approved trust type is required when approving a request");
        }

        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee admin = employeeRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        LocalDateTime now = LocalDateTime.now();

        request.setStatus(DeviceEnrollmentRequestStatus.APPROVED);
        request.setApprovedTrustType(approvedTrustType);
        request.setApprovedBy(admin);
        request.setApprovedAt(now);
        request.setAdminComment(adminComment);
        request.setCompletedAt(now);

        EmployeeDevice device = request.getEmployeeDevice();
        if (device != null) {
            device.setApprovalStatus(DeviceApprovalStatus.APPROVED);
            device.setApprovedTrustType(approvedTrustType);
            device.setReviewedAt(now);
            device.setActive(true);
            employeeDeviceRepository.save(device);
        }

        requestRepository.save(request);
    }

    @Override
    @Transactional
    public void rejectRequest(Long requestId, String adminComment) {
        DeviceEnrollmentRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment request not found"));

        if (request.getStatus() != DeviceEnrollmentRequestStatus.PENDING) {
            throw new BadRequestException("Only pending requests can be rejected");
        }

        LocalDateTime now = LocalDateTime.now();

        request.setStatus(DeviceEnrollmentRequestStatus.REJECTED);
        request.setAdminComment(adminComment);
        request.setCompletedAt(now);

        EmployeeDevice device = request.getEmployeeDevice();
        if (device != null) {
            device.setApprovalStatus(DeviceApprovalStatus.REJECTED);
            device.setReviewedAt(now);
            employeeDeviceRepository.save(device);
        }

        requestRepository.save(request);
    }

    @Override
    public List<AdminDeviceEnrollmentRequestResponseDTO> getPendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtDesc(DeviceEnrollmentRequestStatus.PENDING)
                .stream()
                .map(this::mapToAdminDTO)
                .toList();
    }

    private AdminDeviceEnrollmentRequestResponseDTO mapToAdminDTO(DeviceEnrollmentRequest req) {
        return AdminDeviceEnrollmentRequestResponseDTO.builder()
                .id(req.getId())
                .employeeName(req.getEmployee().getFullName())
                .employeeId(req.getEmployee().getEmployeeId())
                .status(req.getStatus().name())
                .requestType(req.getRequestType().name())
                .employeeDeviceId(req.getEmployeeDevice() != null ? req.getEmployeeDevice().getId() : null)
                .deviceFingerprint(
                        req.getEmployeeDevice() != null ? req.getEmployeeDevice().getDeviceFingerprint() : null
                )
                .requestedTrustType(
                        req.getRequestedTrustType() != null ? req.getRequestedTrustType().name() : null
                )
                .approvedTrustType(
                        req.getApprovedTrustType() != null ? req.getApprovedTrustType().name() : null
                )
                .requestedDeviceName(req.getRequestedDeviceName())
                .requestedUserAgent(req.getRequestedUserAgent())
                .requestedPlatform(req.getRequestedPlatform())
                .requestedBrowser(req.getRequestedBrowser())
                .requestedIpAddress(req.getRequestedIpAddress())
                .requestReason(req.getRequestReason())
                .riskScoreImpact(req.getRiskScoreImpact())
                .createdAt(req.getCreatedAt())
                .expiresAt(req.getExpiresAt())
                .approvedAt(req.getApprovedAt())
                .completedAt(req.getCompletedAt())
                .existingDeviceName(
                        req.getExistingCredentialToReplace() != null
                                ? req.getExistingCredentialToReplace().getDeviceName()
                                : null
                )
                .existingCredentialCreatedAt(
                        req.getExistingCredentialToReplace() != null
                                ? req.getExistingCredentialToReplace().getCreatedAt()
                                : null
                )
                .adminComment(req.getAdminComment())
                .build();
    }
}