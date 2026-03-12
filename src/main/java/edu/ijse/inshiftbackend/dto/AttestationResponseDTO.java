package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttestationResponseDTO {

    @NotBlank(message = "clientDataJSON is required")
    private String clientDataJSON;

    @NotBlank(message = "attestationObject is required")
    private String attestationObject;
}