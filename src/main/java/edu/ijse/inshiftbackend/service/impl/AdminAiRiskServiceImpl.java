package edu.ijse.inshiftbackend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ijse.inshiftbackend.dto.response.*;
import edu.ijse.inshiftbackend.entity.*;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.*;
import edu.ijse.inshiftbackend.service.AdminAiRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAiRiskServiceImpl implements AdminAiRiskService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeBehaviorScoreRepository employeeBehaviorScoreRepository;
    private final PresenceCheckRepository presenceCheckRepository;
    private final AttendanceRiskScoreRepository attendanceRiskScoreRepository;
    private final AttendanceFlagRepository attendanceFlagRepository;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.openrouter.api-key:}")
    private String openRouterApiKey;

    @Value("${ai.openrouter.url:https://openrouter.ai/api/v1/chat/completions}")
    private String openRouterUrl;

    @Value("${ai.openrouter.model:google/gemini-3-flash-preview}")
    private String openRouterModel;

    @Override
    public EmployeeAiRiskAnalysisResponseDTO getEmployeeAiRiskAnalysis(Long employeeId) {
        if (employeeId == null) {
            throw new BadRequestException("Employee id is required");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        EmployeeRiskAnalyticsDTO analytics = buildAnalytics(employee);
        AiRiskInsightDTO aiInsight = buildAiInsight(employee, analytics);

        return EmployeeAiRiskAnalysisResponseDTO.builder()
                .analytics(analytics)
                .aiInsight(aiInsight)
                .build();
    }

    private EmployeeRiskAnalyticsDTO buildAnalytics(Employee employee) {
        Long employeeId = employee.getEmployeeId();

        EmployeeBehaviorScore behaviorScore = employeeBehaviorScoreRepository
                .findByEmployeeEmployeeId(employeeId)
                .orElse(
                        EmployeeBehaviorScore.builder()
                                .employee(employee)
                                .currentRiskScore(0)
                                .currentTrustScore(100)
                                .totalFlagsSeen(0)
                                .totalHighRiskDays(0)
                                .build()
                );

        List<PresenceCheck> allPresenceChecks =
                presenceCheckRepository.findByEmployeeEmployeeIdOrderByCreatedAtDesc(employeeId);

        List<PresenceCheck> recentPresenceChecks = allPresenceChecks.stream()
                .limit(12)
                .toList();

        int totalPresenceChecks = allPresenceChecks.size();
        int respondedCount = (int) allPresenceChecks.stream()
                .filter(pc -> pc.getStatus() != null &&
                        (pc.getStatus().name().equals("RESPONDED") || pc.getStatus().name().equals("LATE")))
                .count();

        int lateCount = (int) allPresenceChecks.stream()
                .filter(pc -> Boolean.TRUE.equals(pc.getLateResponse()))
                .count();

        int missedCount = (int) allPresenceChecks.stream()
                .filter(pc -> Boolean.TRUE.equals(pc.getMissedResponse()) || (pc.getStatus() != null && pc.getStatus().name().equals("MISSED")))
                .count();

        int escalatedCount = (int) allPresenceChecks.stream()
                .filter(pc -> Boolean.TRUE.equals(pc.getEscalated()))
                .count();

        List<Integer> delayValues = allPresenceChecks.stream()
                .map(PresenceCheck::getResponseDelaySeconds)
                .filter(Objects::nonNull)
                .toList();

        int averageDelay = delayValues.isEmpty()
                ? 0
                : (int) Math.round(delayValues.stream().mapToInt(Integer::intValue).average().orElse(0));

        int maxDelay = delayValues.stream().mapToInt(Integer::intValue).max().orElse(0);
        int minDelay = delayValues.stream().mapToInt(Integer::intValue).min().orElse(0);

        int companyPcResponses = (int) allPresenceChecks.stream()
                .filter(pc -> pc.getResponseSource() != null && pc.getResponseSource().name().equals("COMPANY_PC"))
                .count();

        int mobileResponses = (int) allPresenceChecks.stream()
                .filter(pc -> pc.getResponseSource() != null && pc.getResponseSource().name().equals("MOBILE_GPS"))
                .count();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);

        List<AttendanceFlag> recentFlags =
                attendanceFlagRepository.findByEmployeeEmployeeIdAndAttendanceDateBetweenOrderByDetectedAtDesc(
                        employeeId,
                        startDate,
                        endDate
                );

        List<AttendanceRiskScore> recentDailyScores =
                attendanceRiskScoreRepository.findByEmployeeEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        employeeId,
                        startDate,
                        endDate
                );

        int recentHighRiskAttendanceDays = (int) recentDailyScores.stream()
                .filter(score -> Boolean.TRUE.equals(score.getHighRisk()))
                .count();

        List<EmployeePresenceTrendPointDTO> trend = recentPresenceChecks.stream()
                .sorted(Comparator.comparing(PresenceCheck::getCreatedAt))
                .map(pc -> EmployeePresenceTrendPointDTO.builder()
                        .presenceCheckId(pc.getId())
                        .createdAt(pc.getCreatedAt())
                        .respondedAt(pc.getRespondedAt())
                        .status(pc.getStatus() != null ? pc.getStatus().name() : null)
                        .triggerReason(pc.getTriggerReason() != null ? pc.getTriggerReason().name() : null)
                        .riskLevel(pc.getRiskLevel() != null ? pc.getRiskLevel().name() : null)
                        .responseSource(pc.getResponseSource() != null ? pc.getResponseSource().name() : null)
                        .responseDelaySeconds(pc.getResponseDelaySeconds())
                        .lateResponse(pc.getLateResponse())
                        .missedResponse(pc.getMissedResponse())
                        .escalated(pc.getEscalated())
                        .build())
                .toList();

        List<Integer> recentDailyRiskScores = recentDailyScores.stream()
                .map(AttendanceRiskScore::getRiskScore)
                .toList();

        List<String> recentDailyRiskDates = recentDailyScores.stream()
                .map(score -> score.getAttendanceDate().toString())
                .toList();

        return EmployeeRiskAnalyticsDTO.builder()
                .employeeId(employeeId)
                .employeeName(employee.getFullName())
                .currentRiskScore(valueOrZero(behaviorScore.getCurrentRiskScore()))
                .currentTrustScore(valueOrZero(behaviorScore.getCurrentTrustScore()))
                .currentRiskLevel(behaviorScore.getCurrentRiskLevel() != null ? behaviorScore.getCurrentRiskLevel().name() : "LOW")
                .totalFlagsSeen(valueOrZero(behaviorScore.getTotalFlagsSeen()))
                .totalHighRiskDays(valueOrZero(behaviorScore.getTotalHighRiskDays()))
                .totalPresenceChecks(totalPresenceChecks)
                .respondedCount(respondedCount)
                .lateCount(lateCount)
                .missedCount(missedCount)
                .escalatedCount(escalatedCount)
                .averageResponseDelaySeconds(averageDelay)
                .maxResponseDelaySeconds(maxDelay)
                .minResponseDelaySeconds(minDelay)
                .companyPcResponses(companyPcResponses)
                .mobileResponses(mobileResponses)
                .recentAttendanceFlagCount(recentFlags.size())
                .recentHighRiskAttendanceDays(recentHighRiskAttendanceDays)
                .recentPresenceTrend(trend)
                .recentDailyRiskScores(recentDailyRiskScores)
                .recentDailyRiskDates(recentDailyRiskDates)
                .build();
    }

    private AiRiskInsightDTO buildAiInsight(Employee employee, EmployeeRiskAnalyticsDTO analytics) {
        if (openRouterApiKey == null || openRouterApiKey.isBlank()) {
            return fallbackInsight("AI key is not configured", analytics);
        }

        try {
            String prompt = buildPrompt(employee, analytics);

            Map<String, Object> body = new HashMap<>();
            body.put("model", openRouterModel);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content",
                    "You are an HR security and attendance intelligence copilot. " +
                            "Return STRICT JSON only with keys: summary, keyPatterns, recommendedActions, monitoringPriority. " +
                            "keyPatterns and recommendedActions must be arrays of short strings. " +
                            "monitoringPriority must be one of LOW, MEDIUM, HIGH."
            ));
            messages.add(Map.of("role", "user", "content", prompt));

            body.put("messages", messages);
            body.put("max_tokens", 250);
            body.put("temperature", 0.3);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openRouterApiKey);
            headers.set("HTTP-Referer", "http://localhost:8080");
            headers.set("X-Title", "InShift AI Risk Copilot");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(openRouterUrl, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode contentNode = root.path("choices").get(0).path("message").path("content");

            String content = contentNode.asText();
            JsonNode parsed = objectMapper.readTree(content);

            return AiRiskInsightDTO.builder()
                    .summary(parsed.path("summary").asText("No summary generated"))
                    .keyPatterns(readStringArray(parsed.path("keyPatterns")))
                    .recommendedActions(readStringArray(parsed.path("recommendedActions")))
                    .monitoringPriority(parsed.path("monitoringPriority").asText("MEDIUM"))
                    .generatedAt(LocalDateTime.now())
                    .model(openRouterModel)
                    .build();

        } catch (Exception e) {
            return fallbackInsight("AI generation failed: " + e.getMessage(), analytics);
        }
    }

    private String buildPrompt(Employee employee, EmployeeRiskAnalyticsDTO analytics) {
        return """
                Analyze this employee's attendance and presence behavior and return a concise admin-facing risk insight.

                Employee:
                - Employee ID: %d
                - Employee Name: %s

                Current Behavior Score:
                - Current Risk Score: %d
                - Current Trust Score: %d
                - Current Risk Level: %s
                - Total Flags Seen: %d
                - Total High Risk Days: %d

                Presence Metrics:
                - Total Presence Checks: %d
                - Responded Count: %d
                - Late Count: %d
                - Missed Count: %d
                - Escalated Count: %d
                - Average Response Delay Seconds: %d
                - Max Response Delay Seconds: %d
                - Min Response Delay Seconds: %d
                - Company PC Responses: %d
                - Mobile Responses: %d

                Recent Attendance Intelligence:
                - Recent Attendance Flag Count (30d): %d
                - Recent High Risk Attendance Days (30d): %d

                Recent Presence Trend:
                %s

                Recent Daily Risk Scores:
                %s

                Return JSON only.
                """.formatted(
                analytics.getEmployeeId(),
                analytics.getEmployeeName(),
                analytics.getCurrentRiskScore(),
                analytics.getCurrentTrustScore(),
                analytics.getCurrentRiskLevel(),
                analytics.getTotalFlagsSeen(),
                analytics.getTotalHighRiskDays(),
                analytics.getTotalPresenceChecks(),
                analytics.getRespondedCount(),
                analytics.getLateCount(),
                analytics.getMissedCount(),
                analytics.getEscalatedCount(),
                analytics.getAverageResponseDelaySeconds(),
                analytics.getMaxResponseDelaySeconds(),
                analytics.getMinResponseDelaySeconds(),
                analytics.getCompanyPcResponses(),
                analytics.getMobileResponses(),
                analytics.getRecentAttendanceFlagCount(),
                analytics.getRecentHighRiskAttendanceDays(),
                buildTrendText(analytics.getRecentPresenceTrend()),
                buildRiskSeriesText(analytics.getRecentDailyRiskDates(), analytics.getRecentDailyRiskScores())
        );
    }

    private String buildTrendText(List<EmployeePresenceTrendPointDTO> trend) {
        if (trend == null || trend.isEmpty()) {
            return "No recent presence checks.";
        }

        return trend.stream()
                .map(point -> String.format(
                        "- %s | status=%s | delay=%s | source=%s | late=%s | missed=%s | escalated=%s",
                        point.getCreatedAt(),
                        point.getStatus(),
                        point.getResponseDelaySeconds(),
                        point.getResponseSource(),
                        point.getLateResponse(),
                        point.getMissedResponse(),
                        point.getEscalated()
                ))
                .collect(Collectors.joining("\n"));
    }

    private String buildRiskSeriesText(List<String> dates, List<Integer> scores) {
        if (dates == null || scores == null || dates.isEmpty() || scores.isEmpty()) {
            return "No daily risk scores available.";
        }

        StringBuilder sb = new StringBuilder();
        int count = Math.min(dates.size(), scores.size());

        for (int i = 0; i < count; i++) {
            sb.append("- ").append(dates.get(i)).append(": ").append(scores.get(i)).append("\n");
        }

        return sb.toString();
    }

    private AiRiskInsightDTO fallbackInsight(String reason, EmployeeRiskAnalyticsDTO analytics) {
        List<String> patterns = new ArrayList<>();

        if (analytics.getMissedCount() > 0) {
            patterns.add("Missed presence checks detected");
        }
        if (analytics.getLateCount() > 0) {
            patterns.add("Repeated late responses detected");
        }
        if (analytics.getEscalatedCount() > 0) {
            patterns.add("Escalated verification incidents detected");
        }
        if (analytics.getRecentAttendanceFlagCount() > 0) {
            patterns.add("Recent attendance flags present");
        }
        if (analytics.getAverageResponseDelaySeconds() > 120) {
            patterns.add("Slow average response time");
        }
        if (patterns.isEmpty()) {
            patterns.add("Behavior currently appears stable");
        }

        List<String> actions = new ArrayList<>();
        if (analytics.getMissedCount() > 0 || analytics.getEscalatedCount() > 0) {
            actions.add("Increase monitoring frequency");
            actions.add("Review recent verification incidents");
        } else if (analytics.getLateCount() > 0) {
            actions.add("Monitor response delay trend");
        } else {
            actions.add("Continue normal monitoring");
        }

        String priority =
                analytics.getCurrentRiskScore() >= 70 ? "HIGH" :
                        analytics.getCurrentRiskScore() >= 40 ? "MEDIUM" : "LOW";

        return AiRiskInsightDTO.builder()
                .summary("Fallback insight generated because " + reason)
                .keyPatterns(patterns)
                .recommendedActions(actions)
                .monitoringPriority(priority)
                .generatedAt(LocalDateTime.now())
                .model("fallback-local")
                .build();
    }

    private List<String> readStringArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                values.add(item.asText());
            }
        }
        return values;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}