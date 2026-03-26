package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.response.AdminEmployeeDeviceResponseDTO;
import edu.ijse.inshiftbackend.entity.DeviceEnrollmentRequest;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDevice;
import edu.ijse.inshiftbackend.entity.enums.DeviceApprovalStatus;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.DeviceEnrollmentRequestRepository;
import edu.ijse.inshiftbackend.repository.EmployeeDeviceRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.AdminDeviceManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDeviceManagementServiceImpl implements AdminDeviceManagementService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeDeviceRepository employeeDeviceRepository;
    private final DeviceEnrollmentRequestRepository deviceEnrollmentRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminEmployeeDeviceResponseDTO> getEmployeeDevices(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        List<EmployeeDevice> devices = employeeDeviceRepository.findByEmployeeOrderByCreatedAtDesc(employee);

        return devices.stream()
                .map(device -> {
                    DeviceEnrollmentRequest latestRequest = deviceEnrollmentRequestRepository
                            .findTopByEmployeeDeviceOrderByCreatedAtDesc(device)
                            .orElse(null);

                    return AdminEmployeeDeviceResponseDTO.builder()
                            .id(device.getId())
                            .employeeId(employee.getEmployeeId())
                            .requestId(latestRequest != null ? latestRequest.getId() : null)
                            .deviceName(device.getDeviceName())
                            .deviceFingerprint(device.getDeviceFingerprint())
                            .requestedTrustType(
                                    device.getRequestedTrustType() != null
                                            ? device.getRequestedTrustType().name()
                                            : null
                            )
                            .approvedTrustType(
                                    device.getApprovedTrustType() != null
                                            ? device.getApprovedTrustType().name()
                                            : null
                            )
                            .approvalStatus(
                                    device.getApprovalStatus() != null
                                            ? device.getApprovalStatus().name()
                                            : null
                            )
                            .userAgent(device.getUserAgent())
                            .createdAt(device.getCreatedAt())
                            .reviewedAt(device.getReviewedAt())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public void revokeDevice(Long deviceId, String reason) {
        EmployeeDevice device = employeeDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        if (device.getApprovalStatus() == DeviceApprovalStatus.REVOKED) {
            throw new BadRequestException("Device is already revoked");
        }

        LocalDateTime now = LocalDateTime.now();

        device.setApprovalStatus(DeviceApprovalStatus.REVOKED);
        device.setActive(false);
        device.setReviewedAt(now);

        // Keep approved trust type/history for audit visibility.
        // Do NOT mass-revoke all employee passkeys here.
        employeeDeviceRepository.save(device);
    }

    @Override
    @Transactional
    public void restoreDevice(Long deviceId, String reason) {
        EmployeeDevice device = employeeDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        if (device.getApprovalStatus() != DeviceApprovalStatus.REVOKED) {
            throw new BadRequestException("Only revoked devices can be restored");
        }

        if (device.getRequestedTrustType() == null) {
            throw new BadRequestException("Requested trust type is missing for this device");
        }

        LocalDateTime now = LocalDateTime.now();

        device.setApprovalStatus(DeviceApprovalStatus.APPROVED);
        device.setApprovedTrustType(device.getRequestedTrustType());
        device.setActive(true);
        device.setReviewedAt(now);

        employeeDeviceRepository.save(device);
    }
}