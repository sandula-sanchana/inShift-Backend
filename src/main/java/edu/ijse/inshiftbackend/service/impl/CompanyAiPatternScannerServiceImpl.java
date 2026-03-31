package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.response.*;
import edu.ijse.inshiftbackend.service.AiPatternContextBuilderService;
import edu.ijse.inshiftbackend.service.AiPatternInsightService;
import edu.ijse.inshiftbackend.service.CompanyAiPatternScannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyAiPatternScannerServiceImpl implements CompanyAiPatternScannerService {

    private final AiPatternContextBuilderService aiPatternContextBuilderService;
    private final AiPatternInsightService aiPatternInsightService;

    @Override
    public CompanyAiScannerDashboardDTO runCompanyScan(int days) {
        List<EmployeeAiPatternContextDTO> contexts =
                aiPatternContextBuilderService.buildAllEmployeeContexts(days);

        List<SuspiciousEmployeeAiCardDTO> suspiciousEmployees = contexts.stream()
                .map(context -> {
                    EmployeeAiPatternInsightDTO insight =
                            aiPatternInsightService.analyzeEmployee(context);

                    return SuspiciousEmployeeAiCardDTO.builder()
                            .employeeId(context.getEmployeeId())
                            .employeeName(context.getEmployeeName())
                            .currentRiskScore(context.getCurrentRiskScore())
                            .currentTrustScore(context.getCurrentTrustScore())
                            .currentRiskLevel(context.getCurrentRiskLevel())
                            .summary(insight.getSummary())
                            .suspiciousPatterns(insight.getSuspiciousPatterns())
                            .whySuspicious(insight.getWhySuspicious())
                            .recommendedActions(insight.getRecommendedActions())
                            .monitoringPriority(insight.getMonitoringPriority())
                            .confidence(insight.getConfidence())
                            .totalPresenceChecks(context.getTotalPresenceChecks())
                            .lateCount(context.getLateCount())
                            .missedCount(context.getMissedCount())
                            .escalatedCount(context.getEscalatedCount())
                            .attendanceFlagCount(context.getAttendanceFlagCount())
                            .attendanceHighRiskDays(context.getAttendanceHighRiskDays())
                            .recentRiskScores(context.getDailyRiskScores())
                            .recentRiskDates(context.getDailyRiskDates())
                            .build();
                })
                .filter(this::shouldSurface)
                .sorted(this::compareCards)
                .toList();

        CompanyAiOverviewDTO overview =
                aiPatternInsightService.analyzeCompany(contexts, suspiciousEmployees);

        return CompanyAiScannerDashboardDTO.builder()
                .summary(buildSummary(contexts, suspiciousEmployees))
                .aiOverview(overview)
                .suspiciousEmployees(suspiciousEmployees)
                .patternDistribution(buildPatternDistribution(suspiciousEmployees))
                .companyRiskTrend(buildCompanyRiskTrend(contexts))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public EmployeeAiScannerDetailDTO getEmployeeScannerDetail(Long employeeId, int days) {
        EmployeeAiPatternContextDTO context =
                aiPatternContextBuilderService.buildEmployeeContext(employeeId, days);

        EmployeeAiPatternInsightDTO insight =
                aiPatternInsightService.analyzeEmployee(context);

        return EmployeeAiScannerDetailDTO.builder()
                .context(context)
                .insight(insight)
                .build();
    }

    private boolean shouldSurface(SuspiciousEmployeeAiCardDTO card) {
        if (card.getMonitoringPriority() == null) return false;
        return "HIGH".equalsIgnoreCase(card.getMonitoringPriority())
                || "MEDIUM".equalsIgnoreCase(card.getMonitoringPriority())
                || (card.getSuspiciousPatterns() != null && !card.getSuspiciousPatterns().isEmpty());
    }

    private int compareCards(SuspiciousEmployeeAiCardDTO a, SuspiciousEmployeeAiCardDTO b) {
        int priorityCompare = Integer.compare(priorityRank(b.getMonitoringPriority()), priorityRank(a.getMonitoringPriority()));
        if (priorityCompare != 0) return priorityCompare;

        int confidenceCompare = Integer.compare(confidenceRank(b.getConfidence()), confidenceRank(a.getConfidence()));
        if (confidenceCompare != 0) return confidenceCompare;

        return Integer.compare(
                b.getCurrentRiskScore() != null ? b.getCurrentRiskScore() : 0,
                a.getCurrentRiskScore() != null ? a.getCurrentRiskScore() : 0
        );
    }

    private int priorityRank(String value) {
        if ("HIGH".equalsIgnoreCase(value)) return 3;
        if ("MEDIUM".equalsIgnoreCase(value)) return 2;
        return 1;
    }

    private int confidenceRank(String value) {
        if ("HIGH".equalsIgnoreCase(value)) return 3;
        if ("MEDIUM".equalsIgnoreCase(value)) return 2;
        return 1;
    }

    private CompanyAiScannerSummaryDTO buildSummary(
            List<EmployeeAiPatternContextDTO> contexts,
            List<SuspiciousEmployeeAiCardDTO> suspiciousEmployees
    ) {
        int high = (int) suspiciousEmployees.stream()
                .filter(emp -> "HIGH".equalsIgnoreCase(emp.getMonitoringPriority()))
                .count();

        int medium = (int) suspiciousEmployees.stream()
                .filter(emp -> "MEDIUM".equalsIgnoreCase(emp.getMonitoringPriority()))
                .count();

        int low = (int) suspiciousEmployees.stream()
                .filter(emp -> "LOW".equalsIgnoreCase(emp.getMonitoringPriority()))
                .count();

        return CompanyAiScannerSummaryDTO.builder()
                .totalEmployeesScanned(contexts.size())
                .suspiciousEmployeeCount(suspiciousEmployees.size())
                .highPriorityCount(high)
                .mediumPriorityCount(medium)
                .lowPriorityCount(low)
                .build();
    }

    private List<PatternDistributionDTO> buildPatternDistribution(List<SuspiciousEmployeeAiCardDTO> employees) {
        Map<String, Integer> counter = new HashMap<>();

        for (SuspiciousEmployeeAiCardDTO employee : employees) {
            if (employee.getSuspiciousPatterns() == null) continue;
            for (String pattern : employee.getSuspiciousPatterns()) {
                counter.put(pattern, counter.getOrDefault(pattern, 0) + 1);
            }
        }

        return counter.entrySet().stream()
                .map(entry -> PatternDistributionDTO.builder()
                        .label(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .limit(10)
                .toList();
    }

    private List<CompanyRiskTrendPointDTO> buildCompanyRiskTrend(List<EmployeeAiPatternContextDTO> contexts) {
        Map<String, List<Integer>> byDate = new HashMap<>();

        for (EmployeeAiPatternContextDTO context : contexts) {
            List<String> dates = context.getDailyRiskDates();
            List<Integer> scores = context.getDailyRiskScores();

            int size = Math.min(dates.size(), scores.size());
            for (int i = 0; i < size; i++) {
                byDate.computeIfAbsent(dates.get(i), k -> new ArrayList<>()).add(scores.get(i));
            }
        }

        return byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<Integer> values = entry.getValue();
                    double avg = values.stream().mapToInt(Integer::intValue).average().orElse(0);
                    int highRiskCount = (int) values.stream().filter(v -> v >= 70).count();

                    return CompanyRiskTrendPointDTO.builder()
                            .date(entry.getKey())
                            .averageRiskScore(avg)
                            .highRiskCount(highRiskCount)
                            .build();
                })
                .collect(Collectors.toList());
    }
}