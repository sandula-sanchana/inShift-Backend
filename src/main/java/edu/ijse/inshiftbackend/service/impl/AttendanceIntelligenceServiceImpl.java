package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.AttendanceFlag;
import edu.ijse.inshiftbackend.entity.AttendanceRecord;
import edu.ijse.inshiftbackend.entity.AttendanceRiskScore;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.Shift;
import edu.ijse.inshiftbackend.entity.enums.AttendanceFlagType;
import edu.ijse.inshiftbackend.entity.enums.AttendanceSource;
import edu.ijse.inshiftbackend.entity.enums.AttendanceMark;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceIntelligenceServiceImpl implements AttendanceIntelligenceService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceCorrectionRequestRepository correctionRepository;
    private final AttendanceFlagRepository attendanceFlagRepository;
    private final AttendanceRiskScoreRepository riskScoreRepository;
    private final ShiftRepository shiftRepository;

    @Override
    @Transactional
    public void evaluateDay(Long employeeId, LocalDate attendanceDate) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Shift shift = resolveShift(employee);

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
        if (validIns.isEmpty() || validOuts.isEmpty()) return;

        AttendanceRecord firstIn = validIns.get(0);
        AttendanceRecord lastOut = validOuts.get(validOuts.size() - 1);

        long workedMinutes = Duration.between(firstIn.getEventTime(), lastOut.getEventTime()).toMinutes();

        if (workedMinutes < 60) {
            createFlagIfAbsent(
                    employee,
                    attendanceDate,
                    lastOut,
                    AttendanceFlagType.SHORT_WORK_DURATION,
                    RiskSeverity.HIGH,
                    30,
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
        if (validIns.isEmpty() || validOuts.isEmpty()) return;

        AttendanceRecord firstIn = validIns.get(0);
        AttendanceRecord lastOut = validOuts.get(validOuts.size() - 1);

        long workedMinutes = Duration.between(firstIn.getEventTime(), lastOut.getEventTime()).toMinutes();

        long requiredShiftMinutes =
                Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes() - safeInt(shift.getBreakMinutes());

        if (requiredShiftMinutes < 0) requiredShiftMinutes = 0;

        boolean outMarkedOt =
                lastOut.getAttendanceMark() == AttendanceMark.OVERTIME ||
                        (lastOut.getOvertimeMinutes() != null && lastOut.getOvertimeMinutes() > 0);

        if (outMarkedOt && workedMinutes <= requiredShiftMinutes) {
            createFlagIfAbsent(
                    employee,
                    attendanceDate,
                    lastOut,
                    AttendanceFlagType.INVALID_OT_ELIGIBILITY,
                    RiskSeverity.CRITICAL,
                    50,
                    "Overtime detected without enough worked duration. Worked=" +
                            workedMinutes + " mins, required=" + requiredShiftMinutes + " mins"
            );
        }
    }

    private void checkTooManyCorrections(Employee employee, LocalDate attendanceDate) {
        LocalDateTime after = LocalDateTime.now().minusDays(30);
        long correctionCount = correctionRepository.countByEmployeeEmployeeIdAndCreatedAtAfter(
                employee.getEmployeeId(),
                after
        );

        if (correctionCount > 3) {
            createFlagIfAbsent(
                    employee,
                    attendanceDate,
                    null,
                    AttendanceFlagType.TOO_MANY_CORRECTIONS,
                    RiskSeverity.MEDIUM,
                    15,
                    "Too many correction requests in last 30 days: " + correctionCount
            );
        }
    }

    private void checkWebAttendanceDependency(Employee employee, LocalDate attendanceDate) {
        LocalDateTime start = LocalDateTime.now().minusDays(14);
        LocalDateTime end = LocalDateTime.now();

        long webCount = attendanceRecordRepository.countByEmployeeEmployeeIdAndSourceAndEventTimeBetween(
                employee.getEmployeeId(),
                AttendanceSource.WEB,
                start,
                end
        );

        if (webCount > 3) {
            createFlagIfAbsent(
                    employee,
                    attendanceDate,
                    null,
                    AttendanceFlagType.WEB_ATTENDANCE_DEPENDENCY,
                    RiskSeverity.MEDIUM,
                    15,
                    "Too many web/manual attendance punches in last 14 days: " + webCount
            );
        }
    }

    private void createFlagIfAbsent(
            Employee employee,
            LocalDate attendanceDate,
            AttendanceRecord attendance,
            AttendanceFlagType type,
            RiskSeverity severity,
            int scoreImpact,
            String message
    ) {
        boolean exists = attendanceFlagRepository
                .existsByEmployeeEmployeeIdAndAttendanceDateAndFlagTypeAndResolvedFalse(
                        employee.getEmployeeId(),
                        attendanceDate,
                        type
                );

        if (exists) return;

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
        score.setRequiresReview(trustScore < 60);
        score.setHighRisk(trustScore < 40);

        riskScoreRepository.save(score);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}