package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePresenceTrendPointDTO {

    private Long presenceCheckId;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    private String status;
    private String triggerReason;
    private String riskLevel;
    private String responseSource;

    private Integer responseDelaySeconds;
    private Boolean lateResponse;
    private Boolean missedResponse;
    private Boolean escalated;
}