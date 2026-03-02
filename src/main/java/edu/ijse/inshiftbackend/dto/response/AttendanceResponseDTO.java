package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponseDTO {

    private Long id;

    private Long employeeId;
    private String employeeName;

    private Long branchId;
    private String branchName;

    private String type;              // IN / OUT
    private String source;            // MOBILE / WEB
    private String status;            // VALID / PENDING / REJECTED

    private LocalDateTime eventTime;

    private Double lat;
    private Double lng;
    private String locationText;

    private boolean verified;
    private String verificationMethod;

    private String decisionNote;      // if rejected
}