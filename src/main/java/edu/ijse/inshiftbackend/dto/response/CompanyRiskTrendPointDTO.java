package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRiskTrendPointDTO {
    private String date;
    private Double averageRiskScore;
    private Integer highRiskCount;
}