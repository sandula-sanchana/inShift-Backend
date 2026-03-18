package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDeviceTokenResponseDTO {

    private Long id;
    private String deviceType;
    private String deviceName;
    private String userAgent;
    private Boolean active;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
}