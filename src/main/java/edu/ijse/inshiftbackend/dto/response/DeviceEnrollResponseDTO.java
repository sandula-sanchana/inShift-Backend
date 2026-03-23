package edu.ijse.inshiftbackend.dto.response;

import edu.ijse.inshiftbackend.entity.enums.DeviceApprovalStatus;
import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceEnrollResponseDTO {

    private Long deviceId;
    private String deviceFingerprint;
    private DeviceApprovalStatus approvalStatus;
    private DeviceTrustType approvedTrustType;
    private String message;
}