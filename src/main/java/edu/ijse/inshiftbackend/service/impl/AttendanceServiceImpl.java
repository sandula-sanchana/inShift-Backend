package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.AttendancePunchDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceResponseDTO;
import edu.ijse.inshiftbackend.entity.*;
import edu.ijse.inshiftbackend.entity.enums.*;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.*;
import edu.ijse.inshiftbackend.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final AttendanceAuditRepository auditRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public AttendanceResponseDTO punch(
            AttendancePunchDTO dto,
            AttendanceSource source,
            String email
    ) {

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (!employee.getActive()) {
            throw new BadRequestException("Inactive employee cannot mark attendance");
        }

        AttendanceType type = AttendanceType.valueOf(dto.getType());

        Optional<AttendanceRecord> last =
                attendanceRepository
                        .findTopByEmployeeEmployeeIdOrderByEventTimeDesc(employee.getEmployeeId());

        // 🔥 RULE 1: Prevent double IN
        if (type == AttendanceType.IN) {
            if (last.isPresent() && last.get().getType() == AttendanceType.IN) {
                throw new BadRequestException("Already checked in");
            }
        }

        // 🔥 RULE 2: Prevent OUT without IN
        if (type == AttendanceType.OUT) {
            if (last.isEmpty() || last.get().getType() != AttendanceType.IN) {
                throw new BadRequestException("Cannot check out without checking in first");
            }
        }

        // 🔥 Optional rule: require location for mobile
        if (source == AttendanceSource.MOBILE) {
            if (dto.getLat() == null || dto.getLng() == null) {
                throw new BadRequestException("Location required for mobile attendance");
            }
        }

        AttendanceStatus status =
                (source == AttendanceSource.WEB)
                        ? AttendanceStatus.PENDING
                        : AttendanceStatus.VALID;

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
                .verified(source != AttendanceSource.WEB)
                .verificationMethod(source != AttendanceSource.WEB
                        ? VerificationMethod.NONE
                        : VerificationMethod.ADMIN)
                .createdBy("EMPLOYEE")
                .build();

        AttendanceRecord saved = attendanceRepository.save(record);

        // 🔥 Save Audit
        AttendanceAudit audit = AttendanceAudit.builder()
                .attendance(saved)
                .action(AuditAction.CREATE)
                .doneByRole("EMPLOYEE")
                .doneByUserId(employee.getEmployeeId())
                .note("Attendance punch created")
                .build();

        auditRepository.save(audit);

        return AttendanceResponseDTO.builder()
                .id(saved.getId())
                .employeeId(employee.getEmployeeId())
                .employeeName(employee.getFullName())
                .branchId(employee.getBranch().getBranchId())
                .branchName(employee.getBranch().getBranchName())
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