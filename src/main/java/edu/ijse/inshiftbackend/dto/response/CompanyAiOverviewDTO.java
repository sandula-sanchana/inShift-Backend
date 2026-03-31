package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyAiOverviewDTO {
    private String overview;
    private List<String> topPatterns;
    private List<String> trendHighlights;
    private List<String> recommendedAdminFocus;
    private String model;
    private LocalDateTime generatedAt;
}