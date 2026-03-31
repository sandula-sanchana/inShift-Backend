package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeAiScannerDetailDTO {
    private EmployeeAiPatternContextDTO context;
    private EmployeeAiPatternInsightDTO insight;
}