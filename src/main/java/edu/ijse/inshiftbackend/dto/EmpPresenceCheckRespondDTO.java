package edu.ijse.inshiftbackend.dto;

import edu.ijse.inshiftbackend.entity.enums.PresenceCheckResponseSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpPresenceCheckRespondDTO {

    @NotNull(message = "Presence check ID is required")
    private Long presenceCheckId;

    @NotBlank(message = "Device fingerprint is required")
    private String deviceFingerprint;

    @NotNull(message = "Response source is required")
    private PresenceCheckResponseSource responseSource;

    private String biometricProofToken;

    private Double latitude;
    private Double longitude;
    private Double accuracyMeters;
    private String locationText;
    private String responseNote;
}