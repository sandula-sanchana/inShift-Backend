package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeSwapResponseDTO {

    private Long id;

    private Long overtimeAssignmentId;

    private Long fromEmployeeId;
    private String fromEmployeeName;

    private Long toEmployeeId;
    private String toEmployeeName;

    private String note;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}