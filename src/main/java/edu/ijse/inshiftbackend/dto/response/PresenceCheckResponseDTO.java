package edu.ijse.inshiftbackend.dto.response;

import edu.ijse.inshiftbackend.entity.enums.PresenceCheckResponseSource;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckRiskLevel;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckSourceExpected;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckStatus;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckTriggerReason;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceCheckResponseDTO {

    private Long id;
    private Long employeeId;
    private String empCode;
    private String employeeName;
    private String branchName;

    private PresenceCheckTriggerReason triggerReason;
    private PresenceCheckRiskLevel riskLevel;
    private PresenceCheckStatus status;
    private PresenceCheckSourceExpected sourceExpected;

    private String triggerDescription;
    private String adminNote;

    private LocalDateTime createdAt;
    private LocalDateTime dueAt;
    private LocalDateTime notifiedAt;
    private LocalDateTime respondedAt;

    private PresenceCheckResponseSource responseSource;
    private String respondingDeviceFingerprint;

    private Double responseLatitude;
    private Double responseLongitude;
    private Double responseAccuracyMeters;
    private String responseLocationText;
    private String responseNote;

    private Integer responseDelaySeconds;
    private Boolean lateResponse;
    private Boolean missedResponse;
    private Boolean escalated;
    private LocalDateTime escalatedAt;
    private Integer escalationLevel;
}