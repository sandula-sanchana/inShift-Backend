package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyAiScannerDashboardDTO {
    private CompanyAiScannerSummaryDTO summary;
    private CompanyAiOverviewDTO aiOverview;
    private List<SuspiciousEmployeeAiCardDTO> suspiciousEmployees;
    private List<PatternDistributionDTO> patternDistribution;
    private List<CompanyRiskTrendPointDTO> companyRiskTrend;
    private LocalDateTime generatedAt;
}