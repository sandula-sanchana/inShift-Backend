package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspiciousEmployeeAiCardDTO {
    private Long employeeId;
    private String employeeName;

    private Integer currentRiskScore;
    private Integer currentTrustScore;
    private String currentRiskLevel;

    private String summary;
    private List<String> suspiciousPatterns;
    private List<String> whySuspicious;
    private List<String> recommendedActions;

    private String monitoringPriority;
    private String confidence;

    private Integer totalPresenceChecks;
    private Integer lateCount;
    private Integer missedCount;
    private Integer escalatedCount;
    private Integer attendanceFlagCount;
    private Integer attendanceHighRiskDays;

    private List<Integer> recentRiskScores;
    private List<String> recentRiskDates;
}