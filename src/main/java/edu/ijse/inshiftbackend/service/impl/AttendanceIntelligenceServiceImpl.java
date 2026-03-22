package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.response.AttendanceFlagDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceIntelligenceOverviewDTO;
import edu.ijse.inshiftbackend.entity.AttendanceCorrectionRequest;
import edu.ijse.inshiftbackend.entity.AttendanceFlag;
import edu.ijse.inshiftbackend.entity.AttendanceRecord;
import edu.ijse.inshiftbackend.entity.AttendanceRiskScore;
import edu.ijse.inshiftbackend.entity.AttendanceRule;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeBehaviorScore;
import edu.ijse.inshiftbackend.entity.enums.AttendanceFlagType;
import edu.ijse.inshiftbackend.entity.enums.AttendanceRuleKey;
import edu.ijse.inshiftbackend.entity.enums.AttendanceSource;
import edu.ijse.inshiftbackend.entity.enums.AttendanceStatus;
import edu.ijse.inshiftbackend.entity.enums.AttendanceType;
import edu.ijse.inshiftbackend.entity.enums.RiskSeverity;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.AttendanceCorrectionRequestRepository;
import edu.ijse.inshiftbackend.repository.AttendanceFlagRepository;
import edu.ijse.inshiftbackend.repository.AttendanceRecordRepository;
import edu.ijse.inshiftbackend.repository.AttendanceRiskScoreRepository;
import edu.ijse.inshiftbackend.repository.EmployeeBehaviorScoreRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.AttendanceIntelligenceService;
import edu.ijse.inshiftbackend.service.AttendanceRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceIntelligenceServiceImpl implements AttendanceIntelligenceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceFlagRepository attendanceFlagRepository;
    private final AttendanceRiskScoreRepository attendanceRiskScoreRepository;
    private final AttendanceCorrectionRequestRepository correctionRequestRepository;
    private final EmployeeBehaviorScoreRepository employeeBehaviorScoreRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRuleService attendanceRuleService;

    @Override
    @Transactional
    public void evaluateDay(Long employeeId, LocalDate attendanceDate) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        LocalDateTime start = attendanceDate.atStartOfDay();
        LocalDateTime end = attendanceDate.plusDays(1).atStartOfDay();

        List<AttendanceRecord> dailyRecords =
                attendanceRecordRepository.findByEmployeeEmployeeIdAndEventTimeBetween(
                        employeeId,
                        start,
                        end
                );

        attendanceFlagRepository.deleteByEmployeeEmployeeIdAndAttendanceDate(
                employeeId,
                attendanceDate
        );

        evaluateShortWorkDuration(employee, attendanceDate, dailyRecords);
        evaluateInvalidOtEligibility(employee, attendanceDate, dailyRecords);
        evaluateTooManyCorrections(employee, attendanceDate);
        evaluateWebAttendanceDependency(employee, attendanceDate);

        recalculateDailyRiskScore(employee, attendanceDate);
        updateCurrentBehaviorScore(employee, attendanceDate);
    }

    @Override
    public List<AttendanceFlag> getFlagsForEmployeeDay(Long employeeId, LocalDate attendanceDate) {
        return attendanceFlagRepository.findByEmployeeEmployeeIdAndAttendanceDateOrderByDetectedAtDesc(
                employeeId,
                attendanceDate
        );
    }

    @Override
    public List<AttendanceIntelligenceOverviewDTO> getDailyOverview(LocalDate date) {
        List<AttendanceRiskScore> scores = attendanceRiskScoreRepository.findAllByAttendanceDate(date);

        return scores.stream()
                .map(score -> {
                    List<AttendanceFlagDTO> flags =
                            attendanceFlagRepository
                                    .findByEmployeeEmployeeIdAndAttendanceDateOrderByDetectedAtDesc(
                                            score.getEmployee().getEmployeeId(),
                                            date
                                    )
                                    .stream()
                                    .map(flag -> AttendanceFlagDTO.builder()
                                            .id(flag.getId())
                                            .flagType(flag.getFlagType() != null ? flag.getFlagType().name() : null)
                                            .severity(flag.getSeverity() != null ? flag.getSeverity().name() : null)
                                            .scoreImpact(flag.getScoreImpact())
                                            .message(flag.getMessage())
                                            .resolved(flag.getResolved())
                                            .build())
                                    .toList();

                    return AttendanceIntelligenceOverviewDTO.builder()
                            .employeeId(score.getEmployee().getEmployeeId())
                            .employeeName(score.getEmployee().getFullName())
                            .attendanceDate(score.getAttendanceDate())
                            .riskScore(score.getRiskScore())
                            .trustScore(score.getTrustScore())
                            .totalFlags(score.getTotalFlags())
                            .requiresReview(score.getRequiresReview())
                            .highRisk(score.getHighRisk())
                            .flags(flags)
                            .build();
                })
                .toList();
    }

    private void evaluateShortWorkDuration(
            Employee employee,
            LocalDate attendanceDate,
            List<AttendanceRecord> dailyRecords
    ) {
        AttendanceRule rule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.SHORT_WORK_DURATION_MINUTES
        );

        if (!Boolean.TRUE.equals(rule.getEnabled())) {
            return;
        }

        if (dailyRecords == null || dailyRecords.isEmpty()) {
            return;
        }

        Integer thresholdMinutes = rule.getThresholdValue();
        if (thresholdMinutes == null) {
            return;
        }

        long totalWorkedMinutes = calculateWorkedMinutes(dailyRecords);

        if (totalWorkedMinutes < thresholdMinutes) {
            createFlag(
                    employee,
                    attendanceDate,
                    AttendanceFlagType.SHORT_WORK_DURATION,
                    rule,
                    "Worked minutes " + totalWorkedMinutes + " is below threshold " + thresholdMinutes
            );
        }
    }

    private void evaluateInvalidOtEligibility(
            Employee employee,
            LocalDate attendanceDate,
            List<AttendanceRecord> dailyRecords
    ) {
        AttendanceRule rule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.INVALID_OT_MINUTES_LIMIT
        );

        if (!Boolean.TRUE.equals(rule.getEnabled())) {
            return;
        }

        if (dailyRecords == null || dailyRecords.isEmpty()) {
            return;
        }

        Integer thresholdMinutes = rule.getThresholdValue();
        if (thresholdMinutes == null) {
            return;
        }

        long suspiciousOtCount = dailyRecords.stream()
                .filter(this::isUsableAttendanceRecord)
                .filter(r -> r.getOvertimeMinutes() != null)
                .filter(r -> r.getOvertimeMinutes() > thresholdMinutes)
                .count();

        if (suspiciousOtCount > 0) {
            createFlag(
                    employee,
                    attendanceDate,
                    AttendanceFlagType.INVALID_OT_ELIGIBILITY,
                    rule,
                    "Detected " + suspiciousOtCount + " attendance records with overtime minutes above threshold " + thresholdMinutes
            );
        }
    }

    private void evaluateTooManyCorrections(Employee employee, LocalDate attendanceDate) {
        AttendanceRule limitRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.TOO_MANY_CORRECTIONS_LIMIT
        );
        AttendanceRule windowRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.TOO_MANY_CORRECTIONS_WINDOW_DAYS
        );

        if (!Boolean.TRUE.equals(limitRule.getEnabled())) {
            return;
        }

        Integer limit = limitRule.getThresholdValue();
        Integer windowDays = windowRule.getThresholdValue();

        if (limit == null || windowDays == null || windowDays <= 0) {
            return;
        }

        LocalDate startDate = attendanceDate.minusDays(windowDays - 1);

        List<AttendanceCorrectionRequest> corrections =
                correctionRequestRepository.findByEmployeeEmployeeIdAndAttendanceDateBetween(
                        employee.getEmployeeId(),
                        startDate,
                        attendanceDate
                );

        int count = corrections != null ? corrections.size() : 0;

        if (count > limit) {
            createFlag(
                    employee,
                    attendanceDate,
                    AttendanceFlagType.TOO_MANY_CORRECTIONS,
                    limitRule,
                    "Correction request count " + count + " exceeded limit " + limit + " within last " + windowDays + " day(s)"
            );
        }
    }

    private void evaluateWebAttendanceDependency(Employee employee, LocalDate attendanceDate) {
        AttendanceRule limitRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.WEB_ATTENDANCE_DEPENDENCY_LIMIT
        );
        AttendanceRule windowRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.WEB_ATTENDANCE_DEPENDENCY_WINDOW_DAYS
        );

        if (!Boolean.TRUE.equals(limitRule.getEnabled())) {
            return;
        }

        Integer limit = limitRule.getThresholdValue();
        Integer windowDays = windowRule.getThresholdValue();

        if (limit == null || windowDays == null || windowDays <= 0) {
            return;
        }

        LocalDate startDate = attendanceDate.minusDays(windowDays - 1);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = attendanceDate.plusDays(1).atStartOfDay();

        List<AttendanceRecord> records =
                attendanceRecordRepository.findByEmployeeEmployeeIdAndEventTimeBetween(
                        employee.getEmployeeId(),
                        start,
                        end
                );

        long webCount = records.stream()
                .filter(this::isUsableAttendanceRecord)
                .filter(r -> r.getSource() == AttendanceSource.WEB)
                .count();

        if (webCount > limit) {
            createFlag(
                    employee,
                    attendanceDate,
                    AttendanceFlagType.WEB_ATTENDANCE_DEPENDENCY,
                    limitRule,
                    "Web/manual attendance usage count " + webCount + " exceeded limit " + limit + " within last " + windowDays + " day(s)"
            );
        }
    }

    private void createFlag(
            Employee employee,
            LocalDate attendanceDate,
            AttendanceFlagType flagType,
            AttendanceRule rule,
            String message
    ) {
        AttendanceFlag flag = AttendanceFlag.builder()
                .employee(employee)
                .attendanceDate(attendanceDate)
                .attendance(null)
                .flagType(flagType)
                .severity(rule.getSeverity())
                .scoreImpact(rule.getScoreImpact() != null ? rule.getScoreImpact() : 0)
                .message(message)
                .resolved(false)
                .detectedAt(LocalDateTime.now())
                .build();

        attendanceFlagRepository.save(flag);
    }

    private void recalculateDailyRiskScore(Employee employee, LocalDate attendanceDate) {
        List<AttendanceFlag> unresolvedFlags =
                attendanceFlagRepository.findByEmployeeEmployeeIdAndAttendanceDateAndResolvedFalse(
                        employee.getEmployeeId(),
                        attendanceDate
                );

        int riskScore = unresolvedFlags.stream()
                .map(AttendanceFlag::getScoreImpact)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .sum();

        riskScore = clamp(riskScore, 0, 100);

        int trustScore = clamp(100 - riskScore, 0, 100);
        int totalFlags = unresolvedFlags.size();

        AttendanceRule reviewTrustRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.REVIEW_TRUST_THRESHOLD
        );
        AttendanceRule highRiskTrustRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.HIGH_RISK_TRUST_THRESHOLD
        );

        int reviewTrustThreshold = reviewTrustRule.getThresholdValue() != null
                ? reviewTrustRule.getThresholdValue()
                : 70;

        int highRiskTrustThreshold = highRiskTrustRule.getThresholdValue() != null
                ? highRiskTrustRule.getThresholdValue()
                : 40;

        boolean requiresReview = trustScore <= reviewTrustThreshold;
        boolean highRisk = trustScore <= highRiskTrustThreshold;

        AttendanceRiskScore dailyScore =
                attendanceRiskScoreRepository
                        .findByEmployeeEmployeeIdAndAttendanceDate(employee.getEmployeeId(), attendanceDate)
                        .orElse(
                                AttendanceRiskScore.builder()
                                        .employee(employee)
                                        .attendanceDate(attendanceDate)
                                        .riskScore(0)
                                        .trustScore(100)
                                        .totalFlags(0)
                                        .requiresReview(false)
                                        .highRisk(false)
                                        .build()
                        );

        dailyScore.setRiskScore(riskScore);
        dailyScore.setTrustScore(trustScore);
        dailyScore.setTotalFlags(totalFlags);
        dailyScore.setRequiresReview(requiresReview);
        dailyScore.setHighRisk(highRisk);

        attendanceRiskScoreRepository.save(dailyScore);
    }

    private void updateCurrentBehaviorScore(Employee employee, LocalDate attendanceDate) {
        AttendanceRiskScore dailyScore =
                attendanceRiskScoreRepository
                        .findByEmployeeEmployeeIdAndAttendanceDate(employee.getEmployeeId(), attendanceDate)
                        .orElseThrow(() -> new ResourceNotFoundException("Daily attendance risk score not found"));

        EmployeeBehaviorScore current =
                employeeBehaviorScoreRepository
                        .findByEmployeeEmployeeId(employee.getEmployeeId())
                        .orElse(
                                EmployeeBehaviorScore.builder()
                                        .employee(employee)
                                        .currentRiskScore(0)
                                        .currentTrustScore(100)
                                        .currentRiskLevel(RiskSeverity.LOW)
                                        .totalFlagsSeen(0)
                                        .totalHighRiskDays(0)
                                        .updatedAt(LocalDateTime.now())
                                        .build()
                        );

        int previousRisk = valueOrZero(current.getCurrentRiskScore());
        int newRisk = (int) Math.round((previousRisk * 0.70) + (dailyScore.getRiskScore() * 0.30));
        newRisk = clamp(newRisk, 0, 100);

        int newTrust = clamp(100 - newRisk, 0, 100);

        AttendanceRule highRiskTrustRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.HIGH_RISK_TRUST_THRESHOLD
        );
        AttendanceRule reviewTrustRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.REVIEW_TRUST_THRESHOLD
        );

        int highRiskTrustThreshold = highRiskTrustRule.getThresholdValue() != null
                ? highRiskTrustRule.getThresholdValue()
                : 40;

        int reviewTrustThreshold = reviewTrustRule.getThresholdValue() != null
                ? reviewTrustRule.getThresholdValue()
                : 70;

        RiskSeverity level;
        if (newTrust <= highRiskTrustThreshold) {
            level = RiskSeverity.HIGH;
        } else if (newTrust <= reviewTrustThreshold) {
            level = RiskSeverity.MEDIUM;
        } else {
            level = RiskSeverity.LOW;
        }

        int todayFlagCount = valueOrZero(dailyScore.getTotalFlags());

        current.setCurrentRiskScore(newRisk);
        current.setCurrentTrustScore(newTrust);
        current.setCurrentRiskLevel(level);
        current.setTotalFlagsSeen(valueOrZero(current.getTotalFlagsSeen()) + todayFlagCount);

        if (Boolean.TRUE.equals(dailyScore.getHighRisk())) {
            current.setTotalHighRiskDays(valueOrZero(current.getTotalHighRiskDays()) + 1);
        }

        current.setLastEvaluatedAt(LocalDateTime.now());
        current.setUpdatedAt(LocalDateTime.now());

        employeeBehaviorScoreRepository.save(current);
    }

    private long calculateWorkedMinutes(List<AttendanceRecord> dailyRecords) {
        List<AttendanceRecord> ordered = dailyRecords.stream()
                .filter(this::isUsableAttendanceRecord)
                .sorted(Comparator.comparing(AttendanceRecord::getEventTime))
                .toList();

        long totalMinutes = 0;
        LocalDateTime openCheckIn = null;

        for (AttendanceRecord record : ordered) {
            if (record.getType() == AttendanceType.IN) {
                openCheckIn = record.getEventTime();
            } else if (record.getType() == AttendanceType.OUT && openCheckIn != null) {
                if (!record.getEventTime().isBefore(openCheckIn)) {
                    totalMinutes += Duration.between(openCheckIn, record.getEventTime()).toMinutes();
                }
                openCheckIn = null;
            }
        }

        return Math.max(totalMinutes, 0);
    }

    private boolean isUsableAttendanceRecord(AttendanceRecord record) {
        return record != null
                && record.getEventTime() != null
                && record.getStatus() == AttendanceStatus.VALID;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}