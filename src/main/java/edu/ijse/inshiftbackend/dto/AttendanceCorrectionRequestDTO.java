package edu.ijse.inshiftbackend.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceCorrectionRequestDTO {
    private LocalDate attendanceDate;
    private String type;
    private String reason;
    private LocalDateTime requestedCheckInTime;
    private LocalDateTime requestedCheckOutTime;
}