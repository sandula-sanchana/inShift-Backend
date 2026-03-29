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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAdminAttendanceReportCsv(AttendanceReportFilterDTO filter) {
        AttendanceReportResponseDTO report = getAdminAttendanceReport(filter);
        List<AttendanceReportRowDTO> rows = report.getRows();

        StringBuilder csv = new StringBuilder();

        csv.append("Attendance ID,Employee ID,Employee Code,Employee Name,Branch ID,Branch Name,Type,Source,Status,Event Time,Attendance Mark,Late Minutes,Early Leave Minutes,Overtime Minutes,Verified,Verification Method,Decision Note,Reason\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (AttendanceReportRowDTO row : rows) {
            csv.append(csvValue(row.getAttendanceId())).append(",");
            csv.append(csvValue(row.getEmployeeId())).append(",");
            csv.append(csvValue(row.getEmployeeCode())).append(",");
            csv.append(csvValue(row.getEmployeeName())).append(",");
            csv.append(csvValue(row.getBranchId())).append(",");
            csv.append(csvValue(row.getBranchName())).append(",");
            csv.append(csvValue(row.getType())).append(",");
            csv.append(csvValue(row.getSource())).append(",");
            csv.append(csvValue(row.getStatus())).append(",");
            csv.append(csvValue(row.getEventTime() != null ? row.getEventTime().format(formatter) : "")).append(",");
            csv.append(csvValue(row.getAttendanceMark())).append(",");
            csv.append(csvValue(row.getLateMinutes())).append(",");
            csv.append(csvValue(row.getEarlyLeaveMinutes())).append(",");
            csv.append(csvValue(row.getOvertimeMinutes())).append(",");
            csv.append(csvValue(row.getVerified())).append(",");
            csv.append(csvValue(row.getVerificationMethod())).append(",");
            csv.append(csvValue(row.getDecisionNote())).append(",");
            csv.append(csvValue(row.getReason())).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }

        String text = String.valueOf(value);
        text = text.replace("\"", "\"\"");
        return "\"" + text + "\"";
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