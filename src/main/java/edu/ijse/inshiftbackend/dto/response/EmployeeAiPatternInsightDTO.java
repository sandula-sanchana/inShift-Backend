package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeAiPatternInsightDTO {
    private String summary;
    private List<String> suspiciousPatterns;
    private List<String> whySuspicious;
    private List<String> recommendedActions;
    private String monitoringPriority;
    private String confidence;
    private List<String> chartHighlights;
    private LocalDateTime generatedAt;
    private String model;
}