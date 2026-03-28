package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceCheckBiometricVerifyDTO {

    @NotNull(message = "Presence check ID is required")
    private Long presenceCheckId;

    @NotBlank(message = "Device fingerprint is required")
    private String deviceFingerprint;

    @NotBlank(message = "Credential JSON is required")
    private String credentialJson;
}