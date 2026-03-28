package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeOfferOvertimeSwapDTO {

    @NotNull
    private Long toEmployeeId;

    @Size(max = 1000)
    private String note;
}