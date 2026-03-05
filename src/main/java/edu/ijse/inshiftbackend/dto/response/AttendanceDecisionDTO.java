package edu.ijse.inshiftbackend.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDecisionDTO {
    @NotBlank
    @Size(max = 500)
    private String note;
}