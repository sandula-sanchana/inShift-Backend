package edu.ijse.inshiftbackend.dto.report;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceReportResponseDTO {

    private AttendanceReportSummaryDTO summary;
    private List<AttendanceReportRowDTO> rows;
}