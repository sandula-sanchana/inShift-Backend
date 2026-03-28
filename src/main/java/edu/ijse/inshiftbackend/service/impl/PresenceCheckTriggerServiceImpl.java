package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.PresenceCheckPlan;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckRiskLevel;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckSourceExpected;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckStatus;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckTriggerReason;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.repository.PresenceCheckRepository;
import edu.ijse.inshiftbackend.service.PresenceCheckTriggerService;
import edu.ijse.inshiftbackend.service.PresenceNotificationService;
import edu.ijse.inshiftbackend.service.TrustedDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PresenceCheckTriggerServiceImpl implements PresenceCheckTriggerService {

    private static final Set<PresenceCheckStatus> ACTIVE_CHECK_STATUSES = Set.of(
            PresenceCheckStatus.PENDING
    );

    private static final int NOTIFICATION_GRACE_SECONDS = 30;

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

        boolean hasActiveCheck = hasActiveCheck(employee);
        if (hasActiveCheck) {
            throw new BadRequestException("Employee already has an active presence check");
        }

        PresenceCheckRiskLevel riskLevel = calculateRiskLevel(reason);
        int responseMinutes = resolveResponseWindow(riskLevel);

        return createAndNotifyPresenceCheck(
                employee,
                reason,
                description,
                riskLevel,
                resolveExpectedSource(employee),
                responseMinutes
        );
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

        if (hasActiveCheck(plan.getEmployee())) {
            return null;
        }

        PresenceCheckSourceExpected expectedSource =
                plan.getSourceExpected() == PresenceCheckSourceExpected.ANY
                        ? resolveExpectedSource(plan.getEmployee())
                        : plan.getSourceExpected();

        return createAndNotifyPresenceCheck(
                plan.getEmployee(),
                plan.getTriggerReason(),
                plan.getDescription(),
                plan.getRiskLevel(),
                expectedSource,
                safeDueMinutes(plan.getDueInMinutes())
        );
    }

    private PresenceCheck createAndNotifyPresenceCheck(
            Employee employee,
            PresenceCheckTriggerReason reason,
            String description,
            PresenceCheckRiskLevel riskLevel,
            PresenceCheckSourceExpected expectedSource,
            int responseMinutes
    ) {
        LocalDateTime now = LocalDateTime.now();

        PresenceCheck check = PresenceCheck.builder()
                .employee(employee)
                .triggerReason(reason)
                .triggerDescription(description)
                .riskLevel(riskLevel)
                .status(PresenceCheckStatus.PENDING)
                .sourceExpected(expectedSource)
                .createdAt(now)
                .dueAt(now.plusMinutes(responseMinutes).plusSeconds(NOTIFICATION_GRACE_SECONDS))
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

        check = presenceCheckRepository.save(check);

        try {
            presenceNotificationService.sendPresenceCheckNotification(check);

            LocalDateTime notifiedAt = LocalDateTime.now();
            check.setNotifiedAt(notifiedAt);
            check.setDueAt(notifiedAt.plusMinutes(responseMinutes).plusSeconds(NOTIFICATION_GRACE_SECONDS));

        } catch (Exception e) {
            // Keep the check valid even if push fails.
            // Employee can still access it from app manually/by polling.
            LocalDateTime fallbackNoticeTime = LocalDateTime.now();
            check.setNotifiedAt(fallbackNoticeTime);
            check.setDueAt(fallbackNoticeTime.plusMinutes(responseMinutes).plusSeconds(NOTIFICATION_GRACE_SECONDS));

            System.err.println("[PresenceTrigger] Notification failed for check id "
                    + check.getId() + ": " + e.getMessage());
        }

        return presenceCheckRepository.save(check);
    }

    private boolean hasActiveCheck(Employee employee) {
        return presenceCheckRepository.existsByEmployeeEmployeeIdAndStatusIn(
                employee.getEmployeeId(),
                ACTIVE_CHECK_STATUSES
        );
    }

    private int safeDueMinutes(Integer dueInMinutes) {
        if (dueInMinutes == null || dueInMinutes <= 0) {
            return 5;
        }
        return Math.max(dueInMinutes, 5);
    }

    private PresenceCheckSourceExpected resolveExpectedSource(Employee employee) {
        boolean hasCompanyPc = trustedDeviceService.hasApprovedCompanyPc(employee);
        boolean hasMobile = trustedDeviceService.hasApprovedMobile(employee);

        if (hasCompanyPc && hasMobile) {
            return PresenceCheckSourceExpected.ANY;
        }

        if (hasCompanyPc) {
            return PresenceCheckSourceExpected.COMPANY_PC;
        }

        if (hasMobile) {
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
            case LOW -> 12;
            case MEDIUM -> 8;
            case HIGH -> 5;
        };
    }
}