package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.EmpPresenceCheckRespondDTO;
import edu.ijse.inshiftbackend.dto.PresenceCheckCreateDTO;
import edu.ijse.inshiftbackend.dto.response.PresenceCheckResponseDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDevice;
import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.PresenceCheckBiometricProof;
import edu.ijse.inshiftbackend.entity.enums.DeviceApprovalStatus;
import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckResponseSource;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckRiskLevel;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckSourceExpected;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckStatus;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeDeviceRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.PresenceCheckBiometricProofRepository;
import edu.ijse.inshiftbackend.repository.PresenceCheckRepository;
import edu.ijse.inshiftbackend.service.PresenceCheckService;
import edu.ijse.inshiftbackend.service.PresenceCheckTriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PresenceCheckServiceImpl implements PresenceCheckService {

    private static final Set<PresenceCheckStatus> ACTIVE_CHECK_STATUSES = Set.of(
            PresenceCheckStatus.PENDING
    );

    private final PresenceCheckRepository presenceCheckRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeDeviceRepository employeeDeviceRepository;
    private final PresenceCheckBiometricProofRepository presenceCheckBiometricProofRepository;
    private final PresenceCheckTriggerService presenceCheckTriggerService;

    @Override
    @Transactional
    public PresenceCheckResponseDTO createPresenceCheck(PresenceCheckCreateDTO dto, String adminEmail) {
        if (dto == null) {
            throw new BadRequestException("Presence check request is required");
        }

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (dto.getTriggerReason() == null) {
            throw new BadRequestException("Trigger reason is required");
        }

        PresenceCheck created = presenceCheckTriggerService.triggerPresenceCheck(
                employee,
                dto.getTriggerReason(),
                dto.getTriggerDescription()
        );

        if (created == null) {
            throw new BadRequestException("Unable to create presence check");
        }

        if (dto.getAdminNote() != null && !dto.getAdminNote().isBlank()) {
            created.setAdminNote(dto.getAdminNote());
        }

        if (dto.getSourceExpected() != null && dto.getSourceExpected() != PresenceCheckSourceExpected.ANY) {
            created.setSourceExpected(dto.getSourceExpected());
        }

        created = presenceCheckRepository.save(created);
        return mapToDTO(created);
    }

    @Override
    public PresenceCheckResponseDTO getCurrentPendingForEmployee(String employeeEmail) {
        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        PresenceCheck presenceCheck = presenceCheckRepository
                .findFirstByEmployeeEmployeeIdAndStatusInOrderByCreatedAtDesc(
                        employee.getEmployeeId(),
                        ACTIVE_CHECK_STATUSES
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

        if (dto.getResponseSource() == PresenceCheckResponseSource.MOBILE_GPS) {
            validateBiometricProofForMobile(presenceCheck, employee, dto);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime delayStartPoint =
                presenceCheck.getNotifiedAt() != null
                        ? presenceCheck.getNotifiedAt()
                        : presenceCheck.getCreatedAt();

        int delaySeconds = (int) Duration.between(delayStartPoint, now).getSeconds();
        boolean late = presenceCheck.getDueAt() != null && now.isAfter(presenceCheck.getDueAt());

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

        boolean shouldEscalate = late && presenceCheck.getRiskLevel() == PresenceCheckRiskLevel.HIGH;
        presenceCheck.setEscalated(shouldEscalate);
        presenceCheck.setEscalatedAt(shouldEscalate ? now : null);
        presenceCheck.setEscalationLevel(shouldEscalate ? 1 : 0);

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
                // either approved mobile or approved company PC is allowed
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

    private void validateBiometricProofForMobile(
            PresenceCheck presenceCheck,
            Employee employee,
            EmpPresenceCheckRespondDTO dto
    ) {
        if (dto.getBiometricProofToken() == null || dto.getBiometricProofToken().isBlank()) {
            throw new BadRequestException("Biometric proof is required for mobile presence confirmation");
        }

        PresenceCheckBiometricProof proof = presenceCheckBiometricProofRepository
                .findByProofTokenAndUsedFalseAndExpiresAtAfter(
                        dto.getBiometricProofToken(),
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new BadRequestException("Valid biometric proof is required"));

        if (!proof.getEmployee().getEmployeeId().equals(employee.getEmployeeId())) {
            throw new BadRequestException("Biometric proof does not belong to this employee");
        }

        if (!proof.getPresenceCheck().getId().equals(presenceCheck.getId())) {
            throw new BadRequestException("Biometric proof does not belong to this presence check");
        }

        if (!proof.getDeviceFingerprint().equals(dto.getDeviceFingerprint())) {
            throw new BadRequestException("Biometric proof does not match this device");
        }

        proof.setUsed(true);
        proof.setUsedAt(LocalDateTime.now());
        presenceCheckBiometricProofRepository.save(proof);
    }

    @Override
    public List<PresenceCheckResponseDTO> getActivePresenceChecks() {
        return presenceCheckRepository.findByStatusInOrderByCreatedAtDesc(ACTIVE_CHECK_STATUSES)
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

    @Override
    public PresenceCheckResponseDTO getPresenceCheckByIdForEmployee(Long presenceCheckId, String employeeEmail) {
        if (presenceCheckId == null) {
            throw new BadRequestException("Presence check id is required");
        }

        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        PresenceCheck presenceCheck = presenceCheckRepository.findById(presenceCheckId)
                .orElseThrow(() -> new ResourceNotFoundException("Presence check not found"));

        if (!presenceCheck.getEmployee().getEmployeeId().equals(employee.getEmployeeId())) {
            throw new BadRequestException("You cannot access another employee's presence check");
        }

        return mapToDTO(presenceCheck);
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