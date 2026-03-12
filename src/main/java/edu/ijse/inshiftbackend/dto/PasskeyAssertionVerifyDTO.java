package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyAssertionVerifyDTO {

    @NotBlank(message = "Credential JSON is required")
    private String credentialJson;
}