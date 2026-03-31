package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyAiScannerSummaryDTO {
    private Integer totalEmployeesScanned;
    private Integer suspiciousEmployeeCount;
    private Integer highPriorityCount;
    private Integer mediumPriorityCount;
    private Integer lowPriorityCount;
}