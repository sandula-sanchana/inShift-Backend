package edu.ijse.inshiftbackend.dto;

import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceEnrollmentDecisionDTO {

    @NotNull(message = "Approve flag is required")
    private Boolean approve;

    private DeviceTrustType approvedTrustType;

    private String adminComment;
}