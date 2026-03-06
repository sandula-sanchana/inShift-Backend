package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.AttendancePunchDTO;
import edu.ijse.inshiftbackend.dto.AttendanceDecisionDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceResponseDTO;
import edu.ijse.inshiftbackend.entity.AttendanceAudit;
import edu.ijse.inshiftbackend.entity.AttendanceRecord;
import edu.ijse.inshiftbackend.entity.Branch;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.enums.*;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.AttendanceAuditRepository;
import edu.ijse.inshiftbackend.repository.AttendanceRecordRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.AttendanceService;
import edu.ijse.inshiftbackend.util.GeoUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final AttendanceAuditRepository auditRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public AttendanceResponseDTO punch(AttendancePunchDTO dto, AttendanceSource source, String email) {

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (Boolean.FALSE.equals(employee.getActive())) {
            throw new BadRequestException("Inactive employee cannot mark attendance");
        }

        AttendanceType type = parseAttendanceType(dto.getType());

        //last accepted/valid record (used for IN/OUT flow)
        Optional<AttendanceRecord> lastValid =
                attendanceRepository.findTopByEmployeeEmployeeIdAndStatusOrderByEventTimeDesc(
                        employee.getEmployeeId(),
                        AttendanceStatus.VALID
                );

        //last record overall
        Optional<AttendanceRecord> lastAny =
                attendanceRepository.findTopByEmployeeEmployeeIdOrderByEventTimeDesc(employee.getEmployeeId());

        //Enforce Web rules
        validateWebRules(dto, source);

        //Location checks for MOBILE (required + inside branch radius)
        if (source == AttendanceSource.MOBILE) {
            validateMobileLocation(dto, employee.getBranch());
        }

        //IN/OUT Rules
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

        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .branch(employee.getBranch())
                .type(type)
                .source(source)
                .status(status)
                .lat(dto.getLat())
                .lng(dto.getLng())
                .locationText(dto.getLocationText())
                .reason(dto.getReason())
                .verified(verified)
                .verificationMethod(verificationMethod)
                .createdBy("EMPLOYEE")
                .build();

        System.out.println("SOURCE = " + source);
        System.out.println("TYPE = " + type);
        System.out.println("DTO LAT = " + dto.getLat());
        System.out.println("DTO LNG = " + dto.getLng());
        System.out.println("BRANCH LAT = " + employee.getBranch().getLatitude());
        System.out.println("BRANCH LNG = " + employee.getBranch().getLongitude());
        System.out.println("RADIUS = " + employee.getBranch().getRadiusMeters());
        System.out.println("LAST VALID = " + lastValid.map(AttendanceRecord::getType).orElse(null));
        System.out.println("LAST ANY STATUS = " + lastAny.map(AttendanceRecord::getStatus).orElse(null));
        System.out.println("LAST ANY TYPE = " + lastAny.map(AttendanceRecord::getType).orElse(null));

        AttendanceRecord saved = attendanceRepository.save(record);

        AttendanceAudit audit = AttendanceAudit.builder()
                .attendance(saved)
                .action(AuditAction.CREATE)
                .doneByRole("EMPLOYEE")
                .doneByUserId(employee.getEmployeeId())
                .note(buildAuditNote(type, source, status))
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

        //Validate admin exists (role check can be added later if you have roles)
        Employee admin = employeeRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        //Find attendance record
        AttendanceRecord record = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));

        //Only pending can be approved
        if (record.getStatus() != AttendanceStatus.PENDING) {
            throw new BadRequestException("Only PENDING attendance can be approved");
        }


        record.setStatus(AttendanceStatus.VALID);
        record.setVerified(true);
        record.setVerificationMethod(VerificationMethod.ADMIN);
        record.setDecisionNote(null);

        AttendanceRecord saved = attendanceRepository.save(record);

        //audit
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
        //If last record overall is PENDING IN, block OUT until approved
        if (type == AttendanceType.OUT && lastAny.isPresent()) {
            AttendanceRecord last = lastAny.get();
            if (last.getType() == AttendanceType.IN && last.getStatus() == AttendanceStatus.PENDING) {
                throw new BadRequestException("Your last check-in is pending approval. You cannot check out yet.");
            }
            if (last.getType() == AttendanceType.IN && last.getStatus() == AttendanceStatus.REJECTED) {
                throw new BadRequestException("Your last check-in was rejected. Please submit a valid check-in first.");
            }
        }

        //Use VALID flow for session rules
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

    private String buildAuditNote(AttendanceType type, AttendanceSource source, AttendanceStatus status) {
        return "Punch " + type + " via " + source + " saved as " + status;
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
                .lat(saved.getLat())
                .lng(saved.getLng())
                .locationText(saved.getLocationText())
                .verified(saved.isVerified())
                .verificationMethod(saved.getVerificationMethod().name())
                .decisionNote(saved.getDecisionNote())
                .build();
    }
}