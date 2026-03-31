package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeAiPatternContextDTO {
    private Long employeeId;
    private String employeeName;
    private Integer windowDays;

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

    private Integer attendanceFlagCount;
    private Integer attendanceHighRiskDays;
    private Integer averageAttendanceRiskScore;
    private Integer maxAttendanceRiskScore;

    private List<String> attendanceTimeline;
    private List<String> presenceTimeline;
    private List<Integer> dailyRiskScores;
    private List<String> dailyRiskDates;
    private List<String> summaryFacts;
}