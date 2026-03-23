package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyRegisterVerifyDTO {

    @NotBlank(message = "Credential JSON is required")
    private String credentialJson;

    @NotBlank(message = "Device name is required")
    private String deviceName;

    private String deviceFingerprint;
}