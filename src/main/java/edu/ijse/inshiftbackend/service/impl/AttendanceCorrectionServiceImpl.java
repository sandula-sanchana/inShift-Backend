package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.AttendanceCorrectionRequestDTO;
import edu.ijse.inshiftbackend.dto.CorrectionDecisionDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceCorrectionResponseDTO;
import edu.ijse.inshiftbackend.entity.AttendanceAudit;
import edu.ijse.inshiftbackend.entity.AttendanceCorrectionRequest;
import edu.ijse.inshiftbackend.entity.AttendanceRecord;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.enums.AttendanceMark;
import edu.ijse.inshiftbackend.entity.enums.AttendanceSource;
import edu.ijse.inshiftbackend.entity.enums.AttendanceStatus;
import edu.ijse.inshiftbackend.entity.enums.AttendanceType;
import edu.ijse.inshiftbackend.entity.enums.AuditAction;
import edu.ijse.inshiftbackend.entity.enums.CorrectionStatus;
import edu.ijse.inshiftbackend.entity.enums.CorrectionType;
import edu.ijse.inshiftbackend.entity.enums.VerificationMethod;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.AttendanceAuditRepository;
import edu.ijse.inshiftbackend.repository.AttendanceCorrectionRequestRepository;
import edu.ijse.inshiftbackend.repository.AttendanceRecordRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.AttendanceCorrectionService;
import edu.ijse.inshiftbackend.service.AttendanceSummaryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AttendanceCorrectionServiceImpl implements AttendanceCorrectionService {

    private final AttendanceCorrectionRequestRepository correctionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceAuditRepository attendanceAuditRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceSummaryService attendanceSummaryService;

    @Override
    @Transactional
    public AttendanceCorrectionResponseDTO submitRequest(AttendanceCorrectionRequestDTO dto, String employeeEmail) {

        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (Boolean.FALSE.equals(employee.getActive())) {
            throw new BadRequestException("Inactive employee cannot submit correction requests");
        }

        validateSubmitRequest(dto);

        AttendanceCorrectionRequest request = AttendanceCorrectionRequest.builder()
                .employee(employee)
                .attendanceDate(dto.getAttendanceDate())
                .type(parseCorrectionType(dto.getType()))
                .reason(dto.getReason().trim())
                .requestedCheckInTime(dto.getRequestedCheckInTime())
                .requestedCheckOutTime(dto.getRequestedCheckOutTime())
                .status(CorrectionStatus.PENDING)
                .build();

        AttendanceCorrectionRequest saved = correctionRepository.save(request);

        return mapToResponse(saved);
    }

    @Override
    public List<AttendanceCorrectionResponseDTO> getMyRequests(String employeeEmail) {
        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        return correctionRepository.findAllByEmployeeEmployeeIdOrderByCreatedAtDesc(employee.getEmployeeId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AttendanceCorrectionResponseDTO> getPendingRequests() {
        return correctionRepository.findAllByStatusOrderByCreatedAtDesc(CorrectionStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public AttendanceCorrectionResponseDTO approveRequest(Long requestId, CorrectionDecisionDTO dto, String adminEmail) {

        Employee admin = employeeRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        AttendanceCorrectionRequest request = correctionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Correction request not found"));

        if (request.getStatus() != CorrectionStatus.PENDING) {
            throw new BadRequestException("Only PENDING requests can be approved");
        }

        String note = dto != null && dto.getNote() != null ? dto.getNote().trim() : null;

        CorrectionType type = request.getType();
        Employee employee = request.getEmployee();
        LocalDate attendanceDate = request.getAttendanceDate();

        if (type == CorrectionType.MISSED_CHECK_IN) {
            if (request.getRequestedCheckInTime() == null) {
                throw new BadRequestException("Requested check-in time is required to approve MISSED_CHECK_IN");
            }

            ensureNoDuplicateAttendance(
                    employee.getEmployeeId(),
                    AttendanceType.IN,
                    attendanceDate
            );

            createAdminAttendanceRecord(
                    employee,
                    AttendanceType.IN,
                    request.getRequestedCheckInTime(),
                    "Correction approved: MISSED_CHECK_IN",
                    admin
            );

        } else if (type == CorrectionType.MISSED_CHECK_OUT) {
            if (request.getRequestedCheckOutTime() == null) {
                throw new BadRequestException("Requested check-out time is required to approve MISSED_CHECK_OUT");
            }

            ensureNoDuplicateAttendance(
                    employee.getEmployeeId(),
                    AttendanceType.OUT,
                    attendanceDate
            );

            createAdminAttendanceRecord(
                    employee,
                    AttendanceType.OUT,
                    request.getRequestedCheckOutTime(),
                    "Correction approved: MISSED_CHECK_OUT",
                    admin
            );
        }

        request.setStatus(CorrectionStatus.APPROVED);
        request.setAdminDecisionNote(note);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecidedBy(admin);

        AttendanceCorrectionRequest saved = correctionRepository.save(request);

        attendanceSummaryService.generateDailySummary(employee.getEmployeeId(), attendanceDate);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public AttendanceCorrectionResponseDTO rejectRequest(Long requestId, CorrectionDecisionDTO dto, String adminEmail) {

        Employee admin = employeeRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        AttendanceCorrectionRequest request = correctionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Correction request not found"));

        if (request.getStatus() != CorrectionStatus.PENDING) {
            throw new BadRequestException("Only PENDING requests can be rejected");
        }

        String note = dto != null && dto.getNote() != null ? dto.getNote().trim() : null;
        if (note == null || note.isEmpty()) {
            throw new BadRequestException("Decision note is required");
        }

        request.setStatus(CorrectionStatus.REJECTED);
        request.setAdminDecisionNote(note);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecidedBy(admin);

        AttendanceCorrectionRequest saved = correctionRepository.save(request);

        return mapToResponse(saved);
    }

    private void validateSubmitRequest(AttendanceCorrectionRequestDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Correction request is required");
        }

        if (dto.getAttendanceDate() == null) {
            throw new BadRequestException("Attendance date is required");
        }

        if (dto.getType() == null || dto.getType().trim().isEmpty()) {
            throw new BadRequestException("Correction type is required");
        }

        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new BadRequestException("Reason is required");
        }
    }

    private CorrectionType parseCorrectionType(String type) {
        try {
            return CorrectionType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("Invalid correction type");
        }
    }

    private void ensureNoDuplicateAttendance(
            Long employeeId,
            AttendanceType attendanceType,
            LocalDate attendanceDate
    ) {
        LocalDateTime dayStart = attendanceDate.atStartOfDay();
        LocalDateTime dayEnd = attendanceDate.plusDays(1).atStartOfDay().minusNanos(1);

        boolean alreadyExists =
                attendanceRecordRepository.existsByEmployeeEmployeeIdAndTypeAndStatusAndEventTimeBetween(
                        employeeId,
                        attendanceType,
                        AttendanceStatus.VALID,
                        dayStart,
                        dayEnd
                );

        if (alreadyExists) {
            throw new BadRequestException(
                    "A valid " + attendanceType.name() + " record already exists for " + attendanceDate
            );
        }
    }

    private void createAdminAttendanceRecord(
            Employee employee,
            AttendanceType attendanceType,
            LocalDateTime eventTime,
            String reason,
            Employee admin
    ) {
        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .branch(employee.getBranch())
                .type(attendanceType)
                .source(AttendanceSource.WEB)
                .status(AttendanceStatus.VALID)
                .eventTime(eventTime)
                .attendanceMark(AttendanceMark.NORMAL)
                .lateMinutes(0)
                .earlyLeaveMinutes(0)
                .overtimeMinutes(0)
                .reason(reason)
                .verified(true)
                .verificationMethod(VerificationMethod.ADMIN)
                .createdBy("ADMIN_CORRECTION")
                .build();

        AttendanceRecord saved = attendanceRecordRepository.save(record);

        AttendanceAudit audit = AttendanceAudit.builder()
                .attendance(saved)
                .action(AuditAction.CREATE)
                .doneByRole("ADMIN")
                .doneByUserId(admin.getEmployeeId())
                .note("Attendance record created from approved correction request")
                .build();

        attendanceAuditRepository.save(audit);
    }

    private AttendanceCorrectionResponseDTO mapToResponse(AttendanceCorrectionRequest request) {
        return AttendanceCorrectionResponseDTO.builder()
                .id(request.getId())
                .employeeId(request.getEmployee().getEmployeeId())
                .employeeName(request.getEmployee().getFullName())
                .attendanceDate(request.getAttendanceDate())
                .type(request.getType().name())
                .reason(request.getReason())
                .requestedCheckInTime(request.getRequestedCheckInTime())
                .requestedCheckOutTime(request.getRequestedCheckOutTime())
                .status(request.getStatus().name())
                .adminDecisionNote(request.getAdminDecisionNote())
                .decidedAt(request.getDecidedAt())
                .createdAt(request.getCreatedAt())
                .build();
    }
}