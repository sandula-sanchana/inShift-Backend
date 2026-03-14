package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.response.AttendanceDailySummaryResponseDTO;
import edu.ijse.inshiftbackend.entity.AttendanceDailySummary;
import edu.ijse.inshiftbackend.entity.AttendanceRecord;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.enums.AttendanceDayStatus;
import edu.ijse.inshiftbackend.entity.enums.AttendanceStatus;
import edu.ijse.inshiftbackend.entity.enums.AttendanceType;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.AttendanceDailySummaryRepository;
import edu.ijse.inshiftbackend.repository.AttendanceRecordRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.AttendanceSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceSummaryServiceImpl implements AttendanceSummaryService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceDailySummaryRepository summaryRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    @Override
    public AttendanceDailySummaryResponseDTO generateDailySummary(Long employeeId, LocalDate date) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (Boolean.FALSE.equals(employee.getActive())) {
            throw new ResourceNotFoundException("Employee is inactive");
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<AttendanceRecord> records =
                attendanceRecordRepository.findAllByEmployeeEmployeeIdAndEventTimeBetweenOrderByEventTimeAsc(
                        employeeId, start, end
                );

        AttendanceRecord firstIn = records.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.VALID && r.getType() == AttendanceType.IN)
                .findFirst()
                .orElse(null);

        AttendanceRecord lastOut = records.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.VALID)
                .filter(r -> r.getType() == AttendanceType.OUT)
                .filter(r -> firstIn != null && !r.getEventTime().isBefore(firstIn.getEventTime()))
                .reduce((first, second) -> second)
                .orElse(null);

        boolean present = firstIn != null;
        boolean completed = firstIn != null && lastOut != null;

        int lateMinutes = firstIn != null && firstIn.getLateMinutes() != null ? firstIn.getLateMinutes() : 0;
        int earlyLeaveMinutes = lastOut != null && lastOut.getEarlyLeaveMinutes() != null ? lastOut.getEarlyLeaveMinutes() : 0;
        int overtimeMinutes = lastOut != null && lastOut.getOvertimeMinutes() != null ? lastOut.getOvertimeMinutes() : 0;

        AttendanceDayStatus dayStatus = resolveDayStatus(firstIn, lastOut);

        AttendanceDailySummary summary = summaryRepository
                .findByEmployeeEmployeeIdAndSummaryDate(employeeId, date)
                .orElse(
                        AttendanceDailySummary.builder()
                                .employee(employee)
                                .branch(employee.getBranch())
                                .summaryDate(date)
                                .build()
                );

        summary.setFirstInTime(firstIn != null ? firstIn.getEventTime() : null);
        summary.setLastOutTime(lastOut != null ? lastOut.getEventTime() : null);
        summary.setPresent(present);
        summary.setCompleted(completed);
        summary.setLateMinutes(lateMinutes);
        summary.setEarlyLeaveMinutes(earlyLeaveMinutes);
        summary.setOvertimeMinutes(overtimeMinutes);
        summary.setDayStatus(dayStatus);

        AttendanceDailySummary saved = summaryRepository.save(summary);

        return mapToResponse(saved);
    }

    private AttendanceDayStatus resolveDayStatus(AttendanceRecord firstIn, AttendanceRecord lastOut) {
        if (firstIn == null) {
            return AttendanceDayStatus.ABSENT;
        }

        if (lastOut == null) {
            return AttendanceDayStatus.INCOMPLETE;
        }

        if (lastOut.getOvertimeMinutes() != null && lastOut.getOvertimeMinutes() > 0) {
            return AttendanceDayStatus.PRESENT_OVERTIME;
        }

        if (lastOut.getEarlyLeaveMinutes() != null && lastOut.getEarlyLeaveMinutes() > 0) {
            return AttendanceDayStatus.PRESENT_EARLY_LEAVE;
        }

        if (firstIn.getLateMinutes() != null && firstIn.getLateMinutes() > 0) {
            return AttendanceDayStatus.PRESENT_LATE;
        }

        return AttendanceDayStatus.PRESENT;
    }

    private AttendanceDailySummaryResponseDTO mapToResponse(AttendanceDailySummary saved) {
        return AttendanceDailySummaryResponseDTO.builder()
                .employeeId(saved.getEmployee().getEmployeeId())
                .employeeName(saved.getEmployee().getFullName())
                .branchId(saved.getBranch().getBranchId())
                .branchName(saved.getBranch().getBranchName())
                .summaryDate(saved.getSummaryDate())
                .firstInTime(saved.getFirstInTime())
                .lastOutTime(saved.getLastOutTime())
                .present(saved.getPresent())
                .completed(saved.getCompleted())
                .lateMinutes(saved.getLateMinutes())
                .earlyLeaveMinutes(saved.getEarlyLeaveMinutes())
                .overtimeMinutes(saved.getOvertimeMinutes())
                .dayStatus(saved.getDayStatus().name())
                .build();
    }

    @Override
    @Transactional
    public AttendanceDailySummaryResponseDTO getTodaySummaryByEmail(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        return generateDailySummary(employee.getEmployeeId(), LocalDate.now());
    }

    @Override
    @Transactional
    public AttendanceDailySummaryResponseDTO getSummaryByEmailAndDate(String email, LocalDate date) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        return generateDailySummary(employee.getEmployeeId(), date);
    }
}