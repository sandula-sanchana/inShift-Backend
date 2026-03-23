package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.PresenceCheckCreateDTO;
import edu.ijse.inshiftbackend.dto.EmpPresenceCheckRespondDTO;
import edu.ijse.inshiftbackend.dto.response.PresenceCheckResponseDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDevice;
import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.enums.*;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeDeviceRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.PresenceCheckRepository;
import edu.ijse.inshiftbackend.service.PresenceCheckService;
import edu.ijse.inshiftbackend.service.PresenceNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PresenceCheckServiceImpl implements PresenceCheckService {

    private final PresenceCheckRepository presenceCheckRepository;
    private final EmployeeRepository employeeRepository;
    private final PresenceNotificationService presenceNotificationService;
    private final EmployeeDeviceRepository employeeDeviceRepository;

    private static final int DEFAULT_DUE_SECONDS = 120;

    @Override
    @Transactional
    public PresenceCheckResponseDTO createPresenceCheck(PresenceCheckCreateDTO dto, String adminEmail) {
        if (dto == null) {
            throw new BadRequestException("Presence check request is required");
        }

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        presenceCheckRepository.findFirstByEmployeeEmployeeIdAndStatusOrderByCreatedAtDesc(
                employee.getEmployeeId(),
                PresenceCheckStatus.PENDING
        ).ifPresent(existing -> {
            throw new BadRequestException("Employee already has a pending presence check");
        });

        LocalDateTime now = LocalDateTime.now();
        int dueInSeconds = dto.getDueInSeconds() != null && dto.getDueInSeconds() > 0
                ? dto.getDueInSeconds()
                : DEFAULT_DUE_SECONDS;

        PresenceCheck presenceCheck = PresenceCheck.builder()
                .employee(employee)
                .triggerReason(dto.getTriggerReason())
                .riskLevel(resolveManualRiskLevel(dto.getTriggerReason()))
                .status(PresenceCheckStatus.PENDING)
                .sourceExpected(dto.getSourceExpected())
                .triggerDescription(dto.getTriggerDescription())
                .adminNote(dto.getAdminNote())
                .createdAt(now)
                .dueAt(now.plusSeconds(dueInSeconds))
                .notifiedAt(null)
                .respondedAt(null)
                .responseSource(null)
                .responseLatitude(null)
                .responseLongitude(null)
                .responseAccuracyMeters(null)
                .responseLocationText(null)
                .responseNote(null)
                .responseDelaySeconds(null)
                .lateResponse(false)
                .missedResponse(false)
                .escalated(false)
                .escalatedAt(null)
                .escalationLevel(0)
                .respondingDevice(null)
                .build();

        PresenceCheck saved = presenceCheckRepository.save(presenceCheck);

        try {
            presenceNotificationService.sendPresenceCheckNotification(saved);
        } catch (Exception e) {
            System.err.println("Presence check created, but notification send failed: " + e.getMessage());
        } finally {
            saved.setNotifiedAt(LocalDateTime.now());
            saved = presenceCheckRepository.save(saved);
        }

        return mapToDTO(saved);
    }

    private PresenceCheckRiskLevel resolveManualRiskLevel(PresenceCheckTriggerReason reason) {
        return switch (reason) {
            case RANDOM -> PresenceCheckRiskLevel.LOW;
            case LOCATION_ANOMALY, ADMIN_MANUAL, RULE_ENGINE -> PresenceCheckRiskLevel.MEDIUM;
            case RISK_PATTERN, DEVICE_ANOMALY -> PresenceCheckRiskLevel.HIGH;
        };
    }

    @Override
    public PresenceCheckResponseDTO getCurrentPendingForEmployee(String employeeEmail) {
        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        PresenceCheck presenceCheck = presenceCheckRepository
                .findFirstByEmployeeEmployeeIdAndStatusOrderByCreatedAtDesc(
                        employee.getEmployeeId(),
                        PresenceCheckStatus.PENDING
                )
                .orElseThrow(() -> new ResourceNotFoundException("No pending presence check found"));

        return mapToDTO(presenceCheck);
    }

    @Override
    @Transactional
    public PresenceCheckResponseDTO respondToPresenceCheck(EmpPresenceCheckRespondDTO dto, String employeeEmail) {
        if (dto == null) {
            throw new BadRequestException("Presence response is required");
        }

        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        PresenceCheck presenceCheck = presenceCheckRepository.findById(dto.getPresenceCheckId())
                .orElseThrow(() -> new ResourceNotFoundException("Presence check not found"));

        if (!presenceCheck.getEmployee().getEmployeeId().equals(employee.getEmployeeId())) {
            throw new BadRequestException("You cannot respond to another employee's presence check");
        }

        if (presenceCheck.getStatus() != PresenceCheckStatus.PENDING) {
            throw new BadRequestException("Presence check is not pending");
        }

        if (presenceCheck.getRespondedAt() != null) {
            throw new BadRequestException("Presence check has already been responded to");
        }

        EmployeeDevice device = employeeDeviceRepository
                .findByEmployeeAndDeviceFingerprintAndApprovalStatusAndActiveTrue(
                        employee,
                        dto.getDeviceFingerprint(),
                        DeviceApprovalStatus.APPROVED
                )
                .orElseThrow(() -> new BadRequestException("This device is not approved for presence verification"));

        validateResponseAgainstExpectedSource(presenceCheck, device);
        validateResponseByDeviceType(device, dto);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime delayStartPoint =
                presenceCheck.getNotifiedAt() != null
                        ? presenceCheck.getNotifiedAt()
                        : presenceCheck.getCreatedAt();

        int delaySeconds = (int) Duration.between(delayStartPoint, now).getSeconds();
        boolean late = now.isAfter(presenceCheck.getDueAt());

        presenceCheck.setRespondedAt(now);
        presenceCheck.setRespondingDevice(device);
        presenceCheck.setResponseSource(dto.getResponseSource());

        if (device.getApprovedTrustType() == DeviceTrustType.COMPANY_PC) {
            presenceCheck.setResponseLatitude(null);
            presenceCheck.setResponseLongitude(null);
            presenceCheck.setResponseAccuracyMeters(null);
            presenceCheck.setResponseLocationText(null);
        } else {
            presenceCheck.setResponseLatitude(dto.getLatitude());
            presenceCheck.setResponseLongitude(dto.getLongitude());
            presenceCheck.setResponseAccuracyMeters(dto.getAccuracyMeters());
            presenceCheck.setResponseLocationText(dto.getLocationText());
        }

        presenceCheck.setResponseNote(dto.getResponseNote());
        presenceCheck.setResponseDelaySeconds(delaySeconds);
        presenceCheck.setLateResponse(late);
        presenceCheck.setMissedResponse(false);
        presenceCheck.setStatus(late ? PresenceCheckStatus.LATE : PresenceCheckStatus.RESPONDED);

        if (late && presenceCheck.getRiskLevel() == PresenceCheckRiskLevel.HIGH) {
            presenceCheck.setEscalated(true);
            presenceCheck.setEscalatedAt(now);
            presenceCheck.setEscalationLevel(1);
        }

        PresenceCheck saved = presenceCheckRepository.save(presenceCheck);

        return mapToDTO(saved);
    }

    private void validateResponseAgainstExpectedSource(PresenceCheck presenceCheck, EmployeeDevice device) {
        if (presenceCheck.getSourceExpected() == null || device.getApprovedTrustType() == null) {
            throw new BadRequestException("Presence source validation failed");
        }

        switch (presenceCheck.getSourceExpected()) {
            case COMPANY_PC -> {
                if (device.getApprovedTrustType() != DeviceTrustType.COMPANY_PC) {
                    throw new BadRequestException("Presence must be confirmed from an approved company PC");
                }
            }
            case MOBILE_BIOMETRIC -> {
                if (device.getApprovedTrustType() != DeviceTrustType.MOBILE) {
                    throw new BadRequestException("Presence must be confirmed from an approved mobile device");
                }
            }
            case ANY -> {
                // allow either approved mobile or approved company PC
            }
            default -> throw new BadRequestException("Unsupported expected source");
        }
    }

    private void validateResponseByDeviceType(EmployeeDevice device, EmpPresenceCheckRespondDTO dto) {
        if (dto.getResponseSource() == null) {
            throw new BadRequestException("Response source is required");
        }

        if (dto.getResponseSource() == PresenceCheckResponseSource.MANUAL_REVIEW) {
            throw new BadRequestException("Manual review is not allowed for employee response");
        }

        if (device.getApprovedTrustType() == null) {
            throw new BadRequestException("Approved device type is missing");
        }

        switch (device.getApprovedTrustType()) {
            case COMPANY_PC -> {
                if (dto.getResponseSource() != PresenceCheckResponseSource.COMPANY_PC) {
                    throw new BadRequestException("Company PC must respond using COMPANY_PC source");
                }
            }
            case MOBILE -> {
                if (dto.getResponseSource() != PresenceCheckResponseSource.MOBILE_GPS) {
                    throw new BadRequestException("Mobile device must respond using MOBILE_GPS source");
                }
                if (dto.getLatitude() == null || dto.getLongitude() == null) {
                    throw new BadRequestException("Latitude and longitude are required for mobile response");
                }
            }
            default -> throw new BadRequestException("Unsupported device type");
        }
    }

    @Override
    public List<PresenceCheckResponseDTO> getActivePresenceChecks() {
        return presenceCheckRepository.findByStatusOrderByCreatedAtDesc(PresenceCheckStatus.PENDING)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public List<PresenceCheckResponseDTO> getPresenceCheckHistory() {
        return presenceCheckRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public List<PresenceCheckResponseDTO> getMyPresenceCheckHistory(String employeeEmail) {
        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        return presenceCheckRepository.findByEmployeeEmployeeIdOrderByCreatedAtDesc(employee.getEmployeeId())
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    private PresenceCheckResponseDTO mapToDTO(PresenceCheck pc) {
        return PresenceCheckResponseDTO.builder()
                .id(pc.getId())
                .employeeId(pc.getEmployee().getEmployeeId())
                .empCode(pc.getEmployee().getEmpCode())
                .employeeName(pc.getEmployee().getFullName())
                .branchName(pc.getEmployee().getBranch() != null ? pc.getEmployee().getBranch().getBranchName() : null)
                .triggerReason(pc.getTriggerReason())
                .riskLevel(pc.getRiskLevel())
                .status(pc.getStatus())
                .sourceExpected(pc.getSourceExpected())
                .triggerDescription(pc.getTriggerDescription())
                .adminNote(pc.getAdminNote())
                .createdAt(pc.getCreatedAt())
                .dueAt(pc.getDueAt())
                .notifiedAt(pc.getNotifiedAt())
                .respondedAt(pc.getRespondedAt())
                .responseSource(pc.getResponseSource())
                .respondingDeviceFingerprint(
                        pc.getRespondingDevice() != null ? pc.getRespondingDevice().getDeviceFingerprint() : null
                )
                .responseLatitude(pc.getResponseLatitude())
                .responseLongitude(pc.getResponseLongitude())
                .responseAccuracyMeters(pc.getResponseAccuracyMeters())
                .responseLocationText(pc.getResponseLocationText())
                .responseNote(pc.getResponseNote())
                .responseDelaySeconds(pc.getResponseDelaySeconds())
                .lateResponse(pc.getLateResponse())
                .missedResponse(pc.getMissedResponse())
                .escalated(pc.getEscalated())
                .escalatedAt(pc.getEscalatedAt())
                .escalationLevel(pc.getEscalationLevel())
                .build();
    }
}