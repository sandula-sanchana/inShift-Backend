package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeRiskAnalyticsDTO {

    private Long employeeId;
    private String employeeName;

    private Integer currentRiskScore;
    private Integer currentTrustScore;
    private String currentRiskLevel;

    private Integer totalFlagsSeen;
    private Integer totalHighRiskDays;

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

    private Integer recentAttendanceFlagCount;
    private Integer recentHighRiskAttendanceDays;

    private List<EmployeePresenceTrendPointDTO> recentPresenceTrend;
    private List<Integer> recentDailyRiskScores;
    private List<String> recentDailyRiskDates;
}