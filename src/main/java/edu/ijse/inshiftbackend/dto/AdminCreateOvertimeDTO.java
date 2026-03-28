package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCreateOvertimeDTO {

    @NotNull
    private Long employeeId;

    @NotNull
    private LocalDate otDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @NotNull
    @Min(0)
    private Integer breakMinutes;

    @NotBlank
    @Size(max = 1000)
    private String reason;
}