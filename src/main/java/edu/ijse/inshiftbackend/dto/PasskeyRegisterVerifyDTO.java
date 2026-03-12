package edu.ijse.inshiftbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyRegisterVerifyDTO {

    @NotBlank(message = "Credential id is required")
    private String id;

    @NotBlank(message = "Raw id is required")
    private String rawId;

    @NotBlank(message = "Credential type is required")
    private String type;

    @Valid
    @NotNull(message = "Response is required")
    private AttestationResponseDTO response;

    private String deviceName;
}