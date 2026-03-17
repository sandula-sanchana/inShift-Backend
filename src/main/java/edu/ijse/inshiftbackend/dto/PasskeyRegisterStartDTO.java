package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyRegisterStartDTO {

    @NotBlank(message = "Device name is required")
    private String deviceName;
}