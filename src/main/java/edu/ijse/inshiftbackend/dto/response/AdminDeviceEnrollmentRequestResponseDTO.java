package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDeviceEnrollmentRequestResponseDTO {

    private Long id;
    private String employeeName;
    private Long employeeId;
    private String status;
    private String requestType;

    private String requestedDeviceName;
    private String requestedUserAgent;
    private Integer riskScoreImpact;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private String existingDeviceName;
    private LocalDateTime existingCredentialCreatedAt;

    private String adminComment;
}