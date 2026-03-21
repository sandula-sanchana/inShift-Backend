package edu.ijse.inshiftbackend.dto;

import edu.ijse.inshiftbackend.entity.enums.PresenceCheckRiskLevel;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckSourceExpected;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckTriggerReason;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceCheckCreateDTO {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Trigger reason is required")
    private PresenceCheckTriggerReason triggerReason;

    @NotNull(message = "Risk level is required")
    private PresenceCheckRiskLevel riskLevel;

    @NotNull(message = "Expected source is required")
    private PresenceCheckSourceExpected sourceExpected;

    private String triggerDescription;
    private String adminNote;
    private Integer dueInSeconds;
}