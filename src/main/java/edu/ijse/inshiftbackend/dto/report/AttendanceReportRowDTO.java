package edu.ijse.inshiftbackend.dto.report;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceReportRowDTO {

    private Long attendanceId;

    private Long employeeId;
    private String employeeCode;
    private String employeeName;

    private Long branchId;
    private String branchName;

    private String type;
    private String source;
    private String status;

    private LocalDateTime eventTime;

    private String attendanceMark;

    private Integer lateMinutes;
    private Integer earlyLeaveMinutes;
    private Integer overtimeMinutes;

    private Boolean verified;
    private String verificationMethod;
    private String decisionNote;
    private String reason;
}