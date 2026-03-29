package edu.ijse.inshiftbackend.dto.report;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceReportSummaryDTO {

    private int totalRecords;
    private int validCount;
    private int pendingCount;
    private int rejectedCount;

    private int checkInCount;
    private int checkOutCount;

    private int lateCount;
    private int earlyLeaveCount;
    private int overtimeCount;
}