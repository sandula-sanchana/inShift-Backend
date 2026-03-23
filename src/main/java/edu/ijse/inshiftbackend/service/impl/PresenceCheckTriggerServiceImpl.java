package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.PresenceCheckPlan;
import edu.ijse.inshiftbackend.entity.enums.*;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.repository.PresenceCheckRepository;
import edu.ijse.inshiftbackend.service.PresenceCheckTriggerService;
import edu.ijse.inshiftbackend.service.PresenceNotificationService;
import edu.ijse.inshiftbackend.service.TrustedDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PresenceCheckTriggerServiceImpl implements PresenceCheckTriggerService {

    private final PresenceCheckRepository presenceCheckRepository;
    private final PresenceNotificationService presenceNotificationService;
    private final TrustedDeviceService trustedDeviceService;

    @Override
    @Transactional
    public PresenceCheck triggerPresenceCheck(
            Employee employee,
            PresenceCheckTriggerReason reason,
            String description
    ) {
        if (employee == null) {
            throw new BadRequestException("Employee is required");
        }

        if (reason == null) {
            throw new BadRequestException("Presence trigger reason is required");
        }

        ensureNoPendingPresenceCheck(employee);

        PresenceCheckRiskLevel riskLevel = calculateRiskLevel(reason);
        int responseMinutes = resolveResponseWindow(riskLevel);
        LocalDateTime now = LocalDateTime.now();

        PresenceCheck check = PresenceCheck.builder()
                .employee(employee)
                .triggerReason(reason)
                .triggerDescription(description)
                .riskLevel(riskLevel)
                .status(PresenceCheckStatus.PENDING)
                .sourceExpected(resolveExpectedSource(employee))
                .createdAt(now)
                .dueAt(now.plusMinutes(responseMinutes))
                .lateResponse(false)
                .missedResponse(false)
                .escalated(false)
                .escalationLevel(0)
                .build();

        check = presenceCheckRepository.save(check);

        try {
            presenceNotificationService.sendPresenceCheckNotification(check);
        } catch (Exception e) {
            System.err.println("Presence check triggered, but notification failed: " + e.getMessage());
        } finally {
            check.setNotifiedAt(LocalDateTime.now());
            check = presenceCheckRepository.save(check);
        }

        return check;
    }

    @Override
    @Transactional
    public PresenceCheck triggerFromPlan(PresenceCheckPlan plan) {
        if (plan == null) {
            throw new BadRequestException("Presence check plan is required");
        }

        if (plan.getEmployee() == null) {
            throw new BadRequestException("Plan employee is required");
        }

        ensureNoPendingPresenceCheck(plan.getEmployee());

        LocalDateTime now = LocalDateTime.now();

        PresenceCheck check = PresenceCheck.builder()
                .employee(plan.getEmployee())
                .triggerReason(plan.getTriggerReason())
                .triggerDescription(plan.getDescription())
                .riskLevel(plan.getRiskLevel())
                .status(PresenceCheckStatus.PENDING)
                .sourceExpected(
                        plan.getSourceExpected() == PresenceCheckSourceExpected.ANY
                                ? resolveExpectedSource(plan.getEmployee())
                                : plan.getSourceExpected()
                )
                .createdAt(now)
                .dueAt(now.plusMinutes(plan.getDueInMinutes()))
                .lateResponse(false)
                .missedResponse(false)
                .escalated(false)
                .escalationLevel(0)
                .build();

        check = presenceCheckRepository.save(check);

        try {
            presenceNotificationService.sendPresenceCheckNotification(check);
        } catch (Exception e) {
            System.err.println("Presence check triggered from plan, but notification failed: " + e.getMessage());
        } finally {
            check.setNotifiedAt(LocalDateTime.now());
            check = presenceCheckRepository.save(check);
        }

        return check;
    }

    private void ensureNoPendingPresenceCheck(Employee employee) {
        presenceCheckRepository
                .findFirstByEmployeeEmployeeIdAndStatusOrderByCreatedAtDesc(
                        employee.getEmployeeId(),
                        PresenceCheckStatus.PENDING
                )
                .ifPresent(existing -> {
                    throw new BadRequestException("Employee already has a pending presence check");
                });
    }

    private PresenceCheckSourceExpected resolveExpectedSource(Employee employee) {
        if (trustedDeviceService.hasApprovedCompanyPc(employee)) {
            return PresenceCheckSourceExpected.COMPANY_PC;
        }

        if (trustedDeviceService.hasApprovedMobile(employee)) {
            return PresenceCheckSourceExpected.MOBILE_BIOMETRIC;
        }

        return PresenceCheckSourceExpected.ANY;
    }

    private PresenceCheckRiskLevel calculateRiskLevel(PresenceCheckTriggerReason reason) {
        return switch (reason) {
            case RANDOM -> PresenceCheckRiskLevel.LOW;
            case LOCATION_ANOMALY, ADMIN_MANUAL, RULE_ENGINE -> PresenceCheckRiskLevel.MEDIUM;
            case RISK_PATTERN, DEVICE_ANOMALY -> PresenceCheckRiskLevel.HIGH;
        };
    }

    private int resolveResponseWindow(PresenceCheckRiskLevel level) {
        return switch (level) {
            case LOW -> 10;
            case MEDIUM -> 5;
            case HIGH -> 3;
        };
    }
}