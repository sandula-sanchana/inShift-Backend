package edu.ijse.inshiftbackend.dto.report;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceReportFilterDTO {

    private LocalDate dateFrom;
    private LocalDate dateTo;

    private Long employeeId;
    private Long branchId;

    private String status;
    private String source;
}