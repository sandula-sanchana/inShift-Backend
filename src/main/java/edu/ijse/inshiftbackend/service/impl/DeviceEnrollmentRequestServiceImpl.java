package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.response.AdminDeviceEnrollmentRequestResponseDTO;
import edu.ijse.inshiftbackend.entity.DeviceEnrollmentRequest;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.PasskeyCredential;
import edu.ijse.inshiftbackend.entity.enums.DeviceEnrollmentRequestStatus;
import edu.ijse.inshiftbackend.entity.enums.DeviceEnrollmentRequestType;
import edu.ijse.inshiftbackend.repository.DeviceEnrollmentRequestRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.PasskeyCredentialRepository;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
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

    @Override
    @Transactional
    public DeviceEnrollmentRequest createPendingReplacementRequest(
            Employee employee,
            String deviceName,
            String userAgent,
            String ipAddress
    ) {
        expireOldPendingRequests(employee);

        boolean alreadyPending = requestRepository.existsByEmployeeAndStatusIn(
                employee,
                List.of(DeviceEnrollmentRequestStatus.PENDING, DeviceEnrollmentRequestStatus.APPROVED)
        );

        if (alreadyPending) {
            throw new BadRequestException("A device enrollment request is already pending or approved");
        }

        PasskeyCredential existingCredential = passkeyCredentialRepository
                .findTopByEmployeeAndActiveTrueOrderByCreatedAtDesc(employee)
                .orElse(null);

        DeviceEnrollmentRequest request = DeviceEnrollmentRequest.builder()
                .employee(employee)
                .requestType(DeviceEnrollmentRequestType.NEW_DEVICE_REPLACEMENT)
                .status(DeviceEnrollmentRequestStatus.PENDING)
                .requestedDeviceName(deviceName)
                .requestedUserAgent(userAgent)
                .requestedIpAddress(ipAddress)
                .requestReason("New device requested while active passkey already exists")
                .riskScoreImpact(35)
                .existingCredentialToReplace(existingCredential)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        return requestRepository.save(request);
    }

    @Override
    public DeviceEnrollmentRequest getApprovedValidRequest(Employee employee) {
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
        List<DeviceEnrollmentRequest> requests = requestRepository.findByEmployeeOrderByCreatedAtDesc(employee);

        LocalDateTime now = LocalDateTime.now();

        for (DeviceEnrollmentRequest req : requests) {
            if ((req.getStatus() == DeviceEnrollmentRequestStatus.PENDING ||
                    req.getStatus() == DeviceEnrollmentRequestStatus.APPROVED) &&
                    req.getExpiresAt() != null &&
                    req.getExpiresAt().isBefore(now)) {
                req.setStatus(DeviceEnrollmentRequestStatus.EXPIRED);
            }
        }
    }

    @Override
    @Transactional
    public void approveRequest(Long requestId, String adminComment) {
        DeviceEnrollmentRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment request not found"));

        if (request.getStatus() != DeviceEnrollmentRequestStatus.PENDING) {
            throw new BadRequestException("Only pending requests can be approved");
        }

        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee admin = employeeRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        request.setStatus(DeviceEnrollmentRequestStatus.APPROVED);
        request.setApprovedBy(admin);
        request.setApprovedAt(LocalDateTime.now());
        request.setAdminComment(adminComment);
    }

    @Override
    @Transactional
    public void rejectRequest(Long requestId, String adminComment) {
        DeviceEnrollmentRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment request not found"));

        if (request.getStatus() != DeviceEnrollmentRequestStatus.PENDING) {
            throw new BadRequestException("Only pending requests can be rejected");
        }

        request.setStatus(DeviceEnrollmentRequestStatus.REJECTED);
        request.setAdminComment(adminComment);
    }

    @Override
    public List<AdminDeviceEnrollmentRequestResponseDTO> getPendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtDesc(DeviceEnrollmentRequestStatus.PENDING)
                .stream()
                .map(req -> AdminDeviceEnrollmentRequestResponseDTO.builder()
                        .id(req.getId())
                        .employeeName(req.getEmployee().getFullName())
                        .employeeId(req.getEmployee().getEmployeeId())
                        .status(req.getStatus().name())
                        .requestType(req.getRequestType().name())
                        .requestedDeviceName(req.getRequestedDeviceName())
                        .requestedUserAgent(req.getRequestedUserAgent())
                        .riskScoreImpact(req.getRiskScoreImpact())
                        .createdAt(req.getCreatedAt())
                        .expiresAt(req.getExpiresAt())
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
                        .build())
                .toList();
    }
}