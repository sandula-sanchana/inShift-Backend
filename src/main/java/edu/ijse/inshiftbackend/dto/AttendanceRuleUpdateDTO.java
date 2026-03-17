package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRuleUpdateDTO {

    @NotNull(message = "Enabled is required")
    private Boolean enabled;

    private Integer thresholdValue;
    private Integer scoreImpact;
    private String severity;
    private String ruleName;
    private String description;
}