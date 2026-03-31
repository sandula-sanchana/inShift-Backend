package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.response.EmployeeAiPatternContextDTO;
import edu.ijse.inshiftbackend.entity.*;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.*;
import edu.ijse.inshiftbackend.service.AiPatternContextBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiPatternContextBuilderServiceImpl implements AiPatternContextBuilderService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeBehaviorScoreRepository employeeBehaviorScoreRepository;
    private final PresenceCheckRepository presenceCheckRepository;
    private final AttendanceRiskScoreRepository attendanceRiskScoreRepository;
    private final AttendanceFlagRepository attendanceFlagRepository;

    @Override
    public EmployeeAiPatternContextDTO buildEmployeeContext(Long employeeId, int days) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        EmployeeBehaviorScore behaviorScore = employeeBehaviorScoreRepository
                .findByEmployeeEmployeeId(employeeId)
                .orElse(EmployeeBehaviorScore.builder()
                        .employee(employee)
                        .currentRiskScore(0)
                        .currentTrustScore(100)
                        .totalFlagsSeen(0)
                        .totalHighRiskDays(0)
                        .build());

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(Math.max(days - 1, 0));

        List<PresenceCheck> presenceChecks =
                presenceCheckRepository.findByEmployeeEmployeeIdOrderByCreatedAtDesc(employeeId);

        List<PresenceCheck> filteredPresenceChecks = presenceChecks.stream()
                .filter(pc -> pc.getCreatedAt() != null)
                .filter(pc -> !pc.getCreatedAt().toLocalDate().isBefore(startDate)
                        && !pc.getCreatedAt().toLocalDate().isAfter(endDate))
                .sorted(Comparator.comparing(PresenceCheck::getCreatedAt))
                .toList();

        List<AttendanceRiskScore> attendanceRiskScores =
                attendanceRiskScoreRepository
                        .findByEmployeeEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                                employeeId, startDate, endDate
                        );

        List<AttendanceFlag> attendanceFlags =
                attendanceFlagRepository
                        .findByEmployeeEmployeeIdAndAttendanceDateBetweenOrderByDetectedAtDesc(
                                employeeId, startDate, endDate
                        );

        List<Integer> delayValues = filteredPresenceChecks.stream()
                .map(PresenceCheck::getResponseDelaySeconds)
                .filter(Objects::nonNull)
                .toList();

        int respondedCount = (int) filteredPresenceChecks.stream()
                .filter(pc -> pc.getStatus() != null &&
                        ("RESPONDED".equals(pc.getStatus().name()) || "LATE".equals(pc.getStatus().name())))
                .count();

        int lateCount = (int) filteredPresenceChecks.stream()
                .filter(pc -> Boolean.TRUE.equals(pc.getLateResponse()))
                .count();

        int missedCount = (int) filteredPresenceChecks.stream()
                .filter(pc -> Boolean.TRUE.equals(pc.getMissedResponse())
                        || (pc.getStatus() != null && "MISSED".equals(pc.getStatus().name())))
                .count();

        int escalatedCount = (int) filteredPresenceChecks.stream()
                .filter(pc -> Boolean.TRUE.equals(pc.getEscalated()))
                .count();

        int companyPcResponses = (int) filteredPresenceChecks.stream()
                .filter(pc -> pc.getResponseSource() != null && "COMPANY_PC".equals(pc.getResponseSource().name()))
                .count();

        int mobileResponses = (int) filteredPresenceChecks.stream()
                .filter(pc -> pc.getResponseSource() != null && "MOBILE_GPS".equals(pc.getResponseSource().name()))
                .count();

        int averageDelay = delayValues.isEmpty() ? 0 :
                (int) Math.round(delayValues.stream().mapToInt(Integer::intValue).average().orElse(0));

        int maxDelay = delayValues.stream().mapToInt(Integer::intValue).max().orElse(0);
        int minDelay = delayValues.stream().mapToInt(Integer::intValue).min().orElse(0);

        int attendanceHighRiskDays = (int) attendanceRiskScores.stream()
                .filter(score -> Boolean.TRUE.equals(score.getHighRisk()))
                .count();

        int averageAttendanceRiskScore = attendanceRiskScores.isEmpty() ? 0 :
                (int) Math.round(attendanceRiskScores.stream()
                        .mapToInt(score -> score.getRiskScore() != null ? score.getRiskScore() : 0)
                        .average()
                        .orElse(0));

        int maxAttendanceRiskScore = attendanceRiskScores.stream()
                .map(AttendanceRiskScore::getRiskScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        List<String> attendanceTimeline = buildAttendanceTimeline(attendanceRiskScores, attendanceFlags);
        List<String> presenceTimeline = buildPresenceTimeline(filteredPresenceChecks);

        List<Integer> dailyRiskScores = attendanceRiskScores.stream()
                .map(AttendanceRiskScore::getRiskScore)
                .toList();

        List<String> dailyRiskDates = attendanceRiskScores.stream()
                .map(score -> score.getAttendanceDate().toString())
                .toList();

        List<String> summaryFacts = buildSummaryFacts(
                filteredPresenceChecks.size(),
                respondedCount,
                lateCount,
                missedCount,
                escalatedCount,
                attendanceFlags.size(),
                attendanceHighRiskDays,
                averageDelay,
                averageAttendanceRiskScore
        );

        return EmployeeAiPatternContextDTO.builder()
                .employeeId(employeeId)
                .employeeName(employee.getFullName())
                .windowDays(days)
                .currentRiskScore(valueOrZero(behaviorScore.getCurrentRiskScore()))
                .currentTrustScore(valueOrZero(behaviorScore.getCurrentTrustScore()))
                .currentRiskLevel(behaviorScore.getCurrentRiskLevel() != null
                        ? behaviorScore.getCurrentRiskLevel().name() : "LOW")
                .totalFlagsSeen(valueOrZero(behaviorScore.getTotalFlagsSeen()))
                .totalHighRiskDays(valueOrZero(behaviorScore.getTotalHighRiskDays()))
                .totalPresenceChecks(filteredPresenceChecks.size())
                .respondedCount(respondedCount)
                .lateCount(lateCount)
                .missedCount(missedCount)
                .escalatedCount(escalatedCount)
                .averageResponseDelaySeconds(averageDelay)
                .maxResponseDelaySeconds(maxDelay)
                .minResponseDelaySeconds(minDelay)
                .companyPcResponses(companyPcResponses)
                .mobileResponses(mobileResponses)
                .attendanceFlagCount(attendanceFlags.size())
                .attendanceHighRiskDays(attendanceHighRiskDays)
                .averageAttendanceRiskScore(averageAttendanceRiskScore)
                .maxAttendanceRiskScore(maxAttendanceRiskScore)
                .attendanceTimeline(attendanceTimeline)
                .presenceTimeline(presenceTimeline)
                .dailyRiskScores(dailyRiskScores)
                .dailyRiskDates(dailyRiskDates)
                .summaryFacts(summaryFacts)
                .build();
    }

    @Override
    public List<EmployeeAiPatternContextDTO> buildAllEmployeeContexts(int days) {
        return employeeRepository.findAllByActiveTrue().stream()
                .map(emp -> buildEmployeeContext(emp.getEmployeeId(), days))
                .filter(this::hasAnyUsefulSignal)
                .toList();
    }

    private boolean hasAnyUsefulSignal(EmployeeAiPatternContextDTO context) {
        return context.getTotalPresenceChecks() > 0
                || context.getAttendanceFlagCount() > 0
                || context.getAttendanceHighRiskDays() > 0
                || !context.getDailyRiskScores().isEmpty();
    }

    private List<String> buildAttendanceTimeline(
            List<AttendanceRiskScore> scores,
            List<AttendanceFlag> flags
    ) {
        Map<LocalDate, List<AttendanceFlag>> flagsByDate = flags.stream()
                .filter(flag -> flag.getAttendanceDate() != null)
                .collect(Collectors.groupingBy(AttendanceFlag::getAttendanceDate));

        List<String> lines = new ArrayList<>();

        for (AttendanceRiskScore score : scores) {
            List<String> flagTexts = flagsByDate
                    .getOrDefault(score.getAttendanceDate(), Collections.emptyList())
                    .stream()
                    .map(flag -> {
                        String type = flag.getFlagType() != null ? flag.getFlagType().name() : "UNKNOWN";
                        String severity = flag.getSeverity() != null ? flag.getSeverity().name() : "UNKNOWN";
                        return type + "(" + severity + ")";
                    })
                    .toList();

            lines.add(String.format(
                    "%s | riskScore=%s | trustScore=%s | totalFlags=%s | highRisk=%s | requiresReview=%s | flags=%s",
                    score.getAttendanceDate(),
                    score.getRiskScore(),
                    score.getTrustScore(),
                    score.getTotalFlags(),
                    score.getHighRisk(),
                    score.getRequiresReview(),
                    flagTexts
            ));
        }

        return lines;
    }

    private List<String> buildPresenceTimeline(List<PresenceCheck> checks) {
        return checks.stream()
                .map(pc -> String.format(
                        "%s | trigger=%s | riskLevel=%s | expected=%s | status=%s | responseSource=%s | delay=%s | late=%s | missed=%s | escalated=%s | escalationLevel=%s",
                        pc.getCreatedAt(),
                        pc.getTriggerReason(),
                        pc.getRiskLevel(),
                        pc.getSourceExpected(),
                        pc.getStatus(),
                        pc.getResponseSource(),
                        pc.getResponseDelaySeconds(),
                        pc.getLateResponse(),
                        pc.getMissedResponse(),
                        pc.getEscalated(),
                        pc.getEscalationLevel()
                ))
                .toList();
    }

    private List<String> buildSummaryFacts(
            int totalPresenceChecks,
            int respondedCount,
            int lateCount,
            int missedCount,
            int escalatedCount,
            int attendanceFlagCount,
            int attendanceHighRiskDays,
            int averageDelay,
            int averageAttendanceRiskScore
    ) {
        List<String> facts = new ArrayList<>();
        facts.add("Total presence checks in window: " + totalPresenceChecks);
        facts.add("Responded presence checks: " + respondedCount);
        facts.add("Late responses: " + lateCount);
        facts.add("Missed responses: " + missedCount);
        facts.add("Escalated presence checks: " + escalatedCount);
        facts.add("Attendance flags in window: " + attendanceFlagCount);
        facts.add("High-risk attendance days in window: " + attendanceHighRiskDays);
        facts.add("Average response delay seconds: " + averageDelay);
        facts.add("Average attendance risk score: " + averageAttendanceRiskScore);
        return facts;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}