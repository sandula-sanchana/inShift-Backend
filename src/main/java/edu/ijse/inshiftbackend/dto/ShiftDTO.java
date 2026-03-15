package edu.ijse.inshiftbackend.dto;

import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftDTO {

    private Long shiftId;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer graceMinutes;
    private Integer earlyCheckInMinutes;
    private Integer earlyLeaveGraceMinutes;
    private Integer overtimeAfterMinutes;
    private Integer breakMinutes;
    private Boolean active;
    private Boolean isDefault;
}