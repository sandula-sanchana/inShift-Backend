package edu.ijse.inshiftbackend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ijse.inshiftbackend.dto.response.*;
import edu.ijse.inshiftbackend.service.AiPatternInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiPatternInsightServiceImpl implements AiPatternInsightService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.openrouter.api-key:}")
    private String openRouterApiKey;

    @Value("${ai.openrouter.url:https://openrouter.ai/api/v1/chat/completions}")
    private String openRouterUrl;

    @Value("${ai.openrouter.model:google/gemini-3-flash-preview}")
    private String openRouterModel;

    @Override
    public EmployeeAiPatternInsightDTO analyzeEmployee(EmployeeAiPatternContextDTO context) {
        if (openRouterApiKey == null || openRouterApiKey.isBlank()) {
            return fallbackEmployeeInsight("AI key is not configured");
        }

        try {
            String prompt = buildEmployeePrompt(context);

            Map<String, Object> body = new HashMap<>();
            body.put("model", openRouterModel);
            body.put("max_tokens", 500);
            body.put("temperature", 0.2);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content",
                    "You are an HR attendance and presence behavior intelligence analyst. " +
                            "Return STRICT JSON only with keys: summary, suspiciousPatterns, whySuspicious, recommendedActions, monitoringPriority, confidence, chartHighlights. " +
                            "suspiciousPatterns, whySuspicious, recommendedActions, chartHighlights must be arrays of short strings. " +
                            "monitoringPriority must be LOW, MEDIUM, or HIGH. " +
                            "confidence must be LOW, MEDIUM, or HIGH. " +
                            "Focus on repeated patterns, trend shifts, and cross-signal inconsistencies."
            ));
            messages.add(Map.of("role", "user", "content", prompt));

            body.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openRouterApiKey);
            headers.set("HTTP-Referer", "http://localhost:8080");
            headers.set("X-Title", "InShift Company AI Pattern Scanner");

            ResponseEntity<String> response = restTemplate.exchange(
                    openRouterUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode parsed = objectMapper.readTree(content);

            return EmployeeAiPatternInsightDTO.builder()
                    .summary(parsed.path("summary").asText("No summary generated"))
                    .suspiciousPatterns(readStringArray(parsed.path("suspiciousPatterns")))
                    .whySuspicious(readStringArray(parsed.path("whySuspicious")))
                    .recommendedActions(readStringArray(parsed.path("recommendedActions")))
                    .monitoringPriority(parsed.path("monitoringPriority").asText("MEDIUM"))
                    .confidence(parsed.path("confidence").asText("MEDIUM"))
                    .chartHighlights(readStringArray(parsed.path("chartHighlights")))
                    .generatedAt(LocalDateTime.now())
                    .model(openRouterModel)
                    .build();

        } catch (Exception e) {
            return fallbackEmployeeInsight("AI generation failed: " + e.getMessage());
        }
    }

    @Override
    public CompanyAiOverviewDTO analyzeCompany(
            List<EmployeeAiPatternContextDTO> contexts,
            List<SuspiciousEmployeeAiCardDTO> suspiciousEmployees
    ) {
        if (openRouterApiKey == null || openRouterApiKey.isBlank()) {
            return fallbackCompanyOverview("AI key is not configured");
        }

        try {
            String prompt = buildCompanyPrompt(contexts, suspiciousEmployees);

            Map<String, Object> body = new HashMap<>();
            body.put("model", openRouterModel);
            body.put("max_tokens", 400);
            body.put("temperature", 0.2);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content",
                    "You are an HR attendance intelligence copilot. " +
                            "Return STRICT JSON only with keys: overview, topPatterns, trendHighlights, recommendedAdminFocus. " +
                            "topPatterns, trendHighlights, recommendedAdminFocus must be arrays of short strings."
            ));
            messages.add(Map.of("role", "user", "content", prompt));

            body.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openRouterApiKey);
            headers.set("HTTP-Referer", "http://localhost:8080");
            headers.set("X-Title", "InShift Company AI Pattern Scanner");

            ResponseEntity<String> response = restTemplate.exchange(
                    openRouterUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode parsed = objectMapper.readTree(content);

            return CompanyAiOverviewDTO.builder()
                    .overview(parsed.path("overview").asText("No overview generated"))
                    .topPatterns(readStringArray(parsed.path("topPatterns")))
                    .trendHighlights(readStringArray(parsed.path("trendHighlights")))
                    .recommendedAdminFocus(readStringArray(parsed.path("recommendedAdminFocus")))
                    .model(openRouterModel)
                    .generatedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            return fallbackCompanyOverview("AI generation failed: " + e.getMessage());
        }
    }

    private String buildEmployeePrompt(EmployeeAiPatternContextDTO context) {
        return """
                Analyze this employee's multi-day attendance and presence behavior.
                Detect suspicious patterns, repeated abnormal combinations, emerging behavior shifts,
                and inconsistencies between attendance intelligence and presence verification.

                Employee:
                - ID: %d
                - Name: %s
                - Window Days: %d

                Current Behavior:
                - Current Risk Score: %d
                - Current Trust Score: %d
                - Current Risk Level: %s
                - Total Flags Seen: %d
                - Total High Risk Days: %d

                Aggregated Presence:
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

                Aggregated Attendance:
                - Attendance Flag Count: %d
                - High Risk Attendance Days: %d
                - Average Attendance Risk Score: %d
                - Max Attendance Risk Score: %d

                Summary Facts:
                %s

                Attendance Timeline:
                %s

                Presence Timeline:
                %s

                Daily Risk Series:
                %s

                Return JSON only.
                """.formatted(
                context.getEmployeeId(),
                context.getEmployeeName(),
                context.getWindowDays(),
                safeInt(context.getCurrentRiskScore()),
                safeInt(context.getCurrentTrustScore()),
                safeString(context.getCurrentRiskLevel()),
                safeInt(context.getTotalFlagsSeen()),
                safeInt(context.getTotalHighRiskDays()),
                safeInt(context.getTotalPresenceChecks()),
                safeInt(context.getRespondedCount()),
                safeInt(context.getLateCount()),
                safeInt(context.getMissedCount()),
                safeInt(context.getEscalatedCount()),
                safeInt(context.getAverageResponseDelaySeconds()),
                safeInt(context.getMaxResponseDelaySeconds()),
                safeInt(context.getMinResponseDelaySeconds()),
                safeInt(context.getCompanyPcResponses()),
                safeInt(context.getMobileResponses()),
                safeInt(context.getAttendanceFlagCount()),
                safeInt(context.getAttendanceHighRiskDays()),
                safeInt(context.getAverageAttendanceRiskScore()),
                safeInt(context.getMaxAttendanceRiskScore()),
                String.join("\n", context.getSummaryFacts()),
                String.join("\n", context.getAttendanceTimeline()),
                String.join("\n", context.getPresenceTimeline()),
                buildRiskSeries(context.getDailyRiskDates(), context.getDailyRiskScores())
        );
    }

    private String buildCompanyPrompt(
            List<EmployeeAiPatternContextDTO> contexts,
            List<SuspiciousEmployeeAiCardDTO> suspiciousEmployees
    ) {
        String employeeSummary = suspiciousEmployees.stream()
                .map(emp -> String.format(
                        "- %s | priority=%s | confidence=%s | summary=%s | patterns=%s",
                        emp.getEmployeeName(),
                        emp.getMonitoringPriority(),
                        emp.getConfidence(),
                        emp.getSummary(),
                        emp.getSuspiciousPatterns()
                ))
                .reduce("", (a, b) -> a + "\n" + b);

        return """
                Analyze company-wide suspicious attendance and presence behavior trends.

                Company Scan Summary:
                - Total employee contexts analyzed: %d
                - Suspicious employees surfaced: %d

                Suspicious Employee Summaries:
                %s

                Return organization-level suspicious patterns, trend highlights, and recommended admin focus.
                Return JSON only.
                """.formatted(
                contexts.size(),
                suspiciousEmployees.size(),
                employeeSummary
        );
    }

    private String buildRiskSeries(List<String> dates, List<Integer> scores) {
        if (dates == null || scores == null || dates.isEmpty() || scores.isEmpty()) {
            return "No risk series available.";
        }

        StringBuilder sb = new StringBuilder();
        int count = Math.min(dates.size(), scores.size());

        for (int i = 0; i < count; i++) {
            sb.append("- ").append(dates.get(i)).append(": ").append(scores.get(i)).append("\n");
        }
        return sb.toString();
    }

    private EmployeeAiPatternInsightDTO fallbackEmployeeInsight(String reason) {
        return EmployeeAiPatternInsightDTO.builder()
                .summary("Fallback insight generated because " + reason)
                .suspiciousPatterns(List.of("AI insight unavailable"))
                .whySuspicious(List.of("Pattern reasoning could not be generated"))
                .recommendedActions(List.of("Review employee timeline manually"))
                .monitoringPriority("MEDIUM")
                .confidence("LOW")
                .chartHighlights(List.of())
                .generatedAt(LocalDateTime.now())
                .model("fallback-local")
                .build();
    }

    private CompanyAiOverviewDTO fallbackCompanyOverview(String reason) {
        return CompanyAiOverviewDTO.builder()
                .overview("Fallback overview generated because " + reason)
                .topPatterns(List.of("AI overview unavailable"))
                .trendHighlights(List.of())
                .recommendedAdminFocus(List.of("Review suspicious employees manually"))
                .model("fallback-local")
                .generatedAt(LocalDateTime.now())
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

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}