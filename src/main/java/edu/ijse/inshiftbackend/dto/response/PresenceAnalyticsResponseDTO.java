package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceAnalyticsResponseDTO {

    private Long employeeId;
    private String employeeName;

    private Integer totalPresenceChecks;
    private Integer respondedCount;
    private Integer lateCount;
    private Integer missedCount;
    private Integer escalatedCount;

    private Integer averageResponseDelaySeconds;
    private Integer maxResponseDelaySeconds;
    private Integer minResponseDelaySeconds;

    private Integer companyPcResponses;
    private Integer mobileResponses;

    private List<String> riskTrendDates;
    private List<Integer> riskTrendScores;

    private List<PresenceAnalyticsPointDTO> delayTrend;
    private List<PresenceAnalyticsPointDTO> recentPresenceChecks;
}