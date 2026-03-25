package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeAiRiskAnalysisResponseDTO {

    private EmployeeRiskAnalyticsDTO analytics;
    private AiRiskInsightDTO aiInsight;
}