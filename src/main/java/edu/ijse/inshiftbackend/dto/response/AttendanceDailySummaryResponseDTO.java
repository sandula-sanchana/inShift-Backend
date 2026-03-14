package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDailySummaryResponseDTO {
    private Long employeeId;
    private String employeeName;
    private Long branchId;
    private String branchName;
    private LocalDate summaryDate;
    private LocalDateTime firstInTime;
    private LocalDateTime lastOutTime;
    private Boolean present;
    private Boolean completed;
    private Integer lateMinutes;
    private Integer earlyLeaveMinutes;
    private Integer overtimeMinutes;
    private String dayStatus;
}