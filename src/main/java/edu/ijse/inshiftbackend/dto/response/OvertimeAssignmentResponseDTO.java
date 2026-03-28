package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeAssignmentResponseDTO {

    private Long id;

    private Long employeeId;
    private String employeeName;
    private String employeeCode;

    private LocalDate otDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer breakMinutes;
    private Integer durationMinutes;
    private String reason;

    private String status;
    private String employeeResponseNote;

    private Long assignedById;
    private String assignedByName;

    private LocalDateTime assignedAt;
    private LocalDateTime updatedAt;
}