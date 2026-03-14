package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.AttendanceDecisionDTO;
import edu.ijse.inshiftbackend.dto.AttendancePunchDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceResponseDTO;
import edu.ijse.inshiftbackend.entity.AttendanceAudit;
import edu.ijse.inshiftbackend.entity.AttendanceRecord;
import edu.ijse.inshiftbackend.entity.Branch;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.Shift;
import edu.ijse.inshiftbackend.entity.enums.AttendanceMark;
import edu.ijse.inshiftbackend.entity.enums.AttendanceSource;
import edu.ijse.inshiftbackend.entity.enums.AttendanceStatus;
import edu.ijse.inshiftbackend.entity.enums.AttendanceType;
import edu.ijse.inshiftbackend.entity.enums.AuditAction;
import edu.ijse.inshiftbackend.entity.enums.VerificationMethod;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.AttendanceAuditRepository;
import edu.ijse.inshiftbackend.repository.AttendanceRecordRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.AttendanceService;
import edu.ijse.inshiftbackend.service.AttendanceSummaryService;
import edu.ijse.inshiftbackend.util.GeoUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final AttendanceAuditRepository auditRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceSummaryService attendanceSummaryService;

    @Override
    @Transactional
    public AttendanceResponseDTO punch(AttendancePunchDTO dto, AttendanceSource source, String email) {

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (Boolean.FALSE.equals(employee.getActive())) {
            throw new BadRequestException("Inactive employee cannot mark attendance");
        }

        Shift shift = employee.getShift();
        if (shift == null) {
            throw new BadRequestException("Employee shift not assigned");
        }

        if (Boolean.FALSE.equals(shift.getActive())) {
            throw new BadRequestException("Assigned shift is inactive");
        }

        AttendanceType type = parseAttendanceType(dto.getType());

        Optional<AttendanceRecord> lastValid =
                attendanceRepository.findTopByEmployeeEmployeeIdAndStatusOrderByEventTimeDesc(
                        employee.getEmployeeId(),
                        AttendanceStatus.VALID
                );

        Optional<AttendanceRecord> lastAny =
                attendanceRepository.findTopByEmployeeEmployeeIdOrderByEventTimeDesc(employee.getEmployeeId());

        validateWebRules(dto, source);

        if (source == AttendanceSource.MOBILE) {
            validateMobileLocation(dto, employee.getBranch());
        }

        validatePunchFlow(type, lastValid, lastAny);

        AttendanceStatus status = (source == AttendanceSource.WEB)
                ? AttendanceStatus.PENDING
                : AttendanceStatus.VALID;

        boolean verified = (source != AttendanceSource.WEB);

        VerificationMethod verificationMethod = (source == AttendanceSource.MOBILE)
                ? VerificationMethod.PASSKEY
                : (source == AttendanceSource.WEB)
                ? VerificationMethod.ADMIN
                : VerificationMethod.NONE;

        LocalDateTime now = LocalDateTime.now();

        PunchEvaluation evaluation = evaluatePunch(type, shift, now);

        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .branch(employee.getBranch())
                .type(type)
                .source(source)
                .status(status)
                .eventTime(now)
                .attendanceMark(evaluation.mark())
                .lateMinutes(evaluation.lateMinutes())
                .earlyLeaveMinutes(evaluation.earlyLeaveMinutes())
                .overtimeMinutes(evaluation.overtimeMinutes())
                .lat(dto.getLat())
                .lng(dto.getLng())
                .locationText(dto.getLocationText())
                .reason(dto.getReason())
                .verified(verified)
                .verificationMethod(verificationMethod)
                .createdBy("EMPLOYEE")
                .build();

        AttendanceRecord saved = attendanceRepository.save(record);

        attendanceSummaryService.generateDailySummary(employee.getEmployeeId(), now.toLocalDate()); // generate summery after each punch

        AttendanceAudit audit = AttendanceAudit.builder()
                .attendance(saved)
                .action(AuditAction.CREATE)
                .doneByRole("EMPLOYEE")
                .doneByUserId(employee.getEmployeeId())
                .note(buildAuditNote(type, source, status, evaluation))
                .build();

        auditRepository.save(audit);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public List<AttendanceResponseDTO> getPending() {
        return attendanceRepository
                .findAllByStatusOrderByEventTimeDesc(AttendanceStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public AttendanceResponseDTO approve(Long attendanceId, String adminEmail) {

        Employee admin = employeeRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        AttendanceRecord record = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));

        if (record.getStatus() != AttendanceStatus.PENDING) {
            throw new BadRequestException("Only PENDING attendance can be approved");
        }

        record.setStatus(AttendanceStatus.VALID);
        record.setVerified(true);
        record.setVerificationMethod(VerificationMethod.ADMIN);
        record.setDecisionNote(null);

        AttendanceRecord saved = attendanceRepository.save(record);

        AttendanceAudit audit = AttendanceAudit.builder()
                .attendance(saved)
                .action(AuditAction.APPROVE)
                .doneByRole("ADMIN")
                .doneByUserId(admin.getEmployeeId())
                .note("Attendance approved")
                .build();

        auditRepository.save(audit);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public AttendanceResponseDTO reject(Long attendanceId, AttendanceDecisionDTO dto, String adminEmail) {

        Employee admin = employeeRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        AttendanceRecord record = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));

        if (record.getStatus() != AttendanceStatus.PENDING) {
            throw new BadRequestException("Only PENDING attendance can be rejected");
        }

        String note = (dto == null || dto.getNote() == null) ? null : dto.getNote().trim();
        if (note == null || note.isEmpty()) {
            throw new BadRequestException("Decision note is required");
        }

        record.setStatus(AttendanceStatus.REJECTED);
        record.setVerified(false);
        record.setVerificationMethod(VerificationMethod.ADMIN);
        record.setDecisionNote(note);

        AttendanceRecord saved = attendanceRepository.save(record);

        AttendanceAudit audit = AttendanceAudit.builder()
                .attendance(saved)
                .action(AuditAction.REJECT)
                .doneByRole("ADMIN")
                .doneByUserId(admin.getEmployeeId())
                .note("Attendance rejected: " + note)
                .build();

        auditRepository.save(audit);

        return mapToResponse(saved);
    }

    private AttendanceType parseAttendanceType(String typeStr) {
        try {
            return AttendanceType.valueOf(typeStr.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("Invalid attendance type. Must be IN or OUT");
        }
    }

    private void validateWebRules(AttendancePunchDTO dto, AttendanceSource source) {
        if (source == AttendanceSource.WEB) {
            if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
                throw new BadRequestException("Reason is required for Web attendance");
            }
        }
    }

    private void validateMobileLocation(AttendancePunchDTO dto, Branch branch) {

        if (dto.getLat() == null || dto.getLng() == null) {
            throw new BadRequestException("Location required for mobile attendance");
        }

        if (branch == null) {
            throw new BadRequestException("Employee branch not assigned");
        }

        if (branch.getLatitude() == null || branch.getLongitude() == null || branch.getRadiusMeters() == null) {
            throw new BadRequestException("Branch location configuration missing");
        }

        boolean inside = GeoUtil.isWithinRadius(
                branch.getLatitude(),
                branch.getLongitude(),
                dto.getLat(),
                dto.getLng(),
                branch.getRadiusMeters()
        );

        if (!inside) {
            throw new BadRequestException("You are outside the allowed work area");
        }
    }

    private void validatePunchFlow(
            AttendanceType type,
            Optional<AttendanceRecord> lastValid,
            Optional<AttendanceRecord> lastAny
    ) {
        if (type == AttendanceType.OUT && lastAny.isPresent()) {
            AttendanceRecord last = lastAny.get();
            if (last.getType() == AttendanceType.IN && last.getStatus() == AttendanceStatus.PENDING) {
                throw new BadRequestException("Your last check-in is pending approval. You cannot check out yet.");
            }
            if (last.getType() == AttendanceType.IN && last.getStatus() == AttendanceStatus.REJECTED) {
                throw new BadRequestException("Your last check-in was rejected. Please submit a valid check-in first.");
            }
        }

        if (type == AttendanceType.IN) {
            if (lastValid.isPresent() && lastValid.get().getType() == AttendanceType.IN) {
                throw new BadRequestException("Already checked in");
            }
        }

        if (type == AttendanceType.OUT) {
            if (lastValid.isEmpty() || lastValid.get().getType() != AttendanceType.IN) {
                throw new BadRequestException("Cannot check out without a valid check-in");
            }
        }
    }

    private PunchEvaluation evaluatePunch(AttendanceType type, Shift shift, LocalDateTime eventTime) {
        LocalTime nowTime = eventTime.toLocalTime();

        if (type == AttendanceType.IN) {
            LocalTime earliestAllowed = shift.getStartTime().minusMinutes(shift.getEarlyCheckInMinutes());
            LocalTime graceEnd = shift.getStartTime().plusMinutes(shift.getGraceMinutes());

            if (nowTime.isBefore(earliestAllowed)) {
                throw new BadRequestException(
                        "Too early to check in. Earliest allowed time is " + earliestAllowed
                );
            }

            if (nowTime.isAfter(graceEnd)) {
                int lateMinutes = (int) Duration.between(shift.getStartTime(), nowTime).toMinutes();
                return new PunchEvaluation(AttendanceMark.LATE, lateMinutes, 0, 0);
            }

            return new PunchEvaluation(AttendanceMark.ON_TIME, 0, 0, 0);
        }

        LocalTime overtimeStart = shift.getEndTime().plusMinutes(shift.getOvertimeAfterMinutes());

        if (nowTime.isBefore(shift.getEndTime())) {
            int earlyLeaveMinutes = (int) Duration.between(nowTime, shift.getEndTime()).toMinutes();
            return new PunchEvaluation(AttendanceMark.EARLY_LEAVE, 0, earlyLeaveMinutes, 0);
        }

        if (nowTime.isAfter(overtimeStart)) {
            int overtimeMinutes = (int) Duration.between(shift.getEndTime(), nowTime).toMinutes();
            return new PunchEvaluation(AttendanceMark.OVERTIME, 0, 0, overtimeMinutes);
        }

        return new PunchEvaluation(AttendanceMark.NORMAL, 0, 0, 0);
    }

    private String buildAuditNote(
            AttendanceType type,
            AttendanceSource source,
            AttendanceStatus status,
            PunchEvaluation evaluation
    ) {
        return "Punch " + type +
                " via " + source +
                " saved as " + status +
                " [" + evaluation.mark() +
                ", late=" + evaluation.lateMinutes() +
                ", earlyLeave=" + evaluation.earlyLeaveMinutes() +
                ", overtime=" + evaluation.overtimeMinutes() + "]";
    }

    private AttendanceResponseDTO mapToResponse(AttendanceRecord saved) {
        return AttendanceResponseDTO.builder()
                .id(saved.getId())
                .employeeId(saved.getEmployee().getEmployeeId())
                .employeeName(saved.getEmployee().getFullName())
                .branchId(saved.getBranch().getBranchId())
                .branchName(saved.getBranch().getBranchName())
                .type(saved.getType().name())
                .source(saved.getSource().name())
                .status(saved.getStatus().name())
                .eventTime(saved.getEventTime())
                .attendanceMark(saved.getAttendanceMark() != null ? saved.getAttendanceMark().name() : null)
                .lateMinutes(saved.getLateMinutes())
                .earlyLeaveMinutes(saved.getEarlyLeaveMinutes())
                .overtimeMinutes(saved.getOvertimeMinutes())
                .lat(saved.getLat())
                .lng(saved.getLng())
                .locationText(saved.getLocationText())
                .verified(saved.isVerified())
                .verificationMethod(saved.getVerificationMethod().name())
                .decisionNote(saved.getDecisionNote())
                .build();
    }

    private record PunchEvaluation(AttendanceMark mark, int lateMinutes, int earlyLeaveMinutes, int overtimeMinutes) {

    }
}