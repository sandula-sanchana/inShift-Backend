package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.response.AttendanceFlagDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceIntelligenceOverviewDTO;
import edu.ijse.inshiftbackend.entity.AttendanceFlag;
import edu.ijse.inshiftbackend.entity.AttendanceRecord;
import edu.ijse.inshiftbackend.entity.AttendanceRiskScore;
import edu.ijse.inshiftbackend.entity.AttendanceRule;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.Shift;
import edu.ijse.inshiftbackend.entity.enums.AttendanceFlagType;
import edu.ijse.inshiftbackend.entity.enums.AttendanceMark;
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
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.ShiftRepository;
import edu.ijse.inshiftbackend.service.AttendanceIntelligenceService;
import edu.ijse.inshiftbackend.service.AttendanceRuleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AttendanceIntelligenceServiceImpl implements AttendanceIntelligenceService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceCorrectionRequestRepository correctionRepository;
    private final AttendanceFlagRepository attendanceFlagRepository;
    private final AttendanceRiskScoreRepository riskScoreRepository;
    private final ShiftRepository shiftRepository;
    private final AttendanceRuleService attendanceRuleService;

    private static final Set<AttendanceFlagType> DAILY_REBUILD_FLAG_TYPES = Set.of(
            AttendanceFlagType.SHORT_WORK_DURATION,
            AttendanceFlagType.INVALID_OT_ELIGIBILITY,
            AttendanceFlagType.TOO_MANY_CORRECTIONS,
            AttendanceFlagType.WEB_ATTENDANCE_DEPENDENCY
    );

    @Override
    @Transactional
    public void evaluateDay(Long employeeId, LocalDate attendanceDate) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Shift shift = resolveShift(employee);

        clearDailyRebuildFlags(employee, attendanceDate);

        List<AttendanceRecord> validIns = findValidPunches(employeeId, attendanceDate, AttendanceType.IN);
        List<AttendanceRecord> validOuts = findValidPunches(employeeId, attendanceDate, AttendanceType.OUT);

        checkShortWorkDuration(employee, attendanceDate, validIns, validOuts);
        checkInvalidOtEligibility(employee, attendanceDate, shift, validIns, validOuts);
        checkTooManyCorrections(employee, attendanceDate);
        checkWebAttendanceDependency(employee, attendanceDate);

        recalculateRiskScore(employee, attendanceDate);
    }

    @Override
    public List<AttendanceFlag> getFlagsForEmployeeDay(Long employeeId, LocalDate attendanceDate) {
        return attendanceFlagRepository.findAllByEmployeeEmployeeIdAndAttendanceDateOrderByDetectedAtDesc(
                employeeId,
                attendanceDate
        );
    }

    @Override
    public List<AttendanceIntelligenceOverviewDTO> getDailyOverview(LocalDate date) {
        return riskScoreRepository.findAllByAttendanceDateOrderByRiskScoreDesc(date)
                .stream()
                .map(score -> {
                    List<AttendanceFlagDTO> flags = attendanceFlagRepository
                            .findAllByEmployeeEmployeeIdAndAttendanceDateOrderByDetectedAtDesc(
                                    score.getEmployee().getEmployeeId(),
                                    date
                            )
                            .stream()
                            .map(flag -> AttendanceFlagDTO.builder()
                                    .id(flag.getId())
                                    .flagType(flag.getFlagType().name())
                                    .severity(flag.getSeverity().name())
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

    private void clearDailyRebuildFlags(Employee employee, LocalDate attendanceDate) {
        List<AttendanceFlag> existingFlags = attendanceFlagRepository
                .findAllByEmployeeEmployeeIdAndAttendanceDateOrderByDetectedAtDesc(
                        employee.getEmployeeId(),
                        attendanceDate
                );

        existingFlags.stream()
                .filter(flag -> DAILY_REBUILD_FLAG_TYPES.contains(flag.getFlagType()))
                .forEach(attendanceFlagRepository::delete);
    }

    private List<AttendanceRecord> findValidPunches(Long employeeId, LocalDate attendanceDate, AttendanceType type) {
        LocalDateTime start = attendanceDate.atStartOfDay();
        LocalDateTime end = attendanceDate.plusDays(1).atStartOfDay().minusNanos(1);

        return attendanceRecordRepository
                .findAllByEmployeeEmployeeIdAndTypeAndStatusAndEventTimeBetweenOrderByEventTimeAsc(
                        employeeId,
                        type,
                        AttendanceStatus.VALID,
                        start,
                        end
                );
    }

    private Shift resolveShift(Employee employee) {
        Shift employeeShift = employee.getShift();

        if (employeeShift != null && Boolean.TRUE.equals(employeeShift.getActive())) {
            return employeeShift;
        }

        return shiftRepository.findByIsDefaultTrueAndActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active default shift configured"));
    }

    private void checkShortWorkDuration(
            Employee employee,
            LocalDate attendanceDate,
            List<AttendanceRecord> validIns,
            List<AttendanceRecord> validOuts
    ) {
        AttendanceRule rule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.SHORT_WORK_DURATION_MINUTES
        );

        if (!Boolean.TRUE.equals(rule.getEnabled())) return;
        if (validIns.isEmpty() || validOuts.isEmpty()) return;

        AttendanceRecord firstIn = validIns.get(0);
        AttendanceRecord lastOut = validOuts.get(validOuts.size() - 1);

        long workedMinutes = Duration.between(firstIn.getEventTime(), lastOut.getEventTime()).toMinutes();

        if (workedMinutes < safeInt(rule.getThresholdValue())) {
            createFlag(
                    employee,
                    attendanceDate,
                    lastOut,
                    AttendanceFlagType.SHORT_WORK_DURATION,
                    defaultSeverity(rule.getSeverity(), RiskSeverity.HIGH),
                    safeInt(rule.getScoreImpact()),
                    "Worked duration is too short: " + workedMinutes + " minutes"
            );
        }
    }

    private void checkInvalidOtEligibility(
            Employee employee,
            LocalDate attendanceDate,
            Shift shift,
            List<AttendanceRecord> validIns,
            List<AttendanceRecord> validOuts
    ) {
        AttendanceRule rule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.INVALID_OT_ELIGIBILITY_SCORE
        );

        if (!Boolean.TRUE.equals(rule.getEnabled())) return;
        if (validIns.isEmpty() || validOuts.isEmpty()) return;

        AttendanceRecord firstIn = validIns.get(0);
        AttendanceRecord lastOut = validOuts.get(validOuts.size() - 1);

        long workedMinutes = Duration.between(firstIn.getEventTime(), lastOut.getEventTime()).toMinutes();

        long requiredShiftMinutes =
                Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes()
                        - safeInt(shift.getBreakMinutes());

        if (requiredShiftMinutes < 0) requiredShiftMinutes = 0;

        boolean outMarkedOt =
                lastOut.getAttendanceMark() == AttendanceMark.OVERTIME ||
                        (lastOut.getOvertimeMinutes() != null && lastOut.getOvertimeMinutes() > 0);

        if (outMarkedOt && workedMinutes <= requiredShiftMinutes) {
            createFlag(
                    employee,
                    attendanceDate,
                    lastOut,
                    AttendanceFlagType.INVALID_OT_ELIGIBILITY,
                    defaultSeverity(rule.getSeverity(), RiskSeverity.CRITICAL),
                    safeInt(rule.getScoreImpact()),
                    "Overtime detected without enough worked duration. Worked=" +
                            workedMinutes + " mins, required=" + requiredShiftMinutes + " mins"
            );
        }
    }

    private void checkTooManyCorrections(Employee employee, LocalDate attendanceDate) {
        AttendanceRule limitRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.TOO_MANY_CORRECTIONS_LIMIT
        );
        AttendanceRule windowRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.TOO_MANY_CORRECTIONS_WINDOW_DAYS
        );

        if (!Boolean.TRUE.equals(limitRule.getEnabled())) return;

        int windowDays = Math.max(1, safeInt(windowRule.getThresholdValue()));
        LocalDateTime start = attendanceDate.minusDays(windowDays - 1L).atStartOfDay();
        LocalDateTime end = attendanceDate.plusDays(1).atStartOfDay().minusNanos(1);

        long correctionCount = correctionRepository.countByEmployeeEmployeeIdAndCreatedAtAfter(
                employee.getEmployeeId(),
                start
        );

        if (correctionCount >= safeInt(limitRule.getThresholdValue())) {
            createFlag(
                    employee,
                    attendanceDate,
                    null,
                    AttendanceFlagType.TOO_MANY_CORRECTIONS,
                    defaultSeverity(limitRule.getSeverity(), RiskSeverity.MEDIUM),
                    safeInt(limitRule.getScoreImpact()),
                    "Too many correction requests in last " + windowDays + " days: " + correctionCount
            );
        }
    }

    private void checkWebAttendanceDependency(Employee employee, LocalDate attendanceDate) {
        AttendanceRule limitRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.WEB_ATTENDANCE_DEPENDENCY_LIMIT
        );
        AttendanceRule windowRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.WEB_ATTENDANCE_DEPENDENCY_WINDOW_DAYS
        );

        if (!Boolean.TRUE.equals(limitRule.getEnabled())) return;

        int windowDays = Math.max(1, safeInt(windowRule.getThresholdValue()));
        LocalDateTime start = attendanceDate.minusDays(windowDays - 1L).atStartOfDay();
        LocalDateTime end = attendanceDate.plusDays(1).atStartOfDay().minusNanos(1);

        long webCount = attendanceRecordRepository.countByEmployeeEmployeeIdAndSourceAndEventTimeBetween(
                employee.getEmployeeId(),
                AttendanceSource.WEB,
                start,
                end
        );

        if (webCount >= safeInt(limitRule.getThresholdValue())) {
            createFlag(
                    employee,
                    attendanceDate,
                    null,
                    AttendanceFlagType.WEB_ATTENDANCE_DEPENDENCY,
                    defaultSeverity(limitRule.getSeverity(), RiskSeverity.MEDIUM),
                    safeInt(limitRule.getScoreImpact()),
                    "Too many web/manual attendance punches in last " + windowDays + " days: " + webCount
            );
        }
    }

    private void createFlag(
            Employee employee,
            LocalDate attendanceDate,
            AttendanceRecord attendance,
            AttendanceFlagType type,
            RiskSeverity severity,
            int scoreImpact,
            String message
    ) {
        AttendanceFlag flag = AttendanceFlag.builder()
                .employee(employee)
                .attendanceDate(attendanceDate)
                .attendance(attendance)
                .flagType(type)
                .severity(severity)
                .scoreImpact(scoreImpact)
                .message(message)
                .resolved(false)
                .build();

        attendanceFlagRepository.save(flag);
    }

    private void recalculateRiskScore(Employee employee, LocalDate attendanceDate) {
        List<AttendanceFlag> flags =
                attendanceFlagRepository.findAllByEmployeeEmployeeIdAndAttendanceDateOrderByDetectedAtDesc(
                        employee.getEmployeeId(),
                        attendanceDate
                );

        int riskScore = flags.stream()
                .filter(f -> Boolean.FALSE.equals(f.getResolved()))
                .mapToInt(f -> f.getScoreImpact() == null ? 0 : f.getScoreImpact())
                .sum();

        int trustScore = Math.max(0, 100 - riskScore);
        int totalFlags = (int) flags.stream()
                .filter(f -> Boolean.FALSE.equals(f.getResolved()))
                .count();

        AttendanceRule reviewRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.REVIEW_TRUST_THRESHOLD
        );
        AttendanceRule highRiskRule = attendanceRuleService.getRequiredRule(
                AttendanceRuleKey.HIGH_RISK_TRUST_THRESHOLD
        );

        AttendanceRiskScore score = riskScoreRepository
                .findByEmployeeEmployeeIdAndAttendanceDate(employee.getEmployeeId(), attendanceDate)
                .orElse(
                        AttendanceRiskScore.builder()
                                .employee(employee)
                                .attendanceDate(attendanceDate)
                                .build()
                );

        score.setRiskScore(riskScore);
        score.setTrustScore(trustScore);
        score.setTotalFlags(totalFlags);
        score.setRequiresReview(trustScore < safeInt(reviewRule.getThresholdValue()));
        score.setHighRisk(trustScore < safeInt(highRiskRule.getThresholdValue()));

        riskScoreRepository.save(score);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private RiskSeverity defaultSeverity(RiskSeverity value, RiskSeverity fallback) {
        return value == null ? fallback : value;
    }
}