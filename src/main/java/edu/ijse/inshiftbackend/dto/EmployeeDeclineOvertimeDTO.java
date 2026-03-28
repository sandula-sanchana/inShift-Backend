package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDeclineOvertimeDTO {

    @NotBlank
    @Size(max = 1000)
    private String note;
}