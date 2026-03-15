package edu.ijse.inshiftbackend.dto.response;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceCorrectionResponseDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate attendanceDate;
    private String type;
    private String reason;
    private LocalDateTime requestedCheckInTime;
    private LocalDateTime requestedCheckOutTime;
    private String status;
    private String adminDecisionNote;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
}