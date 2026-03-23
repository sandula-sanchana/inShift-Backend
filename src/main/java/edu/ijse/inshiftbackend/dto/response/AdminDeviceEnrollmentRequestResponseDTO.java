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

    private Long employeeDeviceId;
    private String deviceFingerprint;

    private String requestedTrustType;
    private String approvedTrustType;

    private String requestedDeviceName;
    private String requestedUserAgent;
    private String requestedPlatform;
    private String requestedBrowser;
    private String requestedIpAddress;
    private String requestReason;

    private Integer riskScoreImpact;

    private String existingDeviceName;
    private LocalDateTime existingCredentialCreatedAt;

    private String adminComment;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime approvedAt;
    private LocalDateTime completedAt;
}