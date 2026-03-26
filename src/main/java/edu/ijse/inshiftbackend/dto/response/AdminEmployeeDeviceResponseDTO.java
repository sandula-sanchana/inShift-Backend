package edu.ijse.inshiftbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEmployeeDeviceResponseDTO {
    private Long id;
    private Long employeeId;
    private Long requestId;

    private String deviceName;
    private String deviceFingerprint;

    private String requestedTrustType;
    private String approvedTrustType;
    private String approvalStatus;

    private String userAgent;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}