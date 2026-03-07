package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyRegisterVerifyDTO {

    @NotBlank(message = "Credential ID is required")
    private String credentialId;

    @NotBlank(message = "Public key is required")
    private String publicKey;

    private Long signCount;

    private String deviceName;
}