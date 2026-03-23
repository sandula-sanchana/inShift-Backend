package edu.ijse.inshiftbackend.dto;

import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceEnrollRequestDTO {

    @NotBlank(message = "Device fingerprint is required")
    private String deviceFingerprint;

    @NotBlank(message = "Device name is required")
    private String deviceName;

    private String userAgent;

    @NotNull(message = "Requested trust type is required")
    private DeviceTrustType requestedTrustType;
}