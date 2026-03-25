package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRiskInsightDTO {

    private String summary;
    private List<String> keyPatterns;
    private List<String> recommendedActions;
    private String monitoringPriority;
    private LocalDateTime generatedAt;
    private String model;
}