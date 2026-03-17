package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceIntelligenceOverviewDTO {
    private Long employeeId;
    private String employeeName;
    private LocalDate attendanceDate;
    private Integer riskScore;
    private Integer trustScore;
    private Integer totalFlags;
    private Boolean requiresReview;
    private Boolean highRisk;
    private List<AttendanceFlagDTO> flags;
}