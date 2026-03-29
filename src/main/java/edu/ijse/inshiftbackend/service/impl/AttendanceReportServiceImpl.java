package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.report.AttendanceReportFilterDTO;
import edu.ijse.inshiftbackend.dto.report.AttendanceReportResponseDTO;
import edu.ijse.inshiftbackend.dto.report.AttendanceReportRowDTO;
import edu.ijse.inshiftbackend.dto.report.AttendanceReportSummaryDTO;
import edu.ijse.inshiftbackend.entity.AttendanceRecord;
import edu.ijse.inshiftbackend.entity.enums.AttendanceSource;
import edu.ijse.inshiftbackend.entity.enums.AttendanceStatus;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.repository.AttendanceRecordRepository;
import edu.ijse.inshiftbackend.service.AttendanceReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceReportServiceImpl implements AttendanceReportService {

    private final AttendanceRecordRepository attendanceRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public AttendanceReportResponseDTO getAdminAttendanceReport(AttendanceReportFilterDTO filter) {
        if (filter == null) {
            throw new BadRequestException("Attendance report filter is required");
        }

        LocalDate from = filter.getDateFrom();
        LocalDate to = filter.getDateTo();

        if (from == null || to == null) {
            throw new BadRequestException("dateFrom and dateTo are required");
        }

        if (to.isBefore(from)) {
            throw new BadRequestException("dateTo cannot be before dateFrom");
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        AttendanceStatus statusFilter = parseStatus(filter.getStatus());
        AttendanceSource sourceFilter = parseSource(filter.getSource());

        List<AttendanceRecord> records = attendanceRecordRepository
                .findAllByEventTimeBetweenOrderByEventTimeDesc(start, end)
                .stream()
                .filter(record ->
                        filter.getEmployeeId() == null ||
                                record.getEmployee().getEmployeeId().equals(filter.getEmployeeId())
                )
                .filter(record ->
                        filter.getBranchId() == null ||
                                record.getBranch().getBranchId().equals(filter.getBranchId())
                )
                .filter(record ->
                        statusFilter == null || record.getStatus() == statusFilter
                )
                .filter(record ->
                        sourceFilter == null || record.getSource() == sourceFilter
                )
                .toList();

        List<AttendanceReportRowDTO> rows = records.stream()
                .map(this::mapRow)
                .toList();

        AttendanceReportSummaryDTO summary = buildSummary(records);

        log.info(
                "Attendance report generated. dateFrom={}, dateTo={}, employeeId={}, branchId={}, status={}, source={}, rowCount={}",
                from,
                to,
                filter.getEmployeeId(),
                filter.getBranchId(),
                filter.getStatus(),
                filter.getSource(),
                rows.size()
        );

        return AttendanceReportResponseDTO.builder()
                .summary(summary)
                .rows(rows)
                .build();
    }

    private AttendanceStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return AttendanceStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("Invalid attendance status filter");
        }
    }

    private AttendanceSource parseSource(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return AttendanceSource.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("Invalid attendance source filter");
        }
    }

    private AttendanceReportRowDTO mapRow(AttendanceRecord record) {
        return AttendanceReportRowDTO.builder()
                .attendanceId(record.getId())
                .employeeId(record.getEmployee().getEmployeeId())
                .employeeCode(record.getEmployee().getEmpCode())
                .employeeName(record.getEmployee().getFullName())
                .branchId(record.getBranch().getBranchId())
                .branchName(record.getBranch().getBranchName())
                .type(record.getType() != null ? record.getType().name() : null)
                .source(record.getSource() != null ? record.getSource().name() : null)
                .status(record.getStatus() != null ? record.getStatus().name() : null)
                .eventTime(record.getEventTime())
                .attendanceMark(record.getAttendanceMark() != null ? record.getAttendanceMark().name() : null)
                .lateMinutes(record.getLateMinutes())
                .earlyLeaveMinutes(record.getEarlyLeaveMinutes())
                .overtimeMinutes(record.getOvertimeMinutes())
                .verified(record.isVerified())
                .verificationMethod(record.getVerificationMethod() != null ? record.getVerificationMethod().name() : null)
                .decisionNote(record.getDecisionNote())
                .reason(record.getReason())
                .build();
    }

    private AttendanceReportSummaryDTO buildSummary(List<AttendanceRecord> records) {
        int total = records.size();
        int valid = 0;
        int pending = 0;
        int rejected = 0;
        int checkIn = 0;
        int checkOut = 0;
        int late = 0;
        int earlyLeave = 0;
        int overtime = 0;

        for (AttendanceRecord record : records) {
            if (record.getStatus() == AttendanceStatus.VALID) valid++;
            if (record.getStatus() == AttendanceStatus.PENDING) pending++;
            if (record.getStatus() == AttendanceStatus.REJECTED) rejected++;

            if (record.getType() != null) {
                switch (record.getType()) {
                    case IN -> checkIn++;
                    case OUT -> checkOut++;
                }
            }

            if (record.getLateMinutes() != null && record.getLateMinutes() > 0) late++;
            if (record.getEarlyLeaveMinutes() != null && record.getEarlyLeaveMinutes() > 0) earlyLeave++;
            if (record.getOvertimeMinutes() != null && record.getOvertimeMinutes() > 0) overtime++;
        }

        return AttendanceReportSummaryDTO.builder()
                .totalRecords(total)
                .validCount(valid)
                .pendingCount(pending)
                .rejectedCount(rejected)
                .checkInCount(checkIn)
                .checkOutCount(checkOut)
                .lateCount(late)
                .earlyLeaveCount(earlyLeave)
                .overtimeCount(overtime)
                .build();
    }
}