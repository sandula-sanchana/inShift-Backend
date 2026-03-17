package edu.ijse.inshiftbackend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRuleDTO {

    private Long id;
    private String ruleKey;
    private String ruleName;
    private String description;
    private Boolean enabled;
    private Integer thresholdValue;
    private Integer scoreImpact;
    private String severity;
}