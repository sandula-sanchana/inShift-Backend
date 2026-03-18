package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceFCMTokenRegisterDTO {

    @NotBlank(message = "FCM token is required")
    private String fcmToken;

    @NotNull(message = "Device type is required")
    private String deviceType;

    private String deviceName;

    private String userAgent;
}