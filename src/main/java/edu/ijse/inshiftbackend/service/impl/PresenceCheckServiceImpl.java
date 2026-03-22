package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.PresenceCheckCreateDTO;
import edu.ijse.inshiftbackend.dto.EmpPresenceCheckRespondDTO;
import edu.ijse.inshiftbackend.dto.response.PresenceCheckResponseDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckRiskLevel;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckStatus;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckTriggerReason;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
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

    private static final int DEFAULT_DUE_SECONDS = 120;
    //this for admin manual presence checks
    @Override
    @Transactional
    public PresenceCheckResponseDTO createPresenceCheck(PresenceCheckCreateDTO dto, String adminEmail) {
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
                .build();

        PresenceCheck saved = presenceCheckRepository.save(presenceCheck);

        try {
            presenceNotificationService.sendPresenceCheckNotification(saved);
            saved.setNotifiedAt(LocalDateTime.now());
            saved = presenceCheckRepository.save(saved);
        } catch (Exception e) {
            System.err.println("Presence check created, but notification send failed: " + e.getMessage());
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

        LocalDateTime now = LocalDateTime.now();
        int delaySeconds = (int) Duration.between(presenceCheck.getCreatedAt(), now).getSeconds();
        boolean late = now.isAfter(presenceCheck.getDueAt());

        presenceCheck.setRespondedAt(now);
        presenceCheck.setResponseSource(dto.getResponseSource());
        presenceCheck.setResponseLatitude(dto.getLatitude());
        presenceCheck.setResponseLongitude(dto.getLongitude());
        presenceCheck.setResponseAccuracyMeters(dto.getAccuracyMeters());
        presenceCheck.setResponseLocationText(dto.getLocationText());
        presenceCheck.setResponseNote(dto.getResponseNote());
        presenceCheck.setResponseDelaySeconds(delaySeconds);
        presenceCheck.setLateResponse(late);
        presenceCheck.setMissedResponse(false);
        presenceCheck.setStatus(late ? PresenceCheckStatus.LATE : PresenceCheckStatus.RESPONDED);

        PresenceCheck saved = presenceCheckRepository.save(presenceCheck);

        return mapToDTO(saved);
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